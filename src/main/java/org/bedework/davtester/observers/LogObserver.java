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

import org.bedework.davtester.Manager;

import java.util.Observer;
import java.util.Properties;

/**
 * A results observer that prints results to standard output.
 */
public class LogObserver extends BaseResultsObserver {
    RESULT_STRINGS = {
        Manager.RESULT_OK: "[OK]",
        Manager.RESULT_FAILED: "[FAILED]",
        Manager.RESULT_ERROR: "[ERROR]",
        Manager.RESULT_IGNORED: "[IGNORED]",
    }

  private String currentProtocol;
  private String loggedFailures;
  private String currentFile;
  private String currentSuite;

  final boolean printDetails = false;

  public LogObserver(final Manager manager) {
    super(manager);
  }

  public void process(final String message, final Properties args) {
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
    }
  }

  public void updateCalls () {
    super.updateCalls();
    addCall("start", this);
    addCall("testProgress", this);
    addCall("testFile", this);
    addCall("protocol", this);
    addCall("testSuite", this);
    addCall("testResult", this);
    addCall("finish", this);
  }

    public void start () {
      manager().logit("Starting tests")
      if manager().randomSeed != null:
      manager().logit("Randomizing order using seed '{rs}'"
                            .format(rs = manager().randomSeed))
    }
    
    public void testProgress(final Properties args) {
      manager().logit("")
      manager().logit("File {count} of {total}".format( * * result))
    }
    
    public void testFile(final Properties args) {
        currentFile = result["name"].replace("/", ".")[:-4]
        manager().logit("")
        _logResult(currentFile, result)
        if (result["result"] in (manager().RESULT_FAILED, manager().RESULT_ERROR) {
        failtxt = "{result}\n{details}\n\n{file}".format(
                result = RESULT_STRINGS[result["result"]],
                details = result["details"],
                file = currentFile,
                );
        loggedFailures.append(failtxt);
      }
    }
    
    public void testSuite(final Properties args) {
        currentSuite = result["name"]
        result_name = "  Suite: " + result["name"]
        _logResult(result_name, result)
        if (result["result"] in (manager().RESULT_FAILED, manager().RESULT_ERROR) {
        failtxt = "{result}\n{details}\n\n{file}/{suite}".format(
                result = RESULT_STRINGS[result["result"]],
                details = result["details"],
                file = currentFile,
                suite = currentSuite,
                );
        loggedFailures.append(failtxt);
      }
    }
    
    public void testResult(final Properties args) {
      result_name = "    Test: " + result["name"]
      _logResult(result_name, result)
      if ())result["result"]
      in(manager().RESULT_FAILED, manager().RESULT_ERROR)
      {
        failtxt = "{result}\n{details}\n\n{file}/{suite}/{test}"
                .format(
                        result = RESULT_STRINGS[result["result"]],
                        details = result["details"],
                        file = currentFile,
                        suite = currentSuite,
                        test = result["name"],
                        )
        loggedFailures.append(failtxt)
      }

        if (currentProtocol != null) {
          manager().logit("\n".join(currentProtocol))
          currentProtocol = []
        }
    }

    public void logResult(name, final Properties args) {
      if (result["result"] != null){
        result_value = RESULT_STRINGS[result["result"]]
        manager().logit("{name:<60}{value:>10}".format(name = name,
                                                     value = result_value))
      } else{
        manager().logit("{name:<60}".format(name = name))
      }
      if (print_details && result[ "details"]){
        manager().logit(result["details"])
      }
    }

    public void protocol(final Properties args) {
      currentProtocol.append(result)
    }
    
    public void finish () {
        manager().logit("")
        if manager().totals[Manager.RESULT_FAILED] + manager().totals[Manager.RESULT_ERROR] != 0:
            for (failed: loggedFailures) {
              manager().logit("=" * 70)
              manager().logit(failed)
            }
            
            overall = "FAILED (ok={o}, ignored={i}, failed={f}, errors={e})".format(
                o=manager().totals[Manager.RESULT_OK],
                i=manager().totals[Manager.RESULT_IGNORED],
                f=manager().totals[Manager.RESULT_FAILED],
                e=manager().totals[Manager.RESULT_ERROR],
            );
        } else

  {
    overall = "PASSED (ok={o}, ignored={i})".format(
            o = manager().totals[Manager.RESULT_OK],
            i = manager().totals[Manager.RESULT_IGNORED],
            );
    manager().logit("-" * 70);
    manager().logit("Ran {total} tests in {time:.3f}s\n".format(
            total = sum(manager().totals.values()),
            time = manager().timeDiff,
            ));

    manager().logit(overall)
  }
}
