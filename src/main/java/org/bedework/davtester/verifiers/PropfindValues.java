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
import org.w3c.dom.Element;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getQName;
import static org.bedework.util.xml.XmlUtil.hasContent;
import static org.bedework.util.xml.XmlUtil.setElementContent;

/**
Verifier that checks a propfind response for regex matches to property values.
 */
public class PropfindValues extends Verifier {

  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // If no status verification requested, then assume all 2xx codes are OK
    var ignores = args.getStrings("ignore");
    var only = args.getStrings("only");

    // Get property arguments and split on $ delimited for name, value tuples
    var testprops = args.getStrings("props");
    var propsMatch = new ArrayList<NameVal>();
    for (var p : testprops) {
      if (p.contains("$")) {
        var split = p.split("\\$");
        if (p.indexOf("$") != p.length() - 1) {
          propsMatch.add(new NameVal(split[0],
                                     normalizeXML(split[1]),
                                     true));
        } else {
          propsMatch.add(new NameVal(split[0], null, true));
        }
      } else if (p.contains("!")) {
        var split = p.split("!");
        if (p.indexOf("!") != p.length() - 1) {
          propsMatch.add(new NameVal(split[0],
                                     normalizeXML(split[1]),
                                     false));
        } else {
          propsMatch.add(new NameVal(split[0],
                                     null,
                                     false));
        }
      }
    }

    // Process the multistatus response, extracting all hrefs
    // and comparing with the properties defined for this test. Report any
    // mismatches.

    // Must have MULTISTATUS response code
    if (status != 207) {
      fmsg("           HTTP Status for Request: %d\n",
           status);
      return result;
    }

    final MultiStatusResponse msr =
            getMultiStatusResponse(respdata);
    if (msr == null) {
      return result;
    }

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

      // Get all property status
      var okStatusProps = new HashMap<String, String>();

      String value = null;
      for (var propstat : response.propstats) {
        // Determine status for this propstat
        boolean isOkStatus = propstat.status / 100 == 2;

        if (propstat.props.size() != 1) {
          append("           Wrong number of DAV:prop elements\n");
          return result;
        }

        var prop = propstat.props.get(0);
        var fqname = getQName(prop).toString();

        if (XmlUtil.hasChildren(prop)) {
          var sb = new StringBuilder();
          removeWhitespace(prop);

          for (var p : children(prop)) {
            sb.append(p.toString());
          }

          value = sb.toString();
        } else if (XmlUtil.hasContent(prop)) {
          value = content(prop);
        } else {
          value = null;
        }

        if (isOkStatus) {
          okStatusProps.put(fqname, value);
        }
      }

      // Look at each property we want to test and see if present
      for (var nv : propsMatch) {
        if (!okStatusProps.containsKey(nv.name)) {
          fmsg("        Items not returned in report (OK) for %s: %s\n",
               href, nv.name);
          continue;
        }

        var matched = match(value, okStatusProps.get(nv.name));
        if (nv.match && !matched) {
          fmsg("        Items not matching for %s: %s %s\n",
               href, nv.name, okStatusProps.get(nv.name));
          continue;
        }

        if (!nv.match && matched) {
          fmsg("        Items incorrectly match for %s: %s %s\n",
               href, nv.name, okStatusProps.get(nv.name));
        }
      }
    }

    return result;
  }

  private boolean match(final String re,
                        final String val) {
    Pattern p = Pattern.compile(re);

    return p.matcher(val).find();
  }

  private void removeWhitespace(final Element node) {
    for (var child : children(node)) {
      if (hasContent(child)) {
        setElementContent(child,
                          content(child).strip());
      }
      removeWhitespace(child);
    }
  }
}

