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

import org.apache.http.Header;

import java.net.URI;
import java.util.List;

import static java.lang.String.format;

/**
 Verifier that checks the response status code for a specific value.
*/
public class StatusCode extends Verifier {
  @Override
  public VerifyResult verify(final URI uri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // If no status verification requested, then assume all 2xx codes are OK
    var teststatus = args.getStrings("status", "2xx");

    //if (debug()) {
    //  debug("teststatus: " + teststatus + " actual: " + status);
    //}

    for (var ts : teststatus) {
      final int test;
      if (ts.substring(1, 3).equals("xx")) {
        test = Integer.parseInt(ts.substring(0, 1));
      } else {
        test = Integer.parseInt(ts);
      }

      if (test < 100) {
        if ((status / 100) == test) {
          return new VerifyResult();
        }
      } else if (status == test) {
        return new VerifyResult();
      }
    }

    // Didn't match any
    var msg = format("        HTTP Status Code Wrong " +
                             "(expected %s): %d",
                     String.valueOf(teststatus),
                     status);
    //if (debug()) {
    //  debug(msg);
    //}

    return new VerifyResult(msg);
  }
}
