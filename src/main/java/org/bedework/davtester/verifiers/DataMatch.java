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
import org.bedework.util.misc.Util;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.bedework.davtester.Utils.fileToString;
import static org.bedework.davtester.XmlUtils.docToString;

/**
 * Verifier that checks the response body for an exact match to data
 * in a file.
 */
public class DataMatch extends Verifier {
  @Override
  public VerifyResult verify(final URI uri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var filepath = args.getOnlyString("filepath");
    if (manager.dataDir != null) {
      filepath = Util.buildPath(false, manager.dataDir,
                                "/", filepath);
    }

    // status code must be 200, 207
    if ((status != 200) && (status != 207)) {
      fmsg("        HTTP Status Code Wrong: %d", status);
      return result;
    }

    // look for response data
    if (StringUtils.isEmpty(respdata)) {
      append("        No response body");
      return result;
    }

    // read in all data from specified file
    var data = fileToString(filepath);

    if (data == null) {
      append("        Could not read data file");
      return result;
    }

    data = manager.serverInfo.subs(data);

    if (data.equals(respdata)) {
      return result;
    }

    data = data.replace("\n", "\r\n");
    if (data.equals(respdata)) {
      return result;
    }

    // If we have an iCalendar file, then unwrap data and do compare
    if (filepath.endsWith(".ics")) {
      data = data.replace("\r\n ", "");
      var rd = respdata.replace("\r\n ", "");
      if (data.equals(rd)) {
        return result;
      }
    } else if (filepath.endsWith(".xml")) {
      var rd = docToString(XmlUtils.parseXml(respdata));

      var xdata = docToString(XmlUtils.parseXml(data));
      if (xdata == null) {
        fmsg("        Unable to parse xml data: %s", data);
        return result;
      }

      if (xdata.equals(rd)) {
        return result;
      }
    }

    var respLines = Arrays.asList(respdata.split("\n"));
    var dataLines = Arrays.asList(data.split("\n"));

    try {
      Patch<String> patch = DiffUtils.diff(dataLines, respLines);
      var errorDiff = new StringBuilder();

      for (AbstractDelta<String> delta : patch.getDeltas()) {
        errorDiff.append(delta.toString());
        errorDiff.append('\n');
      }

      fmsg("        Response data does not " +
                   "exactly match file data%s",
           errorDiff);
    } catch (final Throwable t) {
      fmsg("        Unable to diff data and response: %s",
           t.getMessage());
    }

    return result;
  }
}