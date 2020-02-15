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

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;

import java.util.List;

import static org.bedework.davtester.Utils.fileToString;

/**
 * Abstract class that handles filename or data then calls methods to compare.
 */
public abstract class FileDataMatch extends Verifier {
  public abstract List<Integer> expectedStatus(final KeyVals args);

  public abstract void compare(final String ruri,
                               final List<Header> responseHeaders,
                               final int status,
                               final String respdata,
                               final KeyVals args,
                               final String filepath,
                               final List<String> filters,
                               final String data);

  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var filepath = args.getOnlyString("filepath");
    if (filepath != null) {
      filepath = manager.normResPath(filepath).toString();
    }

    var data = args.getOnlyString("data");
    var filters = args.getStrings("filter");

    if (!expectedStatus(args).contains(status)) {
      fmsg("        HTTP Status Code Wrong: %d", status);
      return result;
    }

    // look for response data
    if (StringUtils.isEmpty(respdata)) {
      append("        No response body");
      return result;
    }

    // read in all data from specified file or use provided data

    if (filepath != null) {
      data = fileToString(filepath);

      if (data == null) {
        append("        Could not read data file");
        return result;
      }
    } else if (data == null) {
      append("        No file/data to compare response to");
      return result;
    }

    data = manager.serverInfo.subs(data);
    data = manager.serverInfo.extrasubs(data);

    compare(ruri, responseHeaders, status, respdata, args,
            filepath, filters, data);

    return result;
  }
}
