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
import org.bedework.util.misc.Util;
import org.bedework.util.xml.diff.NodeDiff;

import org.apache.http.Header;

import java.util.Arrays;
import java.util.List;

/**
 * Verifier that checks the response body for an exact match to data
 * in a file.
 */
public class XmlDataMatch extends FileDataMatch {
  @Override
  public List<Integer> expectedStatus(final KeyVals args) {
    return Arrays.asList(200, 207);
  }

  @Override
  public void compare(final String ruri,
                      final List<Header> responseHeaders,
                      final int status,
                      final String respdata,
                      final KeyVals args,
                      final String filepath,
                      final String data) {
    final var filters = args.getStrings("filter");

    final var nrespdata = normalizeXMLData(respdata, filters);
    final var ndata = normalizeXMLData(data, filters);

    final var diffs = NodeDiff.diff(nrespdata.getDocumentElement(),
                                    ndata.getDocumentElement());

    if (!Util.isEmpty(diffs)) {
      errorDiff(diffs);
    }
  }
}