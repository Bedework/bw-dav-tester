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

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.FbType;
import org.apache.http.Header;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.Utils.symmetricDiff;

/**
 * Verifier that checks the response of a free-busy-query.
 */
public class FreeBusy extends Verifier {
  @Override
  public VerifyResult verify(final URI uri,
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
    var busy = getPeriods(args.getStrings("busy"));
    var tentative = getPeriods(args.getStrings("tentative"));
    var unavailable = getPeriods(args.getStrings("unavailable"));
    //var duration = args.getOnlyBool("duration");

    // Parse data as calendar object

    var calendar = Icalendar.parseText(respdata);

    // Only one component
    var comps = calendar.getComponents("VFREEBUSY");
    if (comps.size() != 1) {
      append("Wrong number or unexpected components in calendar");
      return result;
    }

    // Must be VFREEBUSY
    final VFreeBusy fb = (VFreeBusy)comps.get(0);

    // Extract periods
    var busyp = new ArrayList<Period>();
    var tentativep = new ArrayList<Period>();
    var unavailablep = new ArrayList<Period>();

    for (var fp : fb.getProperties("FREEBUSY")) {
      var periods = ((net.fortuna.ical4j.model.property.FreeBusy)fp)
              .getPeriods();

      // Check param
      final String fbtype;
      final FbType fbtPar = (FbType)fp.getParameter(Parameter.FBTYPE);

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
    } else if (symmetricDiff(unavailablep, unavailable).size() != 0) {
      append("Busy-unavailable periods do not match");
    }

    return result;
  }
}
