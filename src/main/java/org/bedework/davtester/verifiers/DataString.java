/*
// Copyright (c) 2006-2016 Apple Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.bedework.davtester.verifiers;

import org.bedework.davtester.KeyVals;

import org.apache.http.HttpResponse;

import static java.lang.String.format;

/**
 * Verifier that checks the response body for an exact match to data in a file.
 */
public class DataString extends Verifier {
  @Override
  public VerifyResult verify(final String uri,
                             final HttpResponse response,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var equals = args.getStrings("equals");
    var contains = args.getStrings("contains");
    var notcontains = args.getStrings("notcontains");
    var unwrap = args.getOnlyBool("unwrap");
    var empty = args.getOnlyBool("empty");

    // Test empty
    if (empty) {
      if (respdata != null) {
        return new VerifyResult("        Response data has a body");
      }
      return new VerifyResult();
    }

    // look for response data
    if (respdata == null) {
      return new VerifyResult("        No response body");
    }

    String newrespdata;

    // Unwrap if required
    if (unwrap) {
      newrespdata = respdata.replace("\r\n ", "");
    } else {
      newrespdata = respdata;
    }

    // Check each contains and not-contains (AND operation)
    for (var item : equals) {
      item = manager.serverInfo.subs(item);
      if (!newrespdata.equals(item)) {
        return new VerifyResult(
                format("        Response data does not equal \"%s\"",
                       item));
      }
    }
    for (var item : contains) {
      item = manager.serverInfo.subs(item);
      if (!newrespdata.contains(item.replace("\n", "\r\n"))
              && (!newrespdata.contains(item))) {
        return new VerifyResult(
                format("        Response data does not contain \"%s\"",
                       item));
      }
    }
    for (var item : notcontains) {
      item = manager.serverInfo.subs(item);
      if (newrespdata.contains(item.replace("\n", "\r\n"))
              || newrespdata.contains(item)) {
        return new VerifyResult(
                format("        Response data incorrectly contains \"%s\"",
                       item));
      }
    }

    return new VerifyResult();
  }
}