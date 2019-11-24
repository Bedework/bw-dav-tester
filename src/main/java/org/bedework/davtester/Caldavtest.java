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

import org.bedework.davtester.request.Request;
import org.bedework.util.misc.Util;

import org.apache.http.HttpResponse;
import org.w3c.dom.Element;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.bedework.davtester.Manager.RESULT_FAILED;
import static org.bedework.davtester.Manager.RESULT_IGNORED;
import static org.bedework.davtester.Manager.RESULT_OK;
import static org.bedework.davtester.Manager.TestResult;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.util.xml.XmlUtil.nodeMatches;
import static org.bedework.util.xml.tagdefs.XcardTags.uid;

/*
from cStringIO import StringIO
try {
    # Treat pycalendar as optional
    from pycalendar.icalendar.calendar import Calendar
except ImportError:
    pass
from src.httpshandler import SmartHTTPConnection
from src.jsonPointer import JSONMatcher
from src.manager import manager
from src.request import data, pause
from src.request import request
from src.request import stats
from src.testsuite import testsuite
from src.xmlUtils import nodeForPath, xmlPathSplit
from xml.etree.cElementTree import ElementTree, tostring
import commands
import httplib
import json
import os
import rfc822
import socket
import XmlDefs
import sys
import time
import traceback
import urllib
import urlparse

"""
Patch the HTTPConnection.send to record full request details
"""

httplib.HTTPConnection._send = httplib.HTTPConnection.send


public void recordRequestHeaders (str) {
    if ! hasattr ("requestData") {
        requestData = ""
    requestData += str
    httplib.HTTPConnection._send (str)  # @UndefinedVariable

httplib.HTTPConnection.send = recordRequestHeaders


public void getVersionStringFromResponse(response) {

    if response.version == 9:
        return "HTTP/0.9"
    } else if (response.version == 10:
        return "HTTP/1.0"
    } else if (response.version == 11:
        return "HTTP/1.1"
    } else {
        return "HTTP/?.?"

*/
/**
 * Class to encapsulate a single caldav test run.
 */
class Caldavtest extends DavTesterBase {
  private class RequestPars {
    final String host;
    final int port;

    final String uri;
    final String user;
    final String pswd;
    final String cert;

    RequestPars(final String host, final int port, final String uri,
                final String user,
                final String pswd, final String cert) {
      this.host = host;
      this.port = port;
      this.uri = uri;
      this.user = user;
      this.pswd = pswd;
      this.cert = cert;
    }

    Request makeRequest(final String method) {
      var req = new Request(manager);

      req.method = method;
      req.host = host;
      req.port = port;

      req.ruris.add(uri);
      req.ruri = uri;
      if (user != null) {
        req.user = user;
      }
      if (pswd != null) {
        req.pswd = pswd;
      }
      req.cert = cert;

      return req;
    }
  }

    private Path testPath;

  boolean ignoreAll;
  private boolean only;

  private List<String> startRequests = new ArrayList<>();
  private List<String> endRequests = new ArrayList<>();
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

    final InputStream is = new FileInputStream(testPath.toFile());

    doc = XmlUtils.parseXml(is);
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
      doEnddelete("Deleting Requests...", label = "%s | %s" % (name,
                  "END_DELETE"));
      doRequests("End Requests...", end_requests, false, false,
                 "%s | %s" % (name, "END_REQUESTS"), 1);
      return res;
    } catch (final Throwable t) {
      manager.testFile(name,
                       "FATAL ERROR: %s" % (e, ), manager.RESULT_ERROR);
      ;

      if (manager.debug()) {
        manager.error(t);
      }

      return TestResult.failed();
    }
  }

  public TestResult runTests(final String label){
    var res = new TestResult();

    var testfile = manager.testFile(name, description, null);
    for (var suite : suites) {
      res.add(runTestSuite(testfile, suite,
                           format("%s | %s",
                                  label,
                                  suite.name)));
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
      var etags = new ArrayList<String>();
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
        var result = runTest(testsuite, test, etags, onlyTests,
                             format("%s | %s", label, test.name));
        if (result == 't') {
          res.ok += 1;
        } else if (result == 'f') {
          res.failed += 1;
        } else {
          res.ignored += 1;
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
                       final List<String> etags,
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
    var resulttxt = "";
    // POSTGRES postgresCount = postgresInit();
    final RequestStats reqstats;
    if (test.stats) {
      reqstats = new RequestStats();
    } else {
      reqstats = null;
    }

    IntStream.range(0, test.count).forEachOrdered(ctr -> {
      var failed = false;
      var reqCount = 1;
      for (var req : test.requests) {
        t = time.time() + (manager.serverInfo.waitsuccess if
        req.waitForSuccess  else 100);
        while (t > time.time()) {
          failed = false;
          if (req.iterateData) {
            if (!req.hasNextData()) {
              manager.testResult(testsuite, test.name,
                                 "      No iteration data - ignored",
                                 RESULT_IGNORED, null);
              return 'i';
            }

            while (req.getNextData()) {
              var reqres = doRequest(req, test.details, true, false,
                                     reqstats, etags,
                      format("%s | #%s", label, reqCount + 1),
                      ctr + 1);
              if (!reqres.ok) {
                failed = true;
                break;
              }
            }
          } else {
            var resreq = doRequest(
                    req, test.details, true, false, reqstats, etags,
                    format("%s | #%s", label, reqCount + 1), ctr + 1);
            if (!resreq.ok) {
              failed = true;
            }
          }

          if (!failed || !req.waitForSuccess) {
            break;
          }
        }
        if (failed) {
          break;
        }
      }
    });

    var addons = new Properties();
    if (resulttxt != null) {
      manager.trace(resulttxt);
    }

    if (test.stats) {
      manager.trace(format("    Total Time: %.3f secs",
                           reqstats.totaltime, ), indent = 8);
      manager.trace(format("    Average Time: %.3f secs",
                           reqstats.totaltime / reqstats.count, ),
                    indent = 8);
      var timing = new Properties();
      timing.put("total", reqstats.totaltime);
      timing.put("average", reqstats.totaltime / reqstats.count);
      addons.put("timing", timing);
    }

    // postgresResult(postgresCount, indent=8);
    final int rcode;
    if (result) {
      rcode = RESULT_OK;
    } else {
      rcode = RESULT_FAILED;
    }
    manager.testResult(testsuite, test.name, resulttxt, rcode,
                       addons);
    return ["f", "t"][result]
  }

  public boolean doRequests(final String description,
                            final List<String> list,
                            final boolean doverify,
                            final boolean forceverify,
                            final String label,
                            final int count) {
    if (Util.isEmpty(list)) {
      return true;
    }

    manager.trace("Start: " + description);

    var reqCount = 1;
    for (var req: list) {
      var resreq = doRequest(
              req, false, doverify, forceverify,
              format("%s | #%s", label, String.valueOf(req_count + 1)),
              count);
      if (!resreq.ok) {
        resulttxt += format(
                "\nFailure during multiple requests " +
                        "#%d out of %d, request=%s",
                reqCount, list.size(),
                String.valueOf(req));
        break;
      }
    }
    manager.trace(format("%d60 FAILED: %d5, OK: %d5",
                         "End: " + description,
                         result.ok, result.failed));
    if (resulttxt != null) {
      manager.trace(resulttxt);
    }
    return result;
  }

  public void doGet (Request originalRequest,
                     final UriIdPw uip,
                     final String label) {
    var req = uip.makeRequest(originalRequest, "GET");

    var reqres = doRequest(req, false, false, false, null, label, 1);
        if (reqres.status / 100 != 2) {
          return false,null
        }

    return true, respdata
  }

  public List<UriIdPw> doFindall (final Request originalRequest,
                                  final UriIdPw uip,
                                  final String label) {
    var hrefs = new ArrayList<UriIdPw>();

    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

    req.data = data(manager);
    req.data.value = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop>" +
            "<D:getetag/>" +
            "</D:prop>" +
            "</D:propfind>";
        
    req.data.content_type = "text/xml"
    var reqres = doRequest(req, false, false, false, null, label, 1);
    if (reqres.ok && (response != null) && (response.status == 207) && (respdata != null)
    {
      try {
        tree = ElementTree(file = StringIO(respdata));
      } catch (final Throwable t) {
        return ();
      }

      request_uri = req.getURI(manager.serverInfo);
      for (var response : tree.findall("{DAV:}response")) {

        // Get href for this response
        href = response.findall("{DAV:}href");
        if (len(href) != 1) {
          return false,
          "           Wrong number of DAV:href elements\n"
        }
        href = href[0].text
        if (href != request_uri) {
          hrefs.add(new UriIdPw(href, uip.user, uip.pswd));
        }
      }
    }

    return hrefs;
  }

  public boolean doDeleteall(final Request originalRequest,
                             final List<UriIdPw> deletes,
                             final String label) {
    if (Util.isEmpty(deletes)) {
      return true;
    }
    for (var uip : deletes) {
      var req = uip.makeRequest(originalRequest, "DELETE");

      var reqres = doRequest(req, false, false, false, null,
                             label, 1);
      if (reqres.status / 100 != 2) {
        return false;
      }
    }

    return true;
  }

  public void doFindnew (final Request originalRequest,
                         final UriIdPw uip,
                         final String label,
                         final boolean other) {
    var hresult = "";

    uri = uip.ruri;
    if (other) {
      uri = manager.serverInfo.extrasubs(uri);
      skip = uri
      uri = "/".join(uri.split("/")[:-1]) + "/"
    } else {
      skip = null;
    }
    possible_matches = set();

    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

    req.data = data(manager);
    req.data.value = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop>" +
            "<D:getetag/>" +
            "<D:getlastmodified/>" +
            "</D:prop>" +
            "</D:propfind>";

    req.data.content_type = "text/xml"
    var reqres = doRequest(req, false, false, false, null, label="%s | %s" % (label, "FINDNEW"), 1);
    if (reqres.ok && (response != null) && (response.status == 207) && (respdata != null)) {
      try {
        tree = ElementTree(file=StringIO(respdata));
      } catch (final Throwable t) {
        throwException(t);
        return hresult; // fake
      }

      latest = 0;
      request_uri = req.getURI(manager.serverInfo);
      for (var response: tree.findall("{DAV:}response")) {

        // Get href for this response
        href = response.findall("{DAV:}href");
        if (len(href) != 1) {
          return false,
          "           Wrong number of DAV:href elements\n";
        }
        href = href[0].text;
        if (href != request_uri && (!other|| href != skip) {

          // Get all property status
          propstatus = response.findall("{DAV:}propstat");
          for (props: propstatus) {
            // Determine status for this propstat
            status = props.findall("{DAV:}status");
            if (len(status) == 1) {
              statustxt = status[0].text
              status = false
              if (statustxt.startsWith("HTTP/1.1 ") && (len(statustxt) >= 10)
              {
                status = (statustxt[9] == "2");
              }
            } else {
              status = false;
            }

            if (status) {
              // Get properties for this propstat
              prop = props.findall("{DAV:}prop");
              for (el: prop) {

                // Get properties for this propstat
                glm = el.findall(
                        "{DAV:}getlastmodified");
                if (len(glm) != 1) {
                  continue;
                }
                value = glm[0].text
                value = rfc822.parsedate(value);
                value = time.mktime(value);
                if (value > latest) {
                  possible_matches.clear();
                  possible_matches.add(href);
                  latest = value;
                } else if (value == latest) {
                  possible_matches.add(href);
                }
              }
            } else if (!hresult) {
              possible_matches.add(href);
            }
          }
        }
      }
    }

    if (len(possible_matches) == 1) {
      hresult = possible_matches.pop();
    } else if (len(possible_matches) > 1) {
      not_seen_before = possible_matches - previously_found
      if (len(not_seen_before) == 1) {
        hresult = not_seen_before.pop();
      }
    }
    if (hresult) {
      previously_found.add(hresult);
    }
    return hresult;
  }

  public void doFindcontains (final Request originalRequest,
                              final UriIdPw uip,
                              final String match,
                              final String label) {
    hresult = "";

    uri = uip.ruri;

    var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

    req.data = data(manager);
    req.data.value = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
            "<D:propfind xmlns:D=\"DAV:\">" +
            "<D:prop>" +
            "<D:getetag/>" +
            "</D:prop>" +
            "</D:propfind>";

    req.data.content_type = "text/xml"
    var reqres = doRequest(req, false, false, false, null, label="%s | %s" % (label, "FINDNEW"), 1);
    if (result && (response != null) && (response.status == 207) && (respdata != null) {
      try {
        tree = ElementTree(file=StringIO(respdata));
      } catch (final Throwable t) {
        throwException(t);
        return hresult; // fake
      }

      request_uri = req.getURI(manager.serverInfo);
      for (var response: tree.findall("{DAV:}response")) {

        // Get href for this response
        href = response.findall("{DAV:}href");
        if (len(href) != 1) {
          return false,
          "           Wrong number of DAV:href elements\n";
        }
        href = href[0].text
        if (href != request_uri) {

          respdata = doGet(req,
                           new UriIdPw(href, uip.user, uip.pswd), label);
          if (respdata.find(match) != -1) {
            break;
          }
        }
      }
    } else {
      href = null;
    }

    return href;
  }

  public void doWaitcount(final Request originalRequest,
                           final UriIdPw uip,
                           final int count,
                           final String label) {
    hrefs = []
    for (var ignore: range(manager.serverInfo.waitcount)) {
      var req = uip.makeRequest(originalRequest, "PROPFIND", "1");

      req.data = data(manager);
      req.data.value = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
              "<D:propfind xmlns:D=\"DAV:\">" +
              "<D:prop>" +
              "<D:getetag/>" +
              "</D:prop>" +
              "</D:propfind>";
      req.data.content_type = "text/xml"
      var reqres = doRequest(req, false, false, false, label="%s | %s %d" % (label, "WAITCOUNT", count));
      hrefs = []
      if (result && (response != null) && (response.status == 207) && (respdata != null)
      {
        tree = ElementTree(file = StringIO(respdata));

        for (var response : tree.findall("{DAV:}response")) {
          href = response.findall("{DAV:}href")[0];
          if (href.text.rstrip("/") != collection[0]
                  .rstrip("/")) {
            hrefs.append(href.text);
          }
        }

        if (len(hrefs) == count) {
          return true,null;
        }
      }
      delay = manager.serverInfo.waitdelay
      starttime = time.time();
      while (time.time() < starttime + delay) {
        pass;
      }
    }

    if (!manager.debug || Util.isEmpty(hrefs)) {
      return false, len(hrefs);
    }
    // Get the content of each resource
    rdata = "";
    for (var href: hrefs) {
      result, respdata = doGet(req, new UriIdPw(href, uip.user, uip.pswd), label);
      test = "unknown";
      if (respdata.startsWith("BEGIN:VCALENDAR") {
        uid = respdata.find("UID:");
        if (uid != -1) {
          uid = respdata[uid + 4:uid + respdata[uid:].
          find("\r\n")];
          test = uidmaps.get(uid, "unknown");
        }
      }
      rdata += "\n\nhref: {h}\ntest: {t}\n\n{r}\n".format(h=href, t=test, r=respdata);
    }

    return false, rdata;
  }

  public void doWaitchanged (final Request originalRequest,
                             final UriIdPw uip,
                             final String etag,
                             final String label) {
    for (var ignore: range(manager.serverInfo.waitcount)) {
      var req = uip.makeRequest(originalRequest, "HEAD");

      var reqres = doRequest(req, false, false, false, null, label="%s | %s" % (label, "WAITCHANGED"), 1);
      if (reqres.ok && (response != null) {
        if (response.status / 100 == 2) {
          hdrs = response.msg.getheaders("Etag");
          if (hdrs) {
            newetag = hdrs[0].encode("utf-8");
            if (newetag != etag) {
              break;
            }
          }
        } else {
          return false;
        }
      }
      delay = manager.serverInfo.waitdelay;
      starttime = time.time();
      while (time.time() < starttime + delay) {
        pass;
      }
    }

    return true;
  }

  public boolean doEnddelete(final String description,
                             final String label) {
    if (Util.isEmpty(endDeletes)) {
      return true;
    }
    manager.message("trace", "Start: " + description);
    for (var delReq: endDeletes) {
      var req = delReq.makeRequest("DELETE");
      doRequest(req, false, false, label);
    }
    manager.trace(format("%s60$s", "End: " + description, "[DONE]")));
  }

  private static class DoRequestResult {
    boolean ok = true;
    String message;
    HttpResponse response;
    int status;
    String responseData;

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
  }

  private DoRequestResult doRequest(final Request req,
                                    final boolean details,
                                    final boolean doverify,
                                    final boolean forceverify,
                                    final RequestStats stats,
                                    final List<String> etags,
                                    final String label,
                                    final int count) {
        req.count = count

    if (isinstance(req, pause)) {
      // Useful for pausing at a particular point
      print "Paused"
      sys.stdin.readline();
      return DoRequestResult.ok();
    }

    if (hasMissingFeatures()) {
      return DoRequestResult.ok();
    }
    if (hasExcludedFeatures()) {
      return DoRequestResult.ok();
    }

    // Handle special methods

    switch (req.method) {
      case "DELETEALL":
        for (var ruri: req.ruris) {
          var hrefs = doFindall(req,
                                new UriIdPw(ruri, req.user, req.pswd),
                                format("%s | %s", label, "DELETEALL"));
          if (!doDeleteall(req, hrefs, label="%s | %s" % (label, "DELETEALL")) {
            return false,"DELETEALL failed for: {r}"
                    .format(r = ruri), null, null
          }
        }
        return DoRequestResult.ok();

      case "DELAY":
        // ruri contains a numeric delay in seconds
        delay = int(req.ruri);
        starttime = time.time();
        while (time.time() < starttime + delay) {
          pass
        }
        return DoRequestResult.ok();

      case "GETNEW":
        grabbedlocation = doFindnew(req,
                                    UriIdPw.fromRequest(req),
                                    label);
        if (req.graburi) {
          manager.serverInfo.addextrasubs({req.graburi:
          grabbedlocation});
        }
        req.method = "GET"
        req.ruri = "$";
        break;

      case "FINDNEW":
        grabbedlocation = dofindnew(req, UriIdPw.fromRequest(req), label);
        if (req.graburi) {
          manager.serverInfo.addextrasubs({req.graburi:
          grabbedlocation});
        }
        return DoRequestResult.ok();

      case "GETOTHER":
        collection = (req.ruri, req.user, req.pswd);
        grabbedlocation = doFindnew(req, UriIdPw.fromRequest(req), label, true);
        if (req.graburi) {
          manager.serverInfo.addextrasubs({req.graburi:
          grabbedlocation});
        }
        req.method = "GET"
        req.ruri = "$";
        break;

      case "GETCONTAINS":
        match = req.method[12:]
        grabbedlocation = dofindcontains(req, UriIdPw.fromRequest(req), match, label);
        if (!grabbedlocation) {
          return false,"No matching resource", null, null
        }
        if (req.graburi) {
          manager.serverInfo.addextrasubs({req.graburi:
          grabbedlocation});
        }
        req.method = "GET"
        req.ruri = "$"
        break;

      default: {
        if (req.method.startsWith("WAITCOUNT")) {
          count = int(req.method[10:]);
          for (ruri: req.ruris) {
            waitresult, waitdetails = doWaitcount(req,
                                                  new UriIdPw(ruri, req.user, req.pswd),
                                                  count,
                                                  label = label);
            if (!waitresult) {
              return false,"Count did not change: {w}"
                      .format(w = waitdetails), null, null
            } else {
              return DoRequestResult.ok();
            }
          }
        } else if (req.method.startsWith("WAITDELETEALL")) {
          var count = Integer.parseInt(req.method.substring("WAITDELETEALL".length()));
          for (var ruri: req.ruris) {
            waitresult, waitdetails = dowaitcount(req,
                                                  new UriIdPw(ruri, req.user, req.pswd),
                                                  count, label);
            if (waitresult) {
              hrefs = dofindall(req, collection, label="%s | %s" % (label, "DELETEALL"));
              dodeleteall(req, hrefs, label="%s | %s" % (label, "DELETEALL"));
            } else {
              return false,"Count did not change: {w}"
                      .format(w = waitdetails), null, null
            }
          }
        } else {
          return DoRequestResult.fail("Unknown method " + req.method);
        }
      }
    }
    result = true
    resulttxt = ""
    response = null
    respdata = null

    method = req.method
    uri = req.getURI(manager.serverInfo);
    if (uri == "$") {
      uri = grabbedlocation;
    }
    headers = req.getHeaders(manager.serverInfo);
    data = req.getData();

    // Cache delayed delete
    if (req.end_delete) {
      end_deletes.append((uri, req, ));
    }

    if (details) {
      resulttxt += "        %s: %s\n" % (method, uri);
    }

    // Special for GETCHANGED
    if (req.method == "GETCHANGED") {
      if (!doWaitchanged(
              req,
              uri, etags[uri], req.user, req.pswd,
              label)) {
        return DoRequestResult.fail("Resource did not change");
      }
      method = "GET";
    }

    // Start request timer if required
    if (stats) {
      stats.startTimer();
    }

    // Do the http request
    http = SmartHTTPConnection(
            req.host,
            req.port,
            manager.serverInfo.ssl,
            afunix=req.afunix,
            cert=os.path.join(manager.serverInfo.certdir, req.cert) if req.cert  else null
        );

    if ('User-Agent' not in headers && (label != null)) {
      headers['User-Agent'] = label.encode("utf-8");
    }

    try {
      puri = list(urlparse.urlparse(uri));
      if (req.ruri_quote) {
        puri[2] = urllib.quote(puri[2]);
      }
      quri = urlparse.urlunparse(puri);

      http.request(method, quri, data, headers);

      response = http.getresponse();

      respdata = null
      respdata = response.read();

    } finally {
      http.close();
    }

    // Stop request timer before verification
    if (stats) {
      stats.endTimer();
    }

    if (doverify && (respdata != null)) {
      result, txt = verifyrequest(req, uri, response, respdata);
      resulttxt += txt
    } else if (forceverify) {
      result = (response.status / 100 == 2);
      if (!result) {
        resulttxt += "Status Code Error: %d" % response.status
      }
    }

    if (req.printRequest ||
            (manager.printRequestResponseOnError &&
                     (!result && (not req.waitForSuccess)))) {
      requesttxt = "\n-------BEGIN:REQUEST-------\n"
      requesttxt += http.requestData
      requesttxt += "\n--------END:REQUEST--------\n"
      manager.message("protocol", requesttxt);
    }

        if (req.printResponse||
                (manager.printRequestResponseOnError &&
                         (!result && (!req.wait_for_success)))) {
      responsetxt = "\n-------BEGIN:RESPONSE-------\n"
      responsetxt += "%s %s %s\n" % (getVersionStringFromResponse(
              response), response.status, response.reason,);
      responsetxt += String.valueOf(response.msg) + "\n" + respdata
      responsetxt += "\n--------END:RESPONSE--------\n"
      manager.message("protocol", responsetxt);
    }

    if (etags != null && (req.method == "GET")) {
      hdrs = response.msg.getheaders("Etag");
      if (hdrs) {
        etags[uri] = hdrs[0].encode("utf-8");
      }
    }

    if (req.graburi) {
      manager.serverInfo.addextrasubs({req.graburi:grabbedlocation})
      ;
    }

    if (req.grabcount) {
      ctr = null;
      if (result && (response != null) && (response.status == 207) && (respdata != null)
      {
        tree = ElementTree(file = StringIO(respdata));
        ctr = len(tree.findall("{DAV:}response")) - 1;
      }

      if (ctr == null|| ctr == -1) {
        result = false
        resulttxt += "\nCould not count resources in response\n"
      } else {
        manager.serverInfo.addextrasubs({req.grabcount:
        String.valueOf(ctr)});
      }
    }

    if (req.grabheader) {
      for (hdrname, variable:req.grabheader){
        hdrs = response.msg.getheaders(hdrname);
        if (hdrs) {
          manager.serverInfo.addextrasubs({variable:
          hdrs[0].encode("utf-8")});
        } else {
          result = false
          resulttxt += "\nHeader %s was not extracted from response\n" % (hdrname,)
          ;
        }
      }
    }

    if (req.grabproperty) {
      if (response.status == 207) {
        for (propname, variable:req.grabproperty){
          // grab the property here
          propvalue = extractProperty(propname, respdata);
          if (propvalue == null) {
            result = false
            resulttxt += "\nProperty %s was not extracted from multistatus response\n" % (propname,)
            ;
          } else {
            manager.serverInfo.addextrasubs({variable:
            propvalue.encode("utf-8")});
          }
        }
      }
    }

    if (req.grabelement) {
      for (item: req.grabelement) {
        if (len(item) == 2) {
          elementpath, variables = item;
          parent = null;
        } else {
          elementpath, parent, variables = item;
          parent = manager.serverInfo.extrasubs(parent);
        }
        // grab the property here
        elementvalues = extractElements(elementpath, parent, respdata);
        if (elementvalues == null) {
          result = false;
          resulttxt += "\nElement %s was not extracted from response\n" % (elementpath,);
        } else if (len(variables) != len(elementvalues) {
          result = false
          resulttxt += "\n%d found but expecting %d for element %s from response\n" % (len(elementvalues), len(variables), elementpath,);
        } else {
          for (variable, elementvalue: zip(variables, elementvalues)
          {
            manager.serverInfo.addextrasubs({variable:
            elementvalue.encode("utf-8") if elementvalue else""})
            ;
          }
        }
      }
    }

    if (req.grabjson) {
      for (pointer, variables: req.grabjson) {
        // grab the JSON value here
        pointervalues = extractPointer(pointer, respdata);
        if (pointervalues == null) {
          result = false
          resulttxt += "\Pointer %s was not extracted from response\n" % (pointer,);
        } else if (len(variables) != len(pointervalues) {
          result = false
          resulttxt += "\n%d found but expecting %d for pointer %s from response\n" % (len(pointervalues), len(variables), pointer,);
        } else {
          for (variable, pointervalue: zip(variables, pointervalues)
          {
            manager.serverInfo.addextrasubs({variable:
            pointervalue.encode("utf-8") if pointervalue else""});
          }
        }
      }
    }

    if (req.grabcalprop) {
      for (propname, variable:req.grabcalprop){
        // grab the property here
        propname = manager.serverInfo.subs(propname);
        propname = manager.serverInfo.extrasubs(propname);
        propvalue = extractCalProperty(propname, respdata);
        if (propvalue == null) {
          result = false
          resulttxt += "\nCalendar property %s was not extracted from response\n" % (propname,)
          ;
        } else {
          manager.serverInfo.addextrasubs({variable:
          propvalue.encode("utf-8")});
        }
      }
    }

    if (req.grabcalparam) {
      for (paramname, variable:req.grabcalparam) {
        // grab the property here
        paramname = manager.serverInfo.subs(paramname);
        paramname = manager.serverInfo.extrasubs(paramname);
        paramvalue = extractCalParameter(paramname, respdata);
        if (paramvalue == null) {
          result = false;
          resulttxt += "\nCalendar Parameter %s was not extracted from response\n" % (paramname,)
          ;
        } else {
          manager.serverInfo.addextrasubs({variable:paramvalue.encode("utf-8")});
        }
      }
    }

    return new DoRequestResult(result, resulttxt, response, respdata);
  }

  public void parseXML (final Element node) {
    ignore_all = node.get(XmlDefs.ATTR_IGNORE_ALL,
                          XmlDefs.ATTR_VALUE_NO) == XmlDefs.ATTR_VALUE_YES

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
        startRequests = request.parseList(manager, child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_TESTSUITE)) {
        var suite = new Testsuite(manager);
        suite.parseXML(child);
        suites.add(suite);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_END)) {
        endRequests = request.parseList(manager, child);
      }
    }
  }

  public void extractProperty (propertyname, respdata) {

    try {
      tree = ElementTree(file=StringIO(respdata));
    } catch (final Throwable t) {
      return null;
    }

    for (var response: tree.findall("{DAV:}response") {
      // Get all property status
      propstatus = response.findall("{DAV:}propstat");
      for (var props: propstatus) {
        // Determine status for this propstat
        status = props.findall("{DAV:}status");
        if (len(status) == 1) {
          statustxt = status[0].text
          status = false
          if (statustxt.startsWith("HTTP/1.1 ") && (len(statustxt) >= 10)
          {
            status = (statustxt[9] == "2");
          }
        } else {
          status = false;
        }

        if (!status) {
          continue;
        }

        // Get properties for this propstat
        prop = props.findall("{DAV:}prop");
        if (len(prop) != 1) {
          return false,
          "           Wrong number of DAV:prop elements\n";
        }

        for (var child: prop[0].getchildren()) {
          fqname = child.tag
          if (len(child)) {
            // Copy sub-element data as text into one long string and strip leading/trailing space
            value = ""
            for (var p: child.getchildren() {
              temp = tostring(p);
              temp = temp.strip();
              value += temp
            }
          } else{
            value = child.text
          }

          if (fqname == propertyname) {
            return value;
          }
        }
      }
    }

    return null;
  }

  public void extractElement (elementpath, respdata) {
    try {
      tree = ElementTree();
      tree.parse(StringIO(respdata));
    } except {
      return null;
    }

    // Strip off the top-level item
    if (elementpath[0] == '/') {
      elementpath = elementpath[1:]
      splits = elementpath.split('/', 1);
      root = splits[0]
      if (tree.getroot().tag != root) {
        return null
      }
      if (len(splits) == 1) {
        return tree.getroot().text;
      }
      elementpath = splits[1]
    }

    e = tree.find(elementpath);
    if (e != null) {
      return e.text
    }

    return null;
  }

  public void extractElements (elementpath, parent, respdata) {
    try {
      tree = ElementTree();
      tree.parse(StringIO(respdata));
    } except {
      return null;
    }

    if (parent) {
      tree_root = nodeForPath(tree.getroot(), parent);
      if (!tree_root) {
        return null;
      }
      tree_root = tree_root[0];

      // Handle absolute root element
      if (elementpath[0] == '/') {
        elementpath = elementpath[1:]
      }
      root_path, child_path = xmlPathSplit(elementpath);
      if (child_path) {
        if (tree_root.tag != root_path) {
          return null;
        }
        e = tree_root.findall(child_path);
      } else {
        e = (tree_root,);
      }
    } else {
      // Strip off the top-level item
      if (elementpath[0] == '/') {
        elementpath = elementpath[1:]
        splits = elementpath.split('/', 1);
        root = splits[0]
        if (tree.getroot().tag != root) {
          return null;
        }

        if (len(splits) == 1) {
          return tree.getroot().text;
        }

        elementpath = splits[1]
      }
      e = tree.findall(elementpath);
    }

    if (e != null) {
      return [item.text for item:
      e];
    }

    return null;
  }

  public void extractPointer (pointer, respdata) {
    jp = JSONMatcher(pointer);

    try {
      j = json.loads(respdata);
    } except {
      return null;
    }

    return jp.match(j);
  }

  public void extractCalProperty (propertyname, respdata) {

    prop = _calProperty(propertyname, respdata);
    return prop.getValue().getValue() if prop  else null;
  }

  public void extractCalParameter (parametername, respdata) {
    // propname is a path consisting of component names and the last one a property name
    // e.g. VEVENT/ATTACH
    bits = parametername.split("/");
    propertyname = "/".join(bits[:-1]);
    param = bits[-1]
    bits = param.split("$");
    pname = bits[0]
    if (len(bits) > 1) {
      propertyname += "$%s" % (bits[1],);
    }

    prop = _calProperty(propertyname, respdata);

    try {
      return prop.getParameterValue(pname) if prop  else null
    } except KeyError {
      return null
    }
  }

  public void _calProperty (propertyname, respdata) {

    try {
      cal = Calendar.parseText(respdata);
    } catch (final Throwable t) {
      return null
    }

    // propname is a path consisting of component names and the last one a property name
    // e.g. VEVENT/ATTACH
    bits = propertyname.split("/");
    components = bits[:-1]
    prop = bits[-1]
    bits = prop.split("$");
    pname = bits[0]
    pvalue = bits[1] if len(bits) > 1  else null

    while (components) {

      var found = false;
      for (c: cal.getComponents()) {
        if (c.getType() == components[0]) {
          cal = c
          components = components[1:]
          found = true;
          break
        }
      }
      if (!found) {
        break;
      }
    }

    if (components) {
      return null
    }

    props = cal.getProperties(pname);
    if (pvalue) {
      for (prop: props) {
        if (prop.getValue().getValue() == pvalue) {
          return prop
        }
      }
      return null
    }

    return props[0] if props  else null
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