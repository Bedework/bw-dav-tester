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

import org.bedework.util.misc.response.Response;

import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static org.bedework.davtester.Manager.RESULT_IGNORED;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Class to encapsulate a single test file. (Was Caldavtest)
 */
public class Testfile extends DavTesterBase {
  @Override
  public String getKind() {
    return "CALDAVTEST";
  }

  public Path testPath;

  public boolean only;

  private StartEndTest startRequests;
  private StartEndTest endRequests;

  public EndDeletes endDeletes;

  private List<Testsuite> suites = new ArrayList<>();
  private Testsuite currentSuite;

  public String grabbedLocation;
  public Set<String> previouslyFound = new TreeSet<>();

  public Testfile(final Manager manager,
                  final Path testPath,
                  final boolean ignoreRoot) {
    super(manager);
    this.testPath = testPath;
    this.name = testPath.getFileName().toString();

    endDeletes = new EndDeletes(manager);
  }

  public Response readFile() {
    var resp = new Response();

    doc = XmlUtils.parseXml(testPath.toString());

    if (doc == null) {
      return Response.error(resp,
                            format("No valid document for %s",
                                   testPath));
    }

    final Element rootEl = doc.getDocumentElement();

    if (nodeMatches(rootEl,
                    XmlDefs.ELEMENT_CALTEST)) {
      parseXML(rootEl);
      return Response.ok(resp);
    }

    // Try old root
    if (nodeMatches(rootEl,
                    XmlDefs.ELEMENT_CALDAVTEST)) {
      parseXML(rootEl);
      return Response.ok(resp);
    }

    return Response.error(resp,
                          format("Invalid root element %s",
                                 rootEl));
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
    manager.serverInfo.newUIDs();

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

      final boolean doReqres;

      if (startRequests != null) {
        doReqres = startRequests.run();
      } else {
        doReqres = true;
      }

      final TestResult res;

      if (!doReqres) {
        manager.testFile(testPath.toString(),
                         "Start items failed - tests will not be run.",
                         Manager.RESULT_ERROR);
        res = TestResult.error();
      } else {
        res = runSuites(name);
      }

      endDeletes.run();

      if (endRequests != null) {
        endRequests.run();
      }

      return res;
    } catch (final Throwable t) {
      manager.testFile(testPath.toString(),
                       format("FATAL ERROR: %s", t.getMessage()),
                       Manager.RESULT_ERROR);
      manager.error(t);

      return TestResult.error();
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
    for (var suite: suites) {
      currentSuite = suite;
      res.add(suite.run(testfile, label));
    }

    return res;
  }

  /*
  private String getOneHref(final Element node) {
    var href = childrenMatching(node, WebdavTags.href);

    if (href.size() != 1) {
      throwException("           Wrong number of DAV:href elements\n");
    }

    return content(href.get(0));
  }
  */

  @Override
  public boolean xmlNode(final Element node) {
    if (nodeMatches(node,
                    XmlDefs.ELEMENT_DEFAULTFILTERSAPPLIED)) {
      parseDefaultFiltersApplied(node);
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_START)) {
      startRequests = new StartEndTest(manager, true);
      startRequests.parseXML(node);
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_END)) {
      endRequests = new StartEndTest(manager, false);
      endRequests.parseXML(node);
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_TESTSUITE)) {
      var suite = new Testsuite(manager);
      suite.parseXML(node);
      suites.add(suite);
      return true;
    }

    return super.xmlNode(node);
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