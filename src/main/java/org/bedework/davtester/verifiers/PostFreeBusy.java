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
import org.bedework.davtester.ical.Icalendar;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.CaldavTags;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.FbType;
import org.apache.http.Header;

import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.Utils.symmetricDiff;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Verifier that looks for specific FREEBUSY periods for a
 * particular ATTENDEE.
 */
public class PostFreeBusy extends Verifier {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Must have status 200
    if (status != 200) {
      fmsg("        HTTP Status Code Wrong: %d", status);
      return result;
    }

    // Get expected FREEBUSY info
    var users = args.getStrings("attendee");
    var busy = getPeriods(args.getStrings("busy"));
    var tentative = getPeriods(args.getStrings("tentative"));
    var unavailable = getPeriods(args.getStrings("unavailable"));
    var events = args.getInt("events");

    if (!parseXml(respdata)) {
      return result;
    }

    // Extract each calendar-data object

    for (var resp : children(docRoot)) {
      if (!nodeMatches(resp, CaldavTags.response)) {
        fmsg("        Expect only responses as children. Found: %s",
             resp);
        return result;
      }

      for (var ch : children(resp)) {
        if (!nodeMatches(ch, CaldavTags.calendarData)) {
          continue;
        }

        var calendar = Icalendar.parseText(content(ch));

        // Only one component
        var comps = calendar.getComponents("VFREEBUSY");
        if (comps.size() != 1) {
          append("Wrong number or unexpected components in calendar");
          return result;
        }

        // Must be VFREEBUSY
        final VFreeBusy fb = (VFreeBusy)comps.get(0);

        // Check for attendee value
        for (var attendee : fb.getProperties("ATTENDEE")) {
          var cua = attendee.getValue();

          if (users.contains(cua)) {
            users.remove(cua);
            break;
          }
        }

        // Extract periods
        var busyp = new ArrayList<Period>();
        var tentativep = new ArrayList<Period>();
        var unavailablep = new ArrayList<Period>();

        for (var fp : fb.getProperties("FREEBUSY")) {
          var periods = ((net.fortuna.ical4j.model.property.FreeBusy)fp)
                  .getPeriods();

          // Check param
          final String fbtype;
          final FbType fbtPar = (FbType)fp
                  .getParameter(Parameter.FBTYPE);

          if (fbtPar == null) {
            fbtype = "BUSY";
          } else {
            fbtype = fbtPar.getValue().toUpperCase();
          }

          switch (fbtype) {
            case "BUSY":
              busyp.addAll(periods);
              break;
            case "BUSY-TENTATIVE":
              tentativep.addAll(periods);
              break;
            case "BUSY-UNAVAILABLE":
              unavailablep.addAll(periods);
              break;
            default:
              fmsg("Unknown FBTYPE: %s", fbtype);
              return result;
          }
        }

        // Set sizes must match
        if (busy.size() != busyp.size()) {
          append("Busy period list sizes do not match.");
          return result;
        }

        if (unavailable.size() != unavailablep.size()) {
          append("Unavailable period list sizes do not match.");
          return result;
        }

        if (tentative.size() != tentativep.size()) {
          append("Tentative period list sizes do not match.");
          return result;
        }

        // Compare all periods
        if (symmetricDiff(busyp, busy).size() != 0) {
          fmsg("Busy periods do not match: {}",
               symmetricDiff(busyp, busy));
        } else if (symmetricDiff(tentativep, tentative).size() != 0) {
          append("Busy-tentative periods do not match");
        } else if (symmetricDiff(unavailablep, unavailable)
                .size() != 0) {
          append("Busy-unavailable periods do not match");
        }

        // Check event count
        if ((events != null) &&
                (calendar.getComponents("VEVENT").size() != events)) {
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
