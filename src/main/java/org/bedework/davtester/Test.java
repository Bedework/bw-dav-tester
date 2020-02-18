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
import org.bedework.util.misc.ToString;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.bedework.davtester.Manager.RESULT_FAILED;
import static org.bedework.davtester.Manager.RESULT_IGNORED;
import static org.bedework.davtester.Manager.RESULT_OK;
import static org.bedework.davtester.XmlUtils.attr;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getIntAttributeValue;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 *
 A single test which can be comprised of multiple requests. The test can
 be run more than once, and timing information gathered and averaged across
 all runs.
 */
class Test extends DavTesterBase {
  boolean details;
  boolean stats;
  boolean ignore;
  public boolean skipSuiteOnFail;
  int count = 1;

  List<Request> requests = new ArrayList<>();

  public Test(final Manager manager) {
    super(manager);
  }

  @Override
  public String getKind() {
    return "TEST";
  }

  public void parseXML(final Element node) {
    name = attr(node, XmlDefs.ATTR_NAME);
    details = getYesNoAttributeValue(node, XmlDefs.ATTR_DETAILS);
    count = getIntAttributeValue(node, XmlDefs.ATTR_COUNT, 1);
    stats = getYesNoAttributeValue(node, XmlDefs.ATTR_STATS);
    ignore = getYesNoAttributeValue(node, XmlDefs.ATTR_IGNORE);
    only = getYesNoAttributeValue(node, XmlDefs.ATTR_ONLY);
    skipSuiteOnFail = getYesNoAttributeValue(node, XmlDefs.ATTR_SKIP_SUITE_ON_FAIL);
    httpTrace = getYesNoAttributeValue(node, XmlDefs.ATTR_HTTP_TRACE,
                                       false);

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
        parseFeatures(child, true);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
        parseFeatures(child, false);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_DESCRIPTION)) {
        description = content(child);
      }
    }

    // get request
    requests = Request.parseList(manager, node);
  }

  public TestResult run(final KeyVals testsuite,
                        final Map<String, String> etags,
                        final boolean only,
                        final String label) {
    if (ignore || (manager.currentTestfile.only && !only)) {
      manager.testResult(testsuite, name,
                         "      Deliberately ignored",
                         RESULT_IGNORED, null);
      return TestResult.ignored();
    }

    if (hasMissingFeatures()) {
      manager.testResult(testsuite, name,
                         format("      Missing features: %s",
                                missingFeatures()),
                         RESULT_IGNORED, null);
      return TestResult.ignored();
    }

    if (hasExcludedFeatures()) {
      manager.testResult(testsuite,
                         name,
                         format("      Excluded features: %s",
                                excludedFeatures()),
                         RESULT_IGNORED, null);
      return TestResult.ignored();
    }

    var result = true;
    String resulttxt = null;
    // POSTGRES postgresCount = postgresInit();
    var reqstats = new RequestStats();

    for (var ctr = 0; ctr < count; ctr++) {
      var failed = false;
      var reqCount = 1;
      for (var req: requests) {
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
              manager.testResult(testsuite, name,
                                 "      No iteration data - ignored",
                                 RESULT_IGNORED, null);
              return TestResult.ignored();
            }

            while (req.getNextData()) {
              var reqres = req.run(details, true, false,
                                   reqstats, etags,
                                   format("%s | #%s", label, reqCount),
                                   ctr + 1);
              if (!reqres.ok) {
                failed = true;
                break;
              }

              resulttxt = reqres.message;
            }
          } else {
            var reqres = req.run(details, true, false,
                                 reqstats, etags,
                                 format("%s | #%s", label, reqCount),
                                 ctr + 1);
            if (!reqres.ok) {
              failed = true;
            }

            resulttxt = reqres.message;
          }

          if (!failed || !req.waitForSuccess) {
            break;
          }

          reqCount++;
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

    if (stats) {
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
    if (result) {
      rcode = RESULT_OK;
    } else {
      rcode = RESULT_FAILED;
    }
    manager.testResult(testsuite, name, resulttxt, rcode,
                       addons);

    if (result) {
      return TestResult.ok();
    }

    return TestResult.failed();
  }

  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    //for (var req: requests) {
    //  req.dump();
    //}
  }
}
