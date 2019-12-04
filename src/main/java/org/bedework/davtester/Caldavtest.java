/*
# Copyright (c) 2006-2016 Apple Inc. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package org.bedework.davtester;

import org.bedework.davtester.ical.Icalendar;
import org.bedework.davtester.request.Request;
import org.bedework.util.dav.DavUtil;
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static org.bedework.davtester.Manager.RESULT_FAILED;
import static org.bedework.davtester.Manager.RESULT_IGNORED;
import static org.bedework.davtester.Manager.RESULT_OK;
import static org.bedework.davtester.Manager.TestResult;
import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.Utils.encodeUtf8;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.childrenMatching;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.davtester.XmlUtils.findNodes;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.davtester.XmlUtils.multiStatusResponse;
import static org.bedework.davtester.XmlUtils.parseXmlString;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Class to encapsulate a single caldav test run.
 */
class Caldavtest extends DavTesterBase {
  @Override
  public String getKind() {
    return "CALDAVTEST";
  }

  private class RequestPars {
    final String uri;
    final Request req;

    RequestPars(final String uri,
                final Request req) {
      this.uri = uri;
      this.req = req;
    }

    Request makeRequest(final String method) {
      var nreq = new Request(manager);

      nreq.method = method;

      nreq.scheme = req.scheme;
      nreq.host = req.host;
      nreq.port = req.port;

      nreq.ruris.add(uri);
      nreq.ruri = uri;
      if (req.getUser() != null) {
        nreq.setUser(req.getUser());
      }
      if (req.getPswd() != null) {
        nreq.setPswd(req.getPswd());
      }
      nreq.cert = req.cert;

      return nreq;
    }
  }

  final DavUtil dutil = new DavUtil();
  private Path testPath;

  boolean ignoreAll;
  private boolean only;

  private List<Request> startRequests = new ArrayList<>();
  private List<Request> endRequests = new ArrayList<>();
  private List<RequestPars> endDeletes = new ArrayList<>();
  private List<Testsuite> suites = new ArrayList<>();

  private String grabbedLocation;
  private Set<String> previouslyFound = new TreeSet<>();
  private Map<String, String> uidmaps = new HashMap<>();

  public Caldavtest(final Manager manager,
                    final Path testPath,
                    final boolean ignoreRoot) {
    super(manager);
    this.testPath = testPath;
    this.name = testPath.getFileName().toString();

    doc = XmlUtils.parseXml(testPath.toString());
    parseXML(doc.getDocumentElement());
  }

  public TestResult run() {
    if (missingFeatures().size() != 0) {
      manager.testFile(name,
                       format("Missing features: %s",
                                           missingFeatures()),
                       RESULT_IGNORED);
      return TestResult.ignored();
    }

    if (excludedFeatures().size() != 0) {
      manager.testFile(name,
                       format("Excluded features: %s",
                              excludedFeatures()),
                       RESULT_IGNORED);
      return TestResult.ignored();
    }

    // Always need a new set of UIDs for the entire test
    for (var kv : manager.serverInfo.newUIDs()) {
      uidmaps.put(kv.key, format("%s - %s", kv.val, name));
    }

    for (var suite: suites) {
      if (suite.only) {
        only = true;
        break;
      }
    }

    try {
      if (httpTrace) {
        httpTraceOn();
      }

      final TestResult res;
      var doReqres = doRequests("Start Requests...", startRequests, false,
                          true,
                          format("%s | %s", name,
                                        "START_REQUESTS"), 1);

      if (!doReqres) {
        manager.testFile(name,
                         "Start items failed - tests will not be run.",
                         manager.RESULT_ERROR);
        res = TestResult.failed();
      } else {
        res = runTests(name);
      }
      doEnddelete("Deleting Requests...", format("%s | %s", name,
                  "END_DELETE"));
      doRequests("End Requests...", endRequests, false, false,
                 format("%s | %s", name, "END_REQUESTS"), 1);
      return res;
    } catch (final Throwable t) {
      manager.testFile(name,
                       format("FATAL ERROR: %s", t.getMessage()),
                       manager.RESULT_ERROR);

      if (manager.debug()) {
        manager.error(t);
      }

      return TestResult.failed();
    } finally {
      if (httpTrace) {
        httpTraceOff();
      }
    }
  }

  public TestResult runTests(final String label) {
    var res = new TestResult();

    var testfile = manager.testFile(name, description, null);
    for (var suite : suites) {
      try {
        if (suite.httpTrace) {
          httpTraceOn();
        }

        res.add(runTestSuite(testfile, suite,
                             format("%s | %s",
                                    label,
                                    suite.name)));
      } finally {
        if (suite.httpTrace) {
          httpTraceOff();
        }
      }
    }

    return res;
  }

  public TestResult runTestSuite(final KeyVals testfile,
                                 final Testsuite suite,
                                 final String label) {
    var resultName = suite.name;
    var res = new TestResult();
    // POSTGRES postgresCount = null;

    if (only && !suite.only || suite.ignore) {
      manager.testSuite(testfile, resultName,
                        "    Deliberately ignored",
                        RESULT_IGNORED);
      res.ignored = suite.tests.size();
    } else if (suite.hasMissingFeatures()) {
      manager.testSuite(testfile, resultName,
                        format("    Missing features: %s", suite.missingFeatures()),
                        RESULT_IGNORED);
      res.ignored = suite.tests.size();
    } else if (suite.hasExcludedFeatures()) {
      manager.testSuite(testfile, resultName,
                        format("    Excluded features: %s", suite.excludedFeatures()),
                        RESULT_IGNORED);
      res.ignored = suite.tests.size();
    } else {
      // POSTGRES postgresCount = postgresInit();
      //if (manager.memUsage) {
      //  start_usage = manager.getMemusage();
      //}
      var etags = new HashMap<String, String>();
      var onlyTests = false;
      for (var test: suite.tests) {
        if (test.only) {
          onlyTests = true;
          break;
        }
      }

      var testsuite = manager.testSuite(testfile, resultName, "", null);
      var uids = suite.aboutToRun();
      for (var kv: uids) {
        uidmaps.put(kv.key, format("%s - %s", kv.val, label));
      }

      for (var test: suite.tests) {
        try {
          if (test.httpTrace) {
            httpTraceOn();
          }

          var result = runTest(testsuite, test, etags, onlyTests,
                               format("%s | %s", label, test.name));
          if (result == 't') {
            res.ok += 1;
          } else if (result == 'f') {
            res.failed += 1;
          } else {
            res.ignored += 1;
          }
        } finally {
          if (test.httpTrace) {
            httpTraceOff();
          }
        }
      }
      /*
            if (manager.memUsage){
              end_usage=manager.getMemusage();
              manager.message("trace","    Mem. Usage: RSS=%s%% VSZ=%s%%"%(str(((end_usage[1]-start_usage[1])*100)/start_usage[1]),str(((end_usage[0]-start_usage[0])*100)/start_usage[0])))
              }
        */
    }

    manager.trace(format("  Suite Results: %d PASSED, %d FAILED, %d IGNORED\n",
                          res.ok, res.failed, res.ignored));
    /* POSTGRES
        if postgresCount is ! null:
            postgresResult(postgresCount, indent=4);
         */
    return res;
  }

  public char runTest (final KeyVals testsuite,
                       final Test test,
                       final Map<String, String> etags,
                       final boolean only,
                       final String label) {
    if (test.ignore || only && !test.only) {
      manager.testResult(testsuite, test.name,
                         "      Deliberately ignored",
                         RESULT_IGNORED, null);
      return 'i';
    }

    if (test.hasMissingFeatures()) {
      manager.testResult(testsuite, test.name,
                         format("      Missing features: %s",
                                test.missingFeatures()),
                         RESULT_IGNORED, null);
      return 'i';
    }

    if (test.hasExcludedFeatures()) {
      manager.testResult(testsuite,
                         test.name,
                         format("      Excluded features: %s",
                                test.excludedFeatures()),
                         RESULT_IGNORED, null);
      return 'i';
    }

    var result = true;
    String resulttxt = null;
    // POSTGRES postgresCount = postgresInit();
    var reqstats = new RequestStats();

    for (var ctr = 0; ctr <= test.count; ctr++) {
      var failed = false;
      var reqCount = 1;
      for (var req : test.requests) {
        var t = System.currentTimeMillis();
        if (req.waitForSuccess) {
          t += manager.serverInfo.waitsuccess;
        } else {
          t += 100;
        }

        while (t > System.currentTimeMillis()) {
          failed = false;
          if (req.iterateData) {
            if (!req.hasNextData()) {
              manager.testResult(testsuite, test.name,
                                 "      No iteration data - ignored",
                                 RESULT_IGNORED, null);
              return 'i';
            }

            while (req.getNextData()) {
              var reqres = doRequest(
                      req, test.details, true, false,
                      reqstats, etags,
                      format("%s | #%s", label, reqCount + 1),
                      ctr + 1);
              if (!reqres.ok) {
                failed = true;
                break;
              }

              resulttxt = reqres.message;
            }
          } else {
            var reqres = doRequest(
                    req, test.details, true, false,
                    reqstats, etags,
                    format("%s | #%s", label, reqCount + 1), ctr + 1);
            if (!reqres.ok) {
              failed = true;
            }

            resulttxt = reqres.message;
          }

          if (!failed || !req.waitForSuccess) {
            break;
          }
        }
        if (failed) {
          result = false;
          break;
        }
      }
    }

    var addons = new KeyVals();
    if (resulttxt != null) {
      manager.trace(resulttxt);
    }

    if (test.stats) {
      manager.trace(format("    Total Time: %.3e secs",
                           ((float)reqstats.total / 1000)));
      manager.trace(format("    Average Time: %.3f secs",
                           ((float)reqstats.total / reqstats.count)));
      var timing = new KeyVals();
      timing.put("total", reqstats.total);
      timing.put("average", reqstats.total / reqstats.count);
      addons.put("timing", timing);
    }

    // postgresResult(postgresCount, indent=8);
    final int rcode;
    final char res;
    if (result) {
      rcode = RESULT_OK;
      res = 't';
    } else {
      rcode = RESULT_FAILED;
      res = 'f';
    }
    manager.testResult(testsuite, test.name, resulttxt, rcode,
                       addons);
    return res;
  }

  public boolean doRequests(final String description,
                            final List<Request> requests,
                            final boolean doverify,
                            final boolean forceverify,
                            final String label,
                            final int count) {
    if (Util.isEmpty(requests)) {
      return true;
    }

    var result = true;

    manager.trace("Start: " + description);

    var reqCount = 1;
    var resulttxt = "";

    for (var req: requests) {
      var resreq = doRequest(
              req, false, doverify, forceverify,
              null, // stats
              null, // etags
              format("%s | #%s", label, String.valueOf(reqCount)),
              count);
      if (resreq.message != null) {
        resulttxt += resreq.message;
      }

      if (!resreq.ok) {
        resulttxt += format(
                "\nFailure during multiple requests " +
                        "#%d out of %d, request=%s",
                reqCount, requests.size(),
                String.valueOf(req));
        result = false;
        break;
      }

      reqCount++;
    }

    final String s;
    if (result) {
      s = "[OK]";
    } else {
      s = "[FAILED]";
    }

    manager.trace(format("%s60%s5",
                         "End: " + description,
                         s));
    if (resulttxt.length() > 0) {
      manager.trace(resulttxt);
    }
    return result;
  }

  public DoRequestResult doGet (Request originalRequest,
                                final UriIdPw uip,
                                final String label) {
    var req = uip.makeRequest(originalRequest, "GET");

    var reqres = doRequest(req, false, false, false,
                           null, // stats
                           null, // etags
                           label, 1);
    if (reqres.status / 100 != 2) {
      reqres.ok = false;
    }

    return reqres;
  }

  private String getOneHref(final Element node) {
    var href = childrenMatching(node, WebdavTags.href);

    if (href.size() != 1) {
      throwException("           Wrong number of DAV:href elements\n");
    }

    return content(href.get(0));
  }

  public Result<List<UriIdPw>> doFindall (final Request originalRequest,
                                          final UriIdPw uip,
                                          final String label) {
    var hrefs = new ArrayList<UriIdPw>();

    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");

    var reqres = doRequest(req, false, false, false,
                           null, // stats
                           null, // etags
                           label, 1);
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {
      final MultiStatusResponse msr =
              multiStatusResponse(reqres.responseData);

      var requestUri = req.getURI();
      for (var response: msr.responses) {
        // Get href for this response
        if (!response.href.equals(requestUri)) {
          hrefs.add(new UriIdPw(response.href, uip.user, uip.pswd));
        }
      }
    }

    return new Result<>(hrefs);
  }

  public boolean doDeleteall(final Request originalRequest,
                             final List<UriIdPw> deletes,
                             final String label) {
    if (Util.isEmpty(deletes)) {
      return true;
    }
    for (var uip : deletes) {
      var req = uip.makeRequest(originalRequest, "DELETE");

      var reqres = doRequest(req, false, false, false,
                             null, // stats
                             null, // etags
                             label, 1);
      if (reqres.status / 100 != 2) {
        return false;
      }
    }

    return true;
  }

  public String doFindnew(final Request originalRequest,
                          final UriIdPw uip,
                          final String label,
                          final boolean other) {
    String hresult = null;

    var uri = uip.ruri;
    String skip;

    if (other) {
      // Remove last element
      uri = StringUtils.stripEnd(manager.serverInfo.extrasubs(uri),
                                 "/");
      skip = uri;

      var pos = uri.lastIndexOf("/");

      if (pos > 0) {
        uri = uri.substring(0, pos + 1);
      }
    } else {
      skip = null;
    }

    var possibleMatches = new ArrayList<String>();

    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");
    req.ruris.add(uri);
    req.ruri = uri;

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "<D:getlastmodified/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");
    var reqres = doRequest(req, false, false, false,
                           null, // stats
                           null, // etags
                           format("%s | %s", label, "FINDNEW"),
                           1);
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {
      final MultiStatusResponse msr =
              multiStatusResponse(reqres.responseData);

      long latest = 0;
      var requestUri = req.getURI();
      for (var response: msr.responses) {
        if (!response.href.equals(requestUri) &&
                (!other|| !(response.href.equals(skip)))) {

          for (var propstat: response.propstats) {
            var status = (propstat.status / 100) == 2;

            if (status) {
              // Get properties for this propstat

              for (var prop: propstat.props) {

                // Get properties for this propstat
                var glm = childrenMatching(prop, WebdavTags.getlastmodified);
                if (glm.size() != 1) {
                  continue;
                }

                var value = content(glm.get(0));
                var fmt = DateTimeFormatter.RFC_1123_DATE_TIME;
                ZonedDateTime zdt = fmt.parse (value , ZonedDateTime :: from);
                long tval = Date.from(zdt.toInstant()).getTime();

                if (tval > latest) {
                  possibleMatches.clear();
                  possibleMatches.add(response.href);
                  latest = tval;
                } else if (tval == latest) {
                  possibleMatches.add(response.href);
                }
              }
            } else {
              possibleMatches.add(response.href);
            }
          }
        }
      }
    }

    if (possibleMatches.size() == 1) {
      hresult = possibleMatches.get(0);
    } else if (possibleMatches.size() > 1) {
      var notSeenBefore = diff(possibleMatches, previouslyFound);
      if (notSeenBefore.size() == 1) {
        hresult = notSeenBefore.get(0);
      }
    }
    if (hresult != null) {
      previouslyFound.add(hresult);
    }

    return hresult;
  }

  public String doFindcontains(final Request originalRequest,
                               final UriIdPw uip,
                               final String match,
                               final String label) {
    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");

    var reqres = doRequest(req, false, false, false,
                           null, // stats
                           null, // etags
                           format("%s | %s", label, "FINDNEW"), 1);
    String href = null;
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {

      var requestUri = req.getURI();

      final MultiStatusResponse msr =
              multiStatusResponse(reqres.responseData);

      for (var response : msr.responses) {
        if (!response.href.equals(requestUri)) {
          var respdata = doGet(req,
                               new UriIdPw(response.href,
                                           uip.user,
                                           uip.pswd),
                               label);
          if (respdata.responseData.contains(match)) {
            href = response.href;
            break;
          }
        }
      }
    }

    return href;
  }

  public Result doWaitcount(final Request originalRequest,
                            final UriIdPw uip,
                            final int hrefCount,
                            final String label) {
    var hrefs = new ArrayList<String>();

    for (var ignore = 0; ignore < manager.serverInfo.waitcount; ignore++) {
      var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

      req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                             "<D:propfind xmlns:D=\"DAV:\">" +
                             "<D:prop>" +
                             "<D:getetag/>" +
                             "</D:prop>" +
                             "</D:propfind>",
                     "text/xml");

      var reqres = doRequest(req, false, false, false,
                             null, // stats
                             null, // etags
                             format("%s | %s %d", label,
                                    "WAITCOUNT", hrefCount),
                             1);
      hrefs.clear();

      if (reqres.ok &&
              (reqres.status == 207) &&
              (reqres.responseData != null)) {
        final MultiStatusResponse msr =
                multiStatusResponse(reqres.responseData);

        for (var response : msr.responses) {
          // Get href for this response
          var href = response.href;
          if (!StringUtils.stripEnd(href, "/").equals(
                  StringUtils.stripEnd(uip.ruri, "/"))) {
            hrefs.add(href);
          }
        }

        if (hrefs.size() == hrefCount) {
          return Result.ok();
        }
      }
      var delay = manager.serverInfo.waitdelay;
      synchronized (this) {
        try {
          Thread.sleep(delay);
        } catch (final InterruptedException e) {
          throwException(e);
        }
      }
    }

    if (!manager.debug() || Util.isEmpty(hrefs)) {
      return Result.fail(String.valueOf(hrefs.size()));
    }
    // Get the content of each resource
    var rdata = new StringBuilder();

    for (var href: hrefs) {
      var getDrr = doGet(originalRequest,
                         new UriIdPw(href, uip.user, uip.pswd), label);
      String test = "unknown";
      var rd = getDrr.responseData;
      if (rd.startsWith("BEGIN:VCALENDAR")) {
        var uidpos = rd.indexOf("UID:");
        if (uidpos != -1) {
          var end = rd.indexOf("\r\n", uidpos);
          if (end < 0) {
            end = rd.indexOf("\n", uidpos);
          }

          if (end < 0) {
            return Result.fail("No UID end found in\n" + rdata.toString());
          }
          var uid = rd.substring(uidpos + 4, end);
          test = uidmaps.computeIfAbsent(uid, s -> "unknown");
        }
      }
      rdata.append(format("\n\nhref: %s\ntest: %s\n\n%s\n",
                          href, test, getDrr.responseData));
    }

    return Result.fail(rdata.toString());
  }

  public boolean doWaitchanged(final Request originalRequest,
                               final UriIdPw uip,
                               final String etag,
                               final String label) {
    for (var ignore = 0; ignore < manager.serverInfo.waitcount; ignore++) {
      var req = uip.makeRequest(originalRequest, "HEAD");

      var reqres = doRequest(req, false, false, false,
                             null, // stats
                             null, // etags
                             format("%s | %s", label, "WAITCHANGED"), 1);
      if (reqres.ok) {
        if (reqres.status / 100 == 2) {
          if (!etag.equals(reqres.etag)) {
            break;
          }
        } else {
          return false;
        }
      }
      var delay = manager.serverInfo.waitdelay;
      synchronized (this) {
        try {
          Thread.sleep(delay);
        } catch (final InterruptedException e) {
          throwException(e);
        }
      }
    }

    return true;
  }

  public void doEnddelete(final String description,
                          final String label) {
    if (Util.isEmpty(endDeletes)) {
      return;
    }
    manager.trace("Start: " + description);
    for (var delReq: endDeletes) {
      var req = delReq.makeRequest("DELETE");
      doRequest(req, false, false, false, null, null, label, 0);
    }
    manager.trace(format("%s60%s", "End: " + description, "[DONE]"));
  }

  private static class DoRequestResult {
    boolean ok = true;
    String message;
    //HttpResponse response;
    int status;
    String etag;
    String protocolVersion;
    String reason;
    String responseData;
    List<Header> responseHeaders;

    DoRequestResult() {
    }

    static DoRequestResult ok() {
      return new DoRequestResult();
    }

    static DoRequestResult fail(final String message) {
      var res = new DoRequestResult();
      res.ok = false;
      res.message = message;

      return res;
    }

    public void append(final String val) {
      if ((val == null) || (val.length() == 0)) {
        return;
      }

      if (message == null) {
        message = val;
        return;
      }

      message += "\n";

      message += val;
    }
  }

  private DoRequestResult doRequest(final Request req,
                                    final boolean details,
                                    final boolean doverify,
                                    final boolean forceverify,
                                    final RequestStats stats,
                                    final Map<String, String> etags,
                                    final String label,
                                    final int count) {
    req.count = count;

    if (req instanceof Request.PauseRequest) {
      // Useful for pausing at a particular point
      print("Paused");
      System.console().readLine();

      return DoRequestResult.ok();
    }

    if (hasMissingFeatures()) {
      return DoRequestResult.ok();
    }
    if (hasExcludedFeatures()) {
      return DoRequestResult.ok();
    }

    // Handle special methods
    String ruri = null;

    String methodPar = null;

    var split = req.method.split(" ");
    if (split.length > 1) {
      methodPar = split[1];
    }

    String method = split[0];

    switch (method) {
      case "DELETEALL":
        for (var requri: req.ruris) {
          var hrefs = doFindall(req,
                                new UriIdPw(requri, req.getUser(), req.getPswd()),
                                format("%s | %s", label, "DELETEALL"));
          if (!hrefs.ok) {
            return DoRequestResult.fail(hrefs.message);
          }

          if (!doDeleteall(req, hrefs.val,
                           format("%s | %s", label, "DELETEALL"))) {
            return DoRequestResult.fail(format("DELETEALL failed for: %s",
                    requri));
          }
        }
        return DoRequestResult.ok();

      case "DELAY":
        // ruri contains a numeric delay in seconds
        var delay = Integer.parseInt(req.ruri);
        synchronized (this) {
          try {
            Thread.sleep(delay * 1000);
          } catch (final InterruptedException e) {
            throwException(e);
          }
        }
        return DoRequestResult.ok();

      case "GETNEW":
      case "GETOTHER":
        grabbedLocation = doFindnew(req,
                                    UriIdPw.fromRequest(req),
                                    label,
                                    req.method.equals("GETOTHER"));
        if (req.graburi != null) {
          manager.serverInfo.addextrasubs(new KeyVals(req.graburi,
                                                      grabbedLocation));
        }
        method = "GET";
        ruri = "$";
        break;

      case "FINDNEW":
        grabbedLocation = doFindnew(req,
                                    UriIdPw.fromRequest(req),
                                    label, false);
        if (req.graburi != null) {
          manager.serverInfo.addextrasubs(new KeyVals(req.graburi,
                                                      grabbedLocation));
        }
        return DoRequestResult.ok();

      case "GETCONTAINS":
        grabbedLocation = doFindcontains(req,
                                         UriIdPw.fromRequest(req),
                                         methodPar, label);
        if (grabbedLocation == null) {
          return DoRequestResult.fail("No matching resource");
        }
        if (req.graburi != null) {
          manager.serverInfo.addextrasubs(new KeyVals(req.graburi,
                                                      grabbedLocation));
        }
        method = "GET";
        ruri = "$";
        break;

      case "WAITCOUNT":
        var wcount = Integer.parseInt(methodPar);
        for (var wdruri: req.ruris) {
          var waitres = doWaitcount(req,
                                    new UriIdPw(wdruri, req.getUser(), req.getPswd()),
                                    wcount,
                                    label);
          if (!waitres.ok) {
            return DoRequestResult.fail(format("Count did not change: %s",
                                               waitres.val));
          }
        }

        return DoRequestResult.ok();

      case "WAITDELETEALL":
        for (var wdruri: req.ruris) {
          var waitres = doWaitcount(req,
                                    new UriIdPw(wdruri, req.getUser(), req.getPswd()),
                                    Integer.parseInt(methodPar),
                                    label);
          if (!waitres.ok) {
            return DoRequestResult.fail(
                    format("Count did not change: %s",
                           waitres.message));
          }

          var hrefs = doFindall(req,
                                new UriIdPw(wdruri, req.getUser(), req.getPswd()),
                                format("%s | %s", label, "DELETEALL"));
          if (!hrefs.ok) {
            return DoRequestResult.fail(hrefs.message);
          }

          doDeleteall(req, hrefs.val,
                      format("%s | %s", label, "DELETEALL"));
        }

        return DoRequestResult.ok();
    }

    final DoRequestResult drr = new DoRequestResult();

    ruri = req.getURI();
    if (ruri.equals("$")) {
      ruri = grabbedLocation;
    }

    var headers = req.getHeaders();
    var data = req.getDataVal();

    // Cache delayed delete
    if (req.endDelete) {
      endDeletes.add(new RequestPars(ruri, req));
    }

    if (details) {
      drr.append(format("        %s: %s\n", method, ruri));
    }

    // Special for GETCHANGED
    if (req.method.equals("GETCHANGED")) {
      if (!doWaitchanged(req,
                         new UriIdPw(ruri, req.getUser(), req.getPswd()),
                         etags.get(ruri),
                         label)) {
        return DoRequestResult.fail("Resource did not change");
      }
      method = "GET";
    }

    if (stats != null) {
      stats.startTimer();
    }

    final URI uri;
    try {
      uri = new URIBuilder(new URI(ruri))
              .setScheme(manager.serverInfo.getScheme())
              .setHost(req.host)
              .setPort(req.port)
              .build();
    } catch (final Throwable t) {
      throwException(t);
      return DoRequestResult.fail("Fake for ide"); // fake
    }

    HttpRequestBase meth = HttpUtil.findMethod(method, uri);
    if (meth == null) {
      throwException("No method: " + method);
    }

    var hasUserAgent = false;

    if (!Util.isEmpty(headers)) {
      for (final Header hdr: headers) {
        if (hdr.getName().equalsIgnoreCase("User-Agent")) {
          hasUserAgent = true;
        }
        meth.addHeader(hdr);
      }
    }

    if (!hasUserAgent && (label != null)) {
      meth.addHeader(new BasicHeader("User-Agent",
                                     Utils.encodeUtf8(label)));
    }

    if (data != null) {
      setContent(meth, data.getBytes(), req.getData().contentType);
    }

    String requesttxt = null;
    if (req.printRequest ) {
      requesttxt = "\n-------BEGIN:REQUEST-------\n" +
              data +
              "\n--------END:REQUEST--------\n";
      manager.protocol(requesttxt);
    }

    if (req.httpTrace) {
      httpTraceOn();
    }

    try (CloseableHttpResponse resp =
                 manager.getHttpClient(req.getUser(),
                                       req.getPswd()).execute(meth)) {
      final HttpEntity ent = resp.getEntity();

      if (ent != null) {
        final InputStream in = ent.getContent();

        if (in != null) {
          drr.responseData = readContent(in, ent.getContentLength(),
                                         ContentType.getOrDefault(ent)
                                                    .getCharset());
        }
      }

      drr.reason = resp.getStatusLine().getReasonPhrase();
      drr.protocolVersion = resp.getStatusLine().getProtocolVersion().toString();
      drr.etag = HttpUtil.getFirstHeaderValue(resp, "etag");
      drr.responseHeaders = Arrays.asList(resp.getAllHeaders());
      drr.status = HttpUtil.getStatus(resp);
    } catch (final Throwable t) {
      throwException(t);
    } finally {
      if (req.httpTrace) {
        httpTraceOff();
      }
    }

    if (stats != null) {
      // Stop request timer before verification
      stats.endTimer();
    }

    if (doverify && (drr.responseData != null)) {
      var vres = req.verifyRequest(uri,
                                   drr.responseHeaders,
                                   drr.status,
                                   drr.responseData);
      drr.append(vres.text);
    } else if (forceverify) {
      drr.ok = (drr.status / 100 == 2);
      if (!drr.ok) {
        drr.append(format("Status Code Error: %d", drr.status));
      }
    }

    if (!req.printRequest &&
            (manager.printRequestResponseOnError &&
                     (!drr.ok && !req.waitForSuccess))) {
      manager.protocol(requesttxt);
    }

    if (req.printResponse ||
            (manager.printRequestResponseOnError &&
                     (!drr.ok && (!req.waitForSuccess)))) {
      var responsetxt = "\n-------BEGIN:RESPONSE-------\n" +
              format("%s %s %s\n",
                     drr.protocolVersion,
                     drr.status, drr.reason) +
//              String.valueOf(drr.response.message) +
              drr.responseData +
              "\n--------END:RESPONSE--------\n";
      manager.protocol(responsetxt);
    }

    if (etags != null && (req.method.equals("GET"))) {
      if (drr.etag != null) {
        etags.put(ruri, drr.etag);
      }
    }

    if (req.graburi != null) {
      manager.serverInfo.addextrasubs(new KeyVals(req.graburi,
                                                  grabbedLocation));
    }

    if (req.grabcount != null) {
      var ctr = -1;
      if (drr.ok &&
              (drr.status == 207) &&
              (drr.responseData != null)) {
        final MultiStatusResponse msr =
                multiStatusResponse(drr.responseData);

        ctr = msr.responses.size();
      }

      if (ctr == 0) {
        drr.ok = false;
        drr.append("Could not count resources in response");
      } else {
        manager.serverInfo.addextrasubs(new KeyVals(req.grabcount,
                                                    String.valueOf(ctr)));
      }
    }

    if (!req.grabheader.isEmpty()) {
      for (var prop: req.grabheader) {
        if (!Util.isEmpty(drr.responseHeaders)) {
          manager.serverInfo.addextrasubs(
                  new KeyVals(prop.val,
                              Utils.encodeUtf8(drr.responseHeaders.get(0).getValue())));
        } else {
          drr.ok = false;
          drr.append(format("Header %s was not extracted from response\n",
                            prop.key));
        }
      }
    }

    if (!req.grabproperty.isEmpty()) {
      if (drr.status == 207) {
        for (var prop: req.grabproperty){
          // grab the property here
          var epres = extractProperty(prop.key, drr.responseData);
          if (!epres.ok) {
            drr.ok = false;
            drr.append(format("Property %s was not extracted " +
                                      "from multistatus response",
                              prop.key));
          } else {
            manager.serverInfo.addextrasubs(new KeyVals(prop.val,
                                                        Utils.encodeUtf8(epres.val)));
          }
        }
      }
    }

    if (!Util.isEmpty(req.grabelement)) {
      for (var item: req.grabelement) {
        var elements = extractElements(item.path, drr.responseData);
        if (Util.isEmpty(elements)) {
          drr.ok = false;
          drr.append(format("Element %s was not extracted from response",
                            item.path));
        } else if (item.variables.size() != elements.size()) {
          drr.ok = false;
          drr.append(format("%d found but expecting %d for element %s from response",
                            elements.size(), item.variables.size(),
                            item.path));
        } else {
          var i = 0;
          for (var v: item.variables) {
            var e = elements.get(i);
            i++;

            manager.serverInfo.addextrasubs(
                    new KeyVals(v, contentUtf8(e)));
          }
        }
      }
    }

    if (!Util.isEmpty(req.grabjson)) {
      throwException("Unimplemented");
    }

    /* UNUSED
    if (!Util.isEmpty(req.grabjson)) {
      for (var kv: req.grabjson) {
        // grab the JSON value here
        var pointervalues = extractPointer(kv.path, drr.responseData);
        if (pointervalues == null) {
          drr.ok = false;
          drr.append(format("Pointer %s was not extracted from response",
                            kv.path));
        } else if (kv.variables.size() != pointervalues.size()) {
          drr.ok = false;
          drr.append(format("%d found but expecting %d for pointer %s from response",
                            pointervalues.size(),
                            kv.variables.size(),
                            kv.path));
        } else {
          var i = 0;
          for (var v: kv.variables) {
            var p = pointervalues.get(i);
            i++;

            manager.serverInfo.addextrasubs(new KeyVals(v,
                                                        encodeUtf8(p)));
          }
        }
      }
    }
     */

    if (!Util.isEmpty(req.grabcalprop)) {
      for (var kv: req.grabcalprop) {
        // grab the property here
        var propname = manager.serverInfo.subs(kv.key);
        propname = manager.serverInfo.extrasubs(propname);
        var propvalue = extractCalProperty(propname, drr.responseData);
        if (propvalue == null) {
          drr.ok = false;
          drr.append(format("Calendar property %s was not extracted from response",
                            propname));
        } else {
          manager.serverInfo.addextrasubs(
                  new KeyVals(kv.val,
                              encodeUtf8(propvalue)));
        }
      }
    }

    if (!Util.isEmpty(req.grabcalparam)) {
      for (var kv: req.grabcalparam) {
        // grab the property here
        var path = manager.serverInfo.subs(kv.key);
        path = manager.serverInfo.extrasubs(path);
        var paramvalue = extractCalParameter(path, drr.responseData);
        if (paramvalue == null) {
          drr.ok = false;
          drr.append(format("Calendar Parameter was not extracted from response: %s",
                            path));
        } else {
          manager.serverInfo.addextrasubs(new KeyVals(kv.val,
                                                      encodeUtf8(paramvalue)));
        }
      }
    }

    return drr;
  }

  /** Send content
   *
   * @param content the content as bytes
   * @param contentType its type
   * @throws HttpException if not entity enclosing request
   */
  private static void setContent(final HttpRequestBase req,
                                final byte[] content,
                                final String contentType) {
    if (content == null) {
      return;
    }

    if (!(req instanceof HttpEntityEnclosingRequestBase)) {
      throwException("Invalid operation for method " +
                             req.getMethod());
    }

    final HttpEntityEnclosingRequestBase eem = (HttpEntityEnclosingRequestBase)req;

    final ByteArrayEntity entity = new ByteArrayEntity(content);
    entity.setContentType(contentType);
    eem.setEntity(entity);
  }

  public void parseXML(final Element node) {
    ignoreAll = getYesNoAttributeValue(node,
                                       XmlDefs.ATTR_IGNORE_ALL,
                                       false);
    httpTrace = getYesNoAttributeValue(node, XmlDefs.ATTR_HTTP_TRACE,
                                       false);

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_DESCRIPTION)) {
        description = content(child);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
        parseFeatures(child, true);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
        parseFeatures(child, false);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_START)) {
        startRequests = Request.parseList(manager, child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_TESTSUITE)) {
        var suite = new Testsuite(manager);
        suite.parseXML(child);
        suites.add(suite);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_END)) {
        endRequests = Request.parseList(manager, child);
      }
    }
  }

  public Result<String> extractProperty(final String propertyname,
                                        final String respdata) {
    final MultiStatusResponse msr =
            multiStatusResponse(respdata);

    for (var response: msr.responses) {
      for (var propstat: response.propstats) {
        if ((propstat.status / 100) != 2) {
          continue;
        }

        // Get properties for this propstat
        if (propstat.props.size() != 1) {
          return Result.fail("           Wrong number of DAV:prop elements");
        }

        for (var child: children(propstat.props.get(0))) {
          var tag = child.getTagName();
          if (!tag.equals(propertyname)) {
            continue;
          }

          var subch = children(child);

          if (subch.size() == 0) {
            return new Result<>(content(child));
          }

          // Copy sub-element data as text into one long string and strip leading/trailing space
          var value = new StringBuilder();
          for (var p: subch) {
            value.append(content(p).strip());
          }

          return new Result<>(value.toString());
        }
      }
    }

    return Result.fail(null);
  }

  public List<Element> extractElements (final String elementpath,
                                        final String respdata) {
    final Element rootEl = parseXmlString(respdata).getDocumentElement();

    final String testPath;
    final boolean atRoot;

    if (elementpath.startsWith("/")) {
      testPath = elementpath.substring(1);
      atRoot = true;
    } else {
      testPath = elementpath;
      atRoot = false;
    }

    return findNodes(parseXmlString(respdata),
                     atRoot,
                     testPath);
  }

  /* UNUSED
  public void extractPointer (final String pointer,
                              final String respdata) {
    jp = JSONMatcher(pointer);

    try {
      j = json.loads(respdata);
    } except {
      return null;
    }

    return jp.match(j);
  }
  */

  public String extractCalProperty(final String path,
                                   final String respdata) {
    /* If the path has a $... segment at the end, split it off
       as the desired property value.
     */
    var pos = path.indexOf('$');
    String pvalue = null;
    String ppath = path;
    if (pos > 0) {
      ppath = path.substring(0, pos);
      pvalue = path.substring(pos + 1);
    }
    var prop = calProperty(ppath, pvalue, respdata);
    if (prop == null) {
      return null;
    }

    return prop.getValue();
  }

  public String extractCalParameter(final String path,
                                    final String respdata) {
    /* If the path has a $... segment at the end, split it off
       as the desired property value.
     */
    var pos = path.indexOf('$');
    String pvalue = null;
    String ppath = path;
    if (pos > 0) {
      ppath = path.substring(0, pos);
      pvalue = path.substring(pos + 1);
    }

    // path is a path consisting of component and property names
    // followed by a parameter name
    // e.g. VEVENT/ATTACH/MANAGED-ID
    pos = ppath.lastIndexOf('/');
    var paramName = ppath.substring(pos + 1);
    ppath = ppath.substring(0, pos);

    var prop = calProperty(ppath, pvalue, respdata);

    if (prop == null) {
      return null;
    }

    var param = prop.getParameter(paramName);

    if (param == null) {
      return null;
    }

    return param.getValue();
  }

  private Property calProperty(final String propertyname,
                               final String propertyValue,
                               final String respdata) {
    Component comp = Icalendar.parseText(respdata);

    // propname is a path consisting of component and property names
    // e.g. VEVENT/ATTACH
    var split = propertyname.split("/");

    var spliti = 0;

    while (spliti < split.length) {
      var name = split[spliti];

      var found = false;
      for (var c: comp.getComponents()) {
        if (c.getName().equals(name)) {
          found = true;
          comp = c;
          spliti++;
          break;
        }
      }

      if (!found) {
        break;
      }
    }

    if (spliti == 0) {
      // Didn't match top level component;
      return null;
    }

    // Try properties

    var name = split[spliti];
    var props = comp.getProperties(name);

    if (propertyValue != null) {
      for (var prop: props) {
        if (prop.getValue().equals(propertyValue)) {
          return prop;
        }
      }

      return null;
    }

    if (Util.isEmpty(props)) {
      return null;
    }

    return props.get(0);
  }

  String readContent(final InputStream in, final long expectedLen,
                     final Charset characterSet) throws Throwable {
    StringBuilder res = new StringBuilder();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = 0;
    String charset;

    if (characterSet == null) {
      charset = StandardCharsets.UTF_8.toString();
    } else {
      charset = characterSet.toString();
    }

    //if (logger.debug()) {
    //  System.out.println("Read content - expected=" + expectedLen);
    //}

    boolean hadLf = false;
    boolean hadCr = false;

    while ((expectedLen < 0) || (len < expectedLen)) {
      int ich = in.read();
      if (ich < 0) {
        break;
      }

      len++;

      if (ich == '\n') {
        if (res.length() == 0) {
          continue;
        }
        if (hadLf) {
          res.append('\n');
          hadLf = false;
          hadCr = false;
        } else {
          hadLf = true;
        }
        continue;
      }

      if (ich == '\r') {
        if (hadCr) {
          res.append('\r');
          hadLf = false;
          hadCr = false;
        } else {
          hadCr = true;
        }
        continue;
      }

      if (hadCr || hadLf) {
        hadLf = false;
        hadCr = false;

        if (baos.size() > 0) {
          res.append(new String(baos.toByteArray(), charset));
          if (hadCr) {
            res.append('\r');
          } else {
            res.append('\n');
          }
        }

        baos.reset();
        baos.write(ich);
        continue;
      }

      baos.write(ich);
    }

    if (baos.size() > 0) {
      res.append(new String(baos.toByteArray(), charset));
    }

    return res.toString();
  }

  /*
  private static class CaldavAuthenticator extends Authenticator {
    private String user;
    private char[] pw;

    CaldavAuthenticator(String user, String pw) {
      this.user = user;
      this.pw = pw.toCharArray();
    }

    protected PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(user, pw);
    }
  }

  private static void calInfo(Calendar cal) throws Throwable {
    ComponentList clist = cal.getComponents();

    Iterator it = clist.iterator();

    while (it.hasNext()) {
      Object o = it.next();

      msg("Got component " + o.getClass().getName());

      if (o instanceof VEvent) {
        VEvent ev = (VEvent)o;

        eventInfo(ev);
        / *
      } else if (o instanceof VTimeZone) {
        VTimeZone vtz = (VTimeZone)o;

        debugMsg("Got timezone: \n" + vtz.toString());
        * /
      }
    }
  }

  /* POSTGRES
  public void postgresInit () {
    """
        Initialize postgres statement counter
        """
      if (manager.postgresLog) {
        if (os.path.exists(manager.postgresLog) {
          return int(commands.getoutput("grep \"LOG:  statement:\" %s | wc -l" % (manager.postgresLog,)));

          return 0

          public void postgresResult (startCount, indent) {

            if (manager.postgresLog) {
              if (os.path.exists(manager.postgresLog) {
                newCount = int(commands.getoutput("grep \"LOG:  statement:\" %s | wc -l" % (manager.postgresLog,)));
              } else {
                newCount = 0
            manager.trace(format("Postgres Statements: %d", newCount - startCount));
*/
}