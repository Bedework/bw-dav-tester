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
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.XmlUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getQName;

/**
 * Verifier that checks a propfind response to make sure that the
 * specified properties are returned with appropriate status codes.
 *
 * Arguments for verifier:
 * <ul>
 * <li>ignore: list of hrefs to ignore</li>
 * <li>only: list of specific hrefs to test, others ignored</li>
 * <li>count: match the number of href elements</li>
 * <li>root-element: Qname for the root element of the response</li>
 * <li>status: test the overall response status code (default: 207)</li>
 * <li>okprops: list of properties with propstat status of 200 to test</li>
 * <li>badprops: list of properties with propstat status of 4xx to test</li>
 </ul>
 */
public class PropfindItems extends Verifier {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // If no status verification requested, then assume all 2xx codes are OK
    var ignores = args.getStrings("ignore");
    var only = args.getStrings("only");
    var count = args.getInt("count");

    var root = args.getOnlyString("root-element");
    if (root == null) {
      root = "{DAV:}multistatus";
    }

    // Expected status
    var expectedStatus = args.getInt("status");
    if (expectedStatus == null) {
      expectedStatus = 207;
    }

    // Get property arguments and split on $ delimited for name, value tuples
    var okprops = args.getStrings("okprops");
    var okPropsMatch = new ArrayList<NameVal>();
    var okpropsNomatch = new HashMap<String, String>();

    for (var p : okprops) {
      if (p.contains("$")) {
        var split = p.split("$");
        if (p.indexOf("$") != p.length() - 1) {
          okPropsMatch.add(new NameVal(split[0],
                                       normalizeXML(split[1])));
        } else {
          okPropsMatch.add(new NameVal(split[0], null));
        }
      } else if (p.contains("!")) {
        var split = p.split("!");
        if (p.indexOf("!") != p.length() - 1) {
          okpropsNomatch.put(split[0],
                             normalizeXML(split[1]));
        } else {
          okpropsNomatch.put(split[0],
                             null);
        }
      } else {
        okPropsMatch.add(new NameVal(p, null));
      }
    }

    var badpropsStr = args.getStrings("badprops");
    var badprops = new ArrayList<NameVal>();

    for (var bp: badpropsStr) {
      if (bp.contains("$")) {
        var split = bp.split("$");
        badprops.add(new NameVal(split[0],
                                 normalizeXML(split[1])));
      } else {
        badprops.add(new NameVal(bp, null));
      }
    }

    //ok_test_set = set(ok_props_match)
    //bad_test_set = set(badprops)

    // Process the multistatus response, extracting all hrefs
    // and comparing with the set defined for this test. Report any
    // mismatches.

    // Must have MULTISTATUS response code
    if (status != expectedStatus) {
      fmsg("           HTTP Status for Request: %d\n",
           status);
      return result;
    }

    final MultiStatusResponse msr;
    if (root.equals("{DAV:}multistatus")) {
      msr = getMultiStatusResponse(respdata);
    } else {
      msr = getExtMkcolResponse(respdata);
    }
    if (msr == null) {
      return result;
    }

    var ctr = 0;
    for (var response : msr.responses) {
      // Get href for this response
      var href = URLDecoder.decode(
              StringUtils.stripEnd(response.href, "/"),
              StandardCharsets.UTF_8);

      if (ignores.contains(href)) {
        continue;
      }

      if (!Util.isEmpty(only) && !only.contains(href)) {
        continue;
      }

      if (count != null) {
        ctr += 1;
        continue;
      }

      String value = null;

      // Get all property status
      var okStatusProps = new ArrayList<NameVal>();
      var badStatusProps = new ArrayList<NameVal>();

      for (var propstat : response.propstats) {
        // Determine status for this propstat
        boolean isOkStatus = propstat.status / 100 == 2;

        for (var prop: propstat.props) {
          var fqname = getQName(prop).toString();
          var testNv = new NameVal(fqname, null);

          if (XmlUtil.hasChildren(prop)) {
            // Copy sub-element data as text into one long string and strip leading/trailing space
            var val = new StringBuilder();
            for (var p: children(prop)) {
              val.append(p.toString().strip());
            }

            value = val.toString();

            if (isOkStatus) {
              if (okPropsMatch.contains(testNv)){
                value = null;
              }
            } else if (badprops.contains(testNv)){
              value = null;
            }
          } else if (XmlUtil.hasContent(prop)) {
            value = content(prop);
            if (isOkStatus) {
              if (okPropsMatch.contains(testNv)){
                value = null;
              }
            } else if (badprops.contains(testNv)){
              value = null;
            }
          } else {
            value = null;
          }

          if (isOkStatus) {
            okStatusProps.add(testNv);
          } else {
            badStatusProps.add(testNv);
          }
        }
      }

      // Now do set difference
      var okMissing = diff(okPropsMatch, okStatusProps);
      var okExtras = diff(okStatusProps, okPropsMatch);
      var badMissing = diff(badprops, badStatusProps);
      var badExtras = diff(badStatusProps, badprops);

      // Now remove extras that are in the no-match set
      for (var nv: okPropsMatch) {
        if (okpropsNomatch.containsKey(nv.name) &&
                (Util.cmpObjval(okpropsNomatch.get(nv.name), value) != 0)) {
          okExtras.remove(nv);
        }
      }

      if (okMissing.size() != 0) {
        badHrefs("        Items not returned in report (OK) for %s:",
                 okMissing, href);
        return result;
      }
      if (okExtras.size() != 0) {
        badHrefs("        Unexpected items returned in report (OK) for %s:",
                okExtras, href);
        return result;
      }
      if (badMissing.size() != 0) {
        badHrefs("        Items not returned in report (BAD) for %s:",
                 badMissing, href);
        return result;
      }
      if (badExtras.size() != 0) {
        badHrefs("        Unexpected items returned in report (BAD) for %s:",
                badExtras, href);
        return result;
      }
    }

    if ((count != null) && (count != ctr)) {
      fmsg("        Expected %d response items but got %d.",
           count, ctr);
    }

    return result;
  }
}
