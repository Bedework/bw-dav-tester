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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.Utils.throwException;

/**
 * A results observer that prints results to standard output.
 */
public class Jsondump extends BaseResultsObserver {
  private List<String> currentProtocol = new ArrayList<>();
  private ObjectMapper om = new ObjectMapper();

  public Jsondump() {
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
        testResult(args);
    }
  }

  public void protocol(final KeyVals args) {
    var s = args.getOnlyString("protocol");
    if (s != null) {
      currentProtocol.add(s);
    }
  }

  public void testResult(final KeyVals args) {
    args.put("time", System.currentTimeMillis());
    if (currentProtocol != null) {
      args.put("protocol", currentProtocol);
      currentProtocol = null;
    }
  }

  public void testSuite(final KeyVals args) {
    args.put("time", System.currentTimeMillis());
  }

  public void finish() {
    throwException("Unimplemented");
    try {
      manager().logit(om.writeValueAsString(manager().getResults()));
    } catch (JsonProcessingException e) {
      throwException(e);
    }
  }
}
