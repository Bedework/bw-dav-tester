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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.Properties;

/**
 * A results observer that prints results to standard output.
 */
public class Jsondump extends BaseResultsObserver {
  private String currentProtocol;
  private ObjectMapper om = new ObjectMapper();

  public Jsondump(final Manager manager) {
    super(manager);
  }

  @Override
  public void init(final Manager manager) {
    super.init(manager);

    addCall("finish", this);
    addCall("protocol", this);
    addCall("testSuite", this);
    addCall("testResult", this);
  }

  public void process(final String message, final KeyVals args) {
    switch (message) {
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

  public void protocol(final KeyVals args) {
    currentProtocol = args.getOnlyString("protocol");
  }

  public void testResult(final Properties args) {
    args.put("time", new Date());
    if (currentProtocol != null) {
      args.put("protocol", currentProtocol);
      currentProtocol = null;
    }
  }

  public void testSuite(final KeyVals args) {
    args.put("time", new Date());
  }

  public void finish() {
    manager().print(om.writeValueAsString(manager().getResults()));
  }
}
