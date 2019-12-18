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

/**
 * A results observer that prints results to standard output.
 */
public class Trace extends BaseResultsObserver {

  public Trace() {
  }

  @Override
  public void process(final String message, final KeyVals args) {
    if ("trace".equals(message)) {
      trace(args);
    }
  }

  public void trace(final KeyVals args) {
    if (!args.containsKey("message")) {
      return;
    }

    var message = args.getOnlyString("message");
    manager.logit(message);
  }
}

