/*
# Copyright (c) 2014-2016 Apple Inc. All rights reserved.
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
package org.bedework.davtester.observers;

import org.bedework.davtester.KeyVals;
import org.bedework.davtester.Manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * A results observer that prints results to standard output.
 */
public class Log extends BaseResultsObserver {
  final static Map<Integer, String> RESULT_STRINGS = new HashMap<>();
  static {
    RESULT_STRINGS.put(Manager.RESULT_OK, "[OK]");
    RESULT_STRINGS.put(Manager.RESULT_FAILED, "[FAILED]");
    RESULT_STRINGS.put(Manager.RESULT_ERROR, "[ERROR]");
    RESULT_STRINGS.put(Manager.RESULT_IGNORED, "[IGNORED]");
  }

  private List<String> currentProtocol = new ArrayList<>();
  private List<String> loggedFailures = new ArrayList<>();
  private String currentFile;
  private String currentSuite;

  final boolean printDetails = false;

  public Log() {
  }

  @Override
  public void init(final Manager manager) {
    super.init(manager);

    addCall("start", this);
    addCall("testProgress", this);
    addCall("testFile", this);
    addCall("protocol", this);
    addCall("testSuite", this);
    addCall("testResult", this);
    addCall("finish", this);
  }

  @Override
  public void process(final String message, final KeyVals args) {
    switch (message) {
      case "start":
        start();
        break;
      case "testProgress":
        testProgress(args);
        break;
      case "testFile":
        testFile(args);
        break;
      case "finish":
        finish();
        break;
      case "protocol":
        protocol(args);
        break;
      case "testSuite":
        testSuite(args);
        break;
      case "testResult":
        testResult(args);
    }
  }

  public void start () {
    manager().logit("Starting tests");
    /*
    if manager().randomSeed != null:
    manager().logit("Randomizing order using seed '{rs}'"
                            .format(rs = manager().randomSeed))
     */
  }
    
  public void testProgress(final KeyVals args) {
    manager().logit("");
    manager().logit(format("File %s of %s", args.getOnlyInt("count"),
                           args.getOnlyInt("total")));
  }
    
  public void testFile(final KeyVals args) {
    currentFile = args.getOnlyString("name").replace("/", ".");
    manager().logit("");
    logResult(currentFile, args);

    if (args.containsKey("result")) {
      var res = args.getOnlyInt("result");
      if ((res == Manager.RESULT_FAILED) ||
              (res == Manager.RESULT_ERROR)) {
        var failtxt = format("%s\n%s\n\n%s",
                             RESULT_STRINGS.get(res),
                             args.getOnlyString("details"),
                             currentFile);
        loggedFailures.add(failtxt);
      }
    }
  }
    
  public void testSuite(final KeyVals args) {
    currentSuite = args.getOnlyString("name");
    var resultName = "  Suite: " + args.getOnlyString("name");
    logResult(resultName, args);

    if (args.containsKey("result")) {
      var res = args.getOnlyInt("result");
      if ((res == Manager.RESULT_FAILED) ||
              (res == Manager.RESULT_ERROR)) {
        var failtxt = format("%s\n%s\n\n%s/%s",
                             RESULT_STRINGS.get(res),
                             args.getOnlyString("details"),
                             currentFile,
                             currentSuite);
        loggedFailures.add(failtxt);
      }
    }
  }
    
  public void testResult(final KeyVals args) {
    var result_name = "    Test: " + args.getOnlyString("name");
    logResult(result_name, args);

    if (args.containsKey("result")) {
      var res = args.getOnlyInt("result");
      if ((res == Manager.RESULT_FAILED) ||
              (res == Manager.RESULT_ERROR)) {
        var failtxt = format("%s\n%s\n\n%s/%s/%s",
                             RESULT_STRINGS.get(res),
                             args.getOnlyString("details"),
                             currentFile,
                             currentSuite,
                             args.getOnlyString("name"));
        loggedFailures.add(failtxt);
      }
    }

    if (currentProtocol != null) {
      manager().logit("\n");
      for (var cp: currentProtocol) {
        manager().logit(cp);
      }
      currentProtocol.clear();
    }
  }

  public void logResult(final String name, final KeyVals args) {
    if (args.containsKey("result")) {
      var res = args.getOnlyInt("result");
      var resultValue = RESULT_STRINGS.get(res);
      manager().logit(format("%60s%10s", name, resultValue));
    } else {
      manager().logit(format("%s", name));
    }

    if (printDetails && (args.getOnlyString("details") != null)) {
      manager().logit(args.getOnlyString("details"));
    }
  }

  public void protocol(final KeyVals args) {
    var s = args.getOnlyString("protocol");
    if (s != null) {
      currentProtocol.add(s);
    }
  }
    
  public void finish () {
    manager().logit("");
    String overall;

    if (manager().totals[Manager.RESULT_FAILED] +
            manager().totals[Manager.RESULT_ERROR] != 0) {
      for (var failed: loggedFailures) {
        manager().logit("-".repeat(70));
        manager().logit(failed);
      }
            
      overall = format("FAILED (ok=%s, ignored=%s, failed=%s, errors=%s)",
                       manager().totals[Manager.RESULT_OK],
                       manager().totals[Manager.RESULT_IGNORED],
                       manager().totals[Manager.RESULT_FAILED],
                       manager().totals[Manager.RESULT_ERROR]);
    } else {
      overall = format("PASSED (ok=%s, ignored=%s)",
                       manager().totals[Manager.RESULT_OK],
                       manager().totals[Manager.RESULT_IGNORED]);
      manager().logit("-".repeat(70));
      manager().logit(format("Ran %s tests in %.3f\n",
                             IntStream.of(manager().totals).sum(),
                             (float)manager().totalTime / 1000));
    }

    manager().logit(overall);
  }
}
