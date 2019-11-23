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

import static java.lang.String.format;

/**
 Verifier that checks the response status code for a specific value.
*/
public class StatusCode extends Verifier {
  @Override
  public VerifyResult verify(final Manager manager,
                             final String uri,
                             final HttpResponse response,
                             final String respdata,
                             final KeyVals args) {
    // If no status verification requested, then assume all 2xx codes are OK
    var teststatus = args.getStrings("status", "2xx");
    int respStatus = -1;

    int test = 0;
    for (var ts : teststatus) {
      if (ts.substring(1, 3).equals("xx")) {
        test = Integer.parseInt(ts.substring(0, 1));
      } else {
        test = Integer.parseInt(ts);
      }

      respStatus = response.getStatusLine().getStatusCode();
      if (test < 100) {
        if ((respStatus / 100) == test) {
          return new VerifyResult();
        }
      } else if (respStatus == test) {
        return new VerifyResult();
      }
    }

    // Didn't match any
    return new VerifyResult("        HTTP Status Code Wrong " +
                                    format("(expected %s): %d",
                                           String.valueOf(teststatus),
                                           respStatus));
  }
}
