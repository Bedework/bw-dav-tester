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

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;

import java.net.URI;
import java.util.List;

import static org.bedework.davtester.Utils.fileToString;

/**
 * Verifier that checks the response body for an exact match to data
 * in a file.
 */
public class XmlDataMatch extends Verifier {
  @Override
  public VerifyResult verify(final URI uri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var filepath = args.getOnlyString("filepath");
    var filters = args.getStrings("filter");

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
    data = manager.serverInfo.extrasubs(data);

    var nrespdata = normalizeXMLData(respdata, filters);
    data = normalizeXMLData(data, filters);

    if (!nrespdata.equals(data)) {
      errorDiff("        Response data does not " +
                        "exactly match file data%s",
                respdata, data);
    }

    return result;
  }
}