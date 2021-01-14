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
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.xml.XmlUtil;
import org.bedework.util.xml.tagdefs.WebdavTags;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.Utils.intersection;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.getQName;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Verifier that checks a propfind response to make sure that the
 * specified ACL privileges are available for the currently
 * authenticated user.
 */
public class AclItems extends Verifier {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {

    var granted = args.getStrings("granted");
    var denied = args.getStrings("denied");

    // Process the multistatus response, extracting all
    // current-user-privilege-set elements
    // and check to see that each required privilege is present,
    // or that denied ones are not.

    // Must have MULTISTATUS response code
    if (status != 207) {
      fmsg("           HTTP Status for Request: %d\n",
           status);
      return result;
    }

    if (!parseXml(respdata)) {
      return result;
    }

    final MultiStatusResponse msr =
            getMultiStatusResponse(respdata);
    if (msr == null) {
      return result;
    }

    for (var response: msr.responses) {
      // Get href for this response
      var href = URLDecoder.decode(
              StringUtils.stripEnd(response.href, "/"),
              StandardCharsets.UTF_8);

      // Should be one OK propstat element
      if ((response.propstats.size() != 1) ||
              response.propstats.get(0).status != 200) {
        fmsg("           Expected single OK propstat element: %s",
             respdata);
        return result;
      }

      var propstat = response.propstats.get(0);

      // Expect one prop:
      if (propstat.props.size() != 1) {
        fmsg("           Expected one prop element: %s",
             respdata);
        return result;
      }

      var propCh = children(propstat.props.get(0));
      if ((propCh.size() != 1) ||
              !nodeMatches(propCh.get(0),
                           WebdavTags.currentUserPrivilegeSet)) {
        fmsg("           Expected single currentUserPrivilegeSet element: %s",
             respdata);
        return result;
      }

      var privset = children(propCh.get(0));

      // Get all privileges
      var grantedPrivs = new ArrayList<String>();

      for (var priv: privset) {
        if (!nodeMatches(priv,
                         WebdavTags.privilege)) {
          fmsg("           Expected privilege elements only: %s",
               respdata);
          return result;
        }

        // Should really only be on element inside privilege
        var gpriv = XmlUtil.getOnlyElement(priv);
        grantedPrivs.add(getQName(gpriv).toString());
      }

      // Now do set difference
      var grantedMissing = diff(granted, grantedPrivs);
      var deniedPresent = intersection(grantedPrivs, denied);

      if (grantedMissing.size() != 0) {
        fmsg("        Missing privileges not granted for %s:",
             href);
        for (var i: grantedMissing) {
          append(" " + i, false);
        }
        nl();
      }
      if (deniedPresent.size() != 0) {
        fmsg("        Available privileges that should be denied for %s:",
             href);
        for (var i: deniedPresent) {
          append(" " + i, false);
        }
        nl();
      }
    }

    return result;
  }
}
