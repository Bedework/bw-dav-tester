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
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.apache.http.Header;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Verifier that checks the response for a pre/post-condition
 * <DAV:error> result.
 */
public class Prepostcondition extends Verifier {
  private final static QName twistedDescription =
          new QName("http://twistedmatrix.com/xml_namespace/dav/",
                    "error-description");

  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // If no status verification requested, then
    // assume all 2xx codes are OK
    var teststatus = args.getStrings("error");
    var statusCode = args.getStrings("status",
                                     "403", "409", "507");
    var ignoreextras = args.getOnlyBool("ignoreextras");

    // status code could be anything, but typically 403, 409 or 507
    if (!statusCode.contains(String.valueOf(status))) {
      fmsg("        HTTP Status Code Wrong: %d", status);
      return result;
    }

    // look for pre-condition data
    if (respdata == null) {
      append("        No pre/post condition response body");
      return result;
    }

    if (!parseXml(respdata)) {
      return result;
    }

    if (nodeMatches(docRoot, WebdavTags.error)) {
      append("        Missing <DAV:error> element in response");
      return result;
    }

    // Make a set of expected pre/post condition elements

    var expected = new ArrayList<QName>();

    for (var ts: teststatus) {
      expected.add(QName.valueOf(ts));
    }

    var got = new ArrayList<QName>();
    for (var child: children(docRoot)) {
      if (nodeMatches(child, twistedDescription)) {
        got.add(new QName(child.getNamespaceURI(),
                          child.getLocalName()));
      }
    }

    var missing = diff(expected, got);
    var extras = diff(got, expected);

    if (missing.size() != 0) {
      fmsg("        Items not returned in error: element %s",
           String.valueOf(missing));
    }
    if ((extras.size() != 0) && !ignoreextras) {
      fmsg("        Unexpected items returned in error element: %s",
           String.valueOf(extras));
    }

    return result;
  }
}