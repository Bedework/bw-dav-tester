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
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 A base class for an observer that gets passed results of tests.

 Supported messages:

 trace - tracing tool activity
 begin - beginning
 load - loading a test
 start - starting tests
 testFile - add a test file
 testSuite - add a test suite
 testResult - add a test result
 protocol - protocol log
 finish - tests completed
 */
public abstract class BaseResultsObserver implements Logged {
  private Manager manager;
  private final Map<String, BaseResultsObserver> calls =
          new HashMap<>();

  BaseResultsObserver() {
  }

  public abstract void process(final String message, final Properties args);

  /**
   * Called immediately after creation. Shoudlbe overridden.
   * @param manager
   */
  public void init(final Manager manager) {
    this.manager = manager;
  }

  Manager manager() {
    return manager;
  }

  void addCall(final String msg, final BaseResultsObserver bro) {
    calls.put(msg, bro);
  }

  void message(final String message, final Properties args) {

    var callit = calls.get(message);

    if (callit != null) {
      callit.process(message, args);
    }
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}