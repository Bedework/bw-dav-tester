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
 * A results observer that prints when a test file is loaded.
 */
public class Loadfiles extends BaseResultsObserver {
  public Loadfiles() {
  }

  /**
   * @param message "load"
   * @param args    3 args: "name": file being loaded, or null for
   *                last file "current": current int number of files
   *                loaded "total": int total number of files to load
   */
  public void process(final String message, final KeyVals args) {
    var name = args.getOnlyString("name");
    if (name == null) {
      manager.logit("Loading files complete.\n");
      return;
    }

    var current = args.getOnlyInt("current");
    var total = args.getOnlyInt("total");
    manager.logit(
            String.format("Loading %d of %d: %s", current, total,
                          name));
  }
}
