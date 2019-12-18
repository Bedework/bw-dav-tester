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
import org.bedework.util.misc.Util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.Utils.intersection;

/**
 * Verifier that checks a multistatus response to make sure that the specified hrefs
 * are returned with appropriate status codes.
 */
public class MultistatusItems extends Verifier {
  @Override
  protected VerifyResult verify(final URI uri,
                                final List<Header> responseHeaders,
                                final int status,
                                final String respdata,
                                final KeyVals args) {
    // If no hrefs requested, then assume null should come back
    var okhrefs = args.getStrings("okhrefs");
    var nohrefs = args.getStrings("nohrefs");
    var badhrefs = args.getStrings("badhrefs");

    final Map<Integer, Set<String>> statusHrefs = new HashMap<>();
    for (var arg: args.keySet()){
      try {
        int code = Integer.parseInt(arg);

        statusHrefs.computeIfAbsent(code, s -> new TreeSet<>()).add(arg);
      } catch (final Throwable t) {
        continue;
      }
    }

    var prefix = args.getOnlyString("prefix");
    if (!StringUtils.isEmpty(prefix)) {
      if (prefix.startsWith("-")) {
        prefix = "";
      }
    } else {
      prefix = uri.toString();
    }

    okhrefs = processHrefSubstitutions(okhrefs, prefix);
    nohrefs = processHrefSubstitutions(nohrefs, prefix);
    badhrefs = processHrefSubstitutions(badhrefs, prefix);

    var ignoremissing = args.getOnlyBool("ignoremissing");

    var doOKBad = args.containsKey("okhrefs") ||
            args.containsKey("nohrefs") ||
            args.containsKey("badhrefs");

    // Process the multistatus response, extracting all hrefs
    // and comparing with the set defined for this test. Report any
    // mismatches.

    // Must have MULTISTATUS response code
    if (status != 207) {
      fmsg("           HTTP Status for Request: %d\n",
           status);
      return result;
    }

    if (!parseXml(respdata)) {
      return result;
    }

    final var okStatusHrefs = new ArrayList<String>();
    final var badStatusHrefs = new ArrayList<String>();
    final var statusCodeHrefs = new HashMap<Integer, Set<String>>();

    final MultiStatusResponse msr =
            getMultiStatusResponse(respdata);
    if (msr == null) {
      return result;
    }

    var ok = false;
    int code;

    for (var response : msr.responses) {
      // Get href for this response
      var href = URLDecoder.decode(StringUtils.stripEnd(response.href, "/"),
                                   StandardCharsets.UTF_8);

      // Verify status
      ok = response.propstats.size() > 0;
      code = 0;

      if (ok) {
        okStatusHrefs.add(href);
      } else {
        badStatusHrefs.add(href);
      }
      statusCodeHrefs.computeIfAbsent(code, s -> new TreeSet<>()).add(href);
    }

    var okResultSet = new TreeSet<>(okStatusHrefs);

    // Check for count
    var count = args.getInt("count");
    if (count != null) {
      if (okResultSet.size() != count + 1) {
        fmsg("        %d items returned, but %d items expected",
             okResultSet.size() - 1, count);
      }
      return result;
    }

    // Check for total count
    if (args.containsKey("totalcount")) {
      var totalcount = args.getInts("totalcount");
      if (totalcount.size() > 0) {
        var tot = totalcount.get(0);
        // Add the 2nd value to the 1st if it exists
        if (totalcount.size() == 2) {
          tot += totalcount.get(1);
        }
        if (okResultSet.size() != tot) {
          fmsg("        %d items returned, but %d items expected",
               okResultSet.size(), tot);
        }
      }
      return result;
    }

    var badResultSet = new TreeSet<>(badStatusHrefs);

    // Check for response count
    if (args.containsKey("responsecount")) {
      var responsecount = args.getInt("responsecount");
      var responses = okResultSet.size() + badResultSet.size();
      if (responses != responsecount) {
        fmsg("        %d responses returned, but %d responses expected",
             responses, responsecount);
      }

      return result;
    }

    var okTestSet = new TreeSet<>(okhrefs);
    var noTestSet = new TreeSet<>(nohrefs);
    var badTestSet = new TreeSet<>(badhrefs);

    if (doOKBad) {
      // Now do set difference
      var okMissing = diff(okTestSet, okResultSet);
      var noExtras = intersection(okResultSet, noTestSet);
      var badMissing = diff(badTestSet, badResultSet);

      final List<String> okExtras;
      final List<String> badExtras;
      if (!ignoremissing){
        okExtras = diff(okResultSet, okTestSet);
        badExtras = diff(badResultSet, badTestSet);
      } else {
        okExtras = Collections.EMPTY_LIST;
        badExtras = Collections.EMPTY_LIST;
      }

      if (okMissing.size() != 0) {
        badHrefs("        %d Items not returned in report (OK):",
                 okMissing);
      }

      if (okExtras.size() != 0) {
        badHrefs("        %d Unexpected items returned in report (OK):",
                 okExtras);
      }

      if (noExtras.size() != 0) {
        badHrefs("        %d Unwanted items returned in report (OK):",
                 noExtras);
      }

      if (badMissing.size() != 0) {
        badHrefs("        %d Items not returned in report (BAD):",
                 badMissing);
      }

      if (badExtras.size() != 0) {
        badHrefs("        %d Unexpected items returned in report (BAD):",
                 badExtras);
      }
    }

    if (!doOKBad) {
      var l = diff(statusHrefs.keySet(),
              statusCodeHrefs.keySet());
      if (!Util.isEmpty(l)) {
        badHrefs("        %d Status Codes not returned in report:", l);
      }

      l = diff(statusCodeHrefs.keySet(),
               statusHrefs.keySet());
      if (!Util.isEmpty(l)) {
        badHrefs("        %d Unexpected Status Codes returned in report:",
                 l);
      }

      var allKeys = new TreeSet<>(statusHrefs.keySet());
      allKeys.addAll(statusCodeHrefs.keySet());
      for (var key: allKeys) {
        var kl = diff(statusHrefs.get(key), statusCodeHrefs.get(key));
        if (!Util.isEmpty(kl)) {
          badHrefs("        %d Items not returned in report for %d:",
                   kl, key);
        }

        kl = diff(statusCodeHrefs.get(key), statusHrefs.get(key));
        if (!Util.isEmpty(kl)) {
          badHrefs("        %d Unexpected items returned in report for %d:",
                   kl, key);
        }
      }
    }

    return result;
  }

  /**
   Process the list of hrefs by prepending the supplied prefix. If the href is a
   list of hrefs, then prefix each item in the list and expand into the results. The
   empty string is represented by a single "-" in an href list.

   @param hrefs: list of URIs to process
   @param prefix: prefix to apply to each URI

   @return resulting list of URIs
   */
  private List<String> processHrefSubstitutions(final List<String> hrefs,
                                                final String prefix) {
    var results = new ArrayList<String>();

    for (var href : hrefs) {
      results.add(Util.buildPath(false, prefix, href));
    }

    return results;
  }
}
