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
import org.bedework.util.xml.tagdefs.CaldavTags;

import org.apache.http.Header;

import java.util.List;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Verifier that looks for specific FREEBUSY periods for a
 * particular ATTENDEE.
 */
public class PostFreeBusy extends FreeBusyBase {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    var checker = new FreeBusyBase.PeriodsChecker();

    if (!checker.getPeriods(status, args)) {
      return result;
    }

    // Get expected FREEBUSY info
    var users = args.getStrings("attendee");
    var events = args.getInt("events");

    if (!parseXml(respdata)) {
      return result;
    }

    // Extract each calendar-data object

    for (var resp: children(docRoot)) {
      if (!nodeMatches(resp, CaldavTags.response)) {
        fmsg("        Expect only responses as children. Found: %s",
             resp);
        return result;
      }

      for (var ch: children(resp)) {
        if (!nodeMatches(ch, CaldavTags.calendarData)) {
          continue;
        }

        if (!checker.parseData(content(ch))) {
          return result;
        }

        // Check for attendee value
        for (var attendee: checker.fb.getProperties("ATTENDEE")) {
          var cua = attendee.getValue();

          if (users.contains(cua)) {
            users.remove(cua);
            break;
          }
        }

        // Extract periods
        if (!checker.comparePeriods()) {
          return result;
        }

        // Check event count
        if ((events != null) &&
                (checker.calendar.getComponents("VEVENT").size() != events)) {
          append("Number of VEVENTs does not match");
          break;
        }
      }
    }

    if (!Util.isEmpty(users)) {
      append("           Could not find attendee/calendar data in XML response\n");
    }

    return result;
  }
}
