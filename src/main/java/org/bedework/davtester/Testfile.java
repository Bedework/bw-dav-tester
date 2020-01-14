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
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;
import static org.bedework.davtester.Manager.RESULT_IGNORED;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.childrenMatching;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Class to encapsulate a single test file. (Was Caldavtest)
 */
public class Testfile extends DavTesterBase {
  @Override
  public String getKind() {
    return "CALDAVTEST";
  }

  public static class RequestPars {
    final String uri;
    final Request req;

    public RequestPars(final String uri,
                       final Request req) {
      this.uri = uri;
      this.req = req;
    }

    public Request makeRequest(final String method,
                               final Manager manager) {
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

  public Path testPath;

  boolean ignoreAll;
  public boolean only;

  private List<Request> startRequests = new ArrayList<>();
  private List<Request> endRequests = new ArrayList<>();
  private Map<String, RequestPars> endDeletes = new HashMap<>();

  private List<Testsuite> suites = new ArrayList<>();
  private Testsuite currentSuite;

  public String grabbedLocation;
  public Set<String> previouslyFound = new TreeSet<>();
  public Map<String, String> uidmaps = new HashMap<>();

  public Testfile(final Manager manager,
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
      manager.testFile(testPath.toString(),
                       format("Missing features: %s",
                                           missingFeatures()),
                       RESULT_IGNORED);
      return TestResult.ignored();
    }

    if (excludedFeatures().size() != 0) {
      manager.testFile(testPath.toString(),
                       format("Excluded features: %s",
                              excludedFeatures()),
                       RESULT_IGNORED);
      return TestResult.ignored();
    }

    // Always need a new set of UIDs for the entire test file
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
      var doReqres = doRequests("Start Requests...", startRequests,
                          true,
                          format("%s | %s", name,
                                        "START_REQUESTS"));

      if (!doReqres) {
        manager.testFile(testPath.toString(),
                         "Start items failed - tests will not be run.",
                         Manager.RESULT_ERROR);
        res = TestResult.failed();
      } else {
        res = runSuites(name);
      }
      doEnddelete("Deleting Requests...", format("%s | %s", name,
                  "END_DELETE"));
      doRequests("End Requests...", endRequests, false,
                 format("%s | %s", name, "END_REQUESTS"));
      return res;
    } catch (final Throwable t) {
      manager.testFile(testPath.toString(),
                       format("FATAL ERROR: %s", t.getMessage()),
                       Manager.RESULT_ERROR);
      manager.error(t);

      return TestResult.failed();
    } finally {
      if (httpTrace) {
        httpTraceOff();
      }
    }
  }

  public TestResult runSuites(final String label) {
    var res = new TestResult();

    var testfile = manager.testFile(testPath.toString(),
                                    description, null);
    for (var suite : suites) {
      currentSuite = suite;
      res.add(suite.run(testfile, label));
    }

    return res;
  }

  private boolean doRequests(final String description,
                             final List<Request> requests,
                             final boolean forceverify,
                             final String label) {
    /* This method is only used for start and end requests
     */
    if (Util.isEmpty(requests)) {
      return true;
    }

    var result = true;

    manager.trace("Start: " + description);

    var reqCount = 1;
    StringBuilder resulttxt = new StringBuilder();

    for (var req: requests) {
      var resreq = req.run(false,
                           false, // doverify,
                           forceverify,
                           null, // stats
                           null, // etags
                           format("%s | #%s", label, reqCount),
                           1);  // count
      if (resreq.message != null) {
        resulttxt.append(resreq.message);
      }

      if (!resreq.ok &&
         (!req.method.equals("DELETE") ||
                (resreq.status != HttpServletResponse.SC_NOT_FOUND))) {
        resulttxt.append(format(
                "\nFailure during multiple requests " +
                        "#%d out of %d, request=%s",
                reqCount, requests.size(),
                req));
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
      manager.logit(resulttxt.toString());
    }

    manager.trace(format("%s60%s5",
                         "End: " + description,
                         s));
    if (resulttxt.length() > 0) {
      manager.trace(resulttxt.toString());
    }
    return result;
  }

  private String getOneHref(final Element node) {
    var href = childrenMatching(node, WebdavTags.href);

    if (href.size() != 1) {
      throwException("           Wrong number of DAV:href elements\n");
    }

    return content(href.get(0));
  }

  public void addEndDelete(final String uri,
                           final Request req) {
    if (endDeletes.containsKey(uri)) {
      return;
    }

    endDeletes.put(uri, new RequestPars(uri, req));
  }

  public void doEnddelete(final String description,
                          final String label) {
    if (endDeletes.isEmpty()) {
      return;
    }
    manager.trace("Start: " + description);
    for (var delReq: endDeletes.values()) {
      var req = delReq.makeRequest("DELETE", manager);
      req.run(false, false, false, null, null, label, 0);
    }
    manager.trace(format("%s60%s", "End: " + description, "[DONE]"));
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