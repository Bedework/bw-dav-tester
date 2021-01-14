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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;

/**
 * Verifier that checks the response headers for a specific value.
 */
public class Header extends Verifier {
  private enum Presence {
    absent,
    single,
    multiple
  }

  private static class TestInfo {
    final String hdrName;
    final String hdrVal;
    final Presence present;
    final boolean matchValue;

    // From hdrVal
    final Pattern pattern;

    TestInfo(final String hdrName,
             final String hdrVal,
             final Presence present,
             final boolean matchValue) {
      this.hdrName = hdrName;
      if (hdrVal != null) {
        this.hdrVal = hdrVal.replace(" ", "");
        pattern = Pattern.compile(this.hdrVal);
      } else {
        this.hdrVal = null;
        pattern = null;
      }
      this.present = present;
      this.matchValue = matchValue;
    }
  }

  @Override
  public VerifyResult verify(final String ruri,
                             final List<org.apache.http.Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Split into header/value tuples
    var testheader = args.getStrings("header");
    var testInfo = new ArrayList<TestInfo>();
    Presence present;

    for (var p: testheader) {
      present = Presence.single;

      if (p.startsWith("!")) {
        // header must not be present
        p = p.substring(1);
        present = Presence.absent;
      } else if (p.startsWith("*")) {
        // header must be present 1 or more times
        p = p.substring(1);
        present = Presence.multiple;
      }

      if (p.contains("$")) {
        var split = p.split("\\$");
        testInfo.add(new TestInfo(split[0],
                                  split[1],
                                  present,
                                  true));
      } else if (p.contains("!")) {
        var split = p.split("!");
        testInfo.add(new TestInfo(split[0],
                                  split[1],
                                  present,
                                  false));
      } else {
        testInfo.add(new TestInfo(p,
                                  null,
                                  present,
                                  true));
      }
    }

    for (var ti: testInfo) {
      var hdrs = getHeaders(ti.hdrName, responseHeaders);
      if (Util.isEmpty(hdrs)) {
        if (ti.present == Presence.absent) {
          continue;
        }

        fmsg("        Missing Response Header: %s",
             ti.hdrName);
        continue;
      }

      if (!Util.isEmpty(hdrs) && (ti.present == Presence.absent)) {
        fmsg("        Response Header was present one or more times: %s",
             ti.hdrName);
        continue;
      }

      if ((hdrs.size() != 1) && (ti.present == Presence.single)) {
        fmsg("        Multiple Response Headers: %s",
             ti.hdrName);
        continue;
      }

      if (ti.pattern != null) {
        var matched = false;
        for (var hdr: hdrs) {
          var hval = hdr.getValue().replace(" ", "");
          if (ti.pattern.matcher(hval).lookingAt()) {
            matched = true;
            break;
          }
        }

        if (ti.matchValue != matched) {
          fmsg("        Wrong Response Header Value: %s: %s",
               ti.hdrName, valueOf(hdrs));
        }
      }
    }

    return result;
  }

  private List<org.apache.http.Header> getHeaders(final String name,
                                                  final List<org.apache.http.Header> respHeaders) {
    var res = new ArrayList<org.apache.http.Header>();

    for (final var h: respHeaders) {
      if (h.getName().equals(name)) {
        res.add(h);
      }
    }

    return res;
  }
}
