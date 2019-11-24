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
package org.bedework.davtester.verifiers;

import org.bedework.davtester.KeyVals;
import org.bedework.davtester.Manager;

import org.apache.http.HttpResponse;

/** Base class for verifiers
 *
 * User: mike Date: 11/20/19 Time: 22:55
 */
public abstract class Verifier {
  public static class VerifyResult {
    public boolean ok = true;
    public String text;

    // An ok result
    public VerifyResult() {
    }

    // A not ok
    public VerifyResult(String text) {
      this.text = text;
    }

    void append(final String val) {
      if (text != null) {
        text += "\n";
      }

      text += val;
    }
  }

  protected Manager manager;

  public void init(final Manager manager) {
    this.manager = manager;
  }

  public boolean featureSupported(final String feature) {
    return manager.featureSupported(feature);
  }

  public abstract VerifyResult verify(final String uri,
                                      final HttpResponse response,
                                      final String respdata,
                                      final KeyVals args);
}
