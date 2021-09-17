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

import java.util.List;

/**
 * Verifier that checks the response of a free-busy-query.
 */
public class FreeBusy extends FreeBusyBase {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    final var checker = new FreeBusyBase.PeriodsChecker();

    if (!checker.getPeriods(status, args)) {
      return result;
    }

    //var duration = args.getOnlyBool("duration");

    if (!checker.parseData(respdata)) {
      return result;
    }

    // Extract periods
    checker.comparePeriods();

    return result;
  }
}
