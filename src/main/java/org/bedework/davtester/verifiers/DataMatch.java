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
import org.bedework.davtester.XmlUtils;

import org.apache.http.Header;

import java.util.Arrays;
import java.util.List;

import static org.bedework.davtester.XmlUtils.docToString;

/**
 * Verifier that checks the response body for an exact match to data
 * in a file.
 */
public class DataMatch extends FileDataMatch {
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
                      final List<String> filters,
                      final String data) {
    if (data.equals(respdata)) {
      return;
    }

    var ndata = data.replace("\n", "\r\n");
    if (ndata.equals(respdata)) {
      return;
    }

    // If we have iCalendar data, then unwrap data and do compare
    if ((filepath != null) && filepath.endsWith(".ics")) {
      ndata = ndata.replace("\r\n ", "");
      final var rd = respdata.replace("\r\n ", "");
      if (ndata.equals(rd)) {
        return;
      }
    } else if ((filepath != null) && filepath.endsWith(".xml")) {
      final var rd = docToString(XmlUtils.parseXml(respdata));

      final var xdata = docToString(XmlUtils.parseXml(data));
      if (xdata == null) {
        fmsg("        Unable to parse xml data: %s", data);
        return;
      }

      if (xdata.equals(rd)) {
        return;
      }
    }

    errorDiff("        Response data does not " +
                      "exactly match file data%s",
              respdata, data);
  }
}