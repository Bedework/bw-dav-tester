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
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.FbType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.Utils.symmetricDiff;
import static org.bedework.davtester.Utils.throwException;

/**
 * Verifier that checks the response of a free-busy-query.
 */
public abstract class FreeBusyBase extends Verifier {
  protected class PeriodsChecker {
    PeriodList busy;
    PeriodList tentative;
    PeriodList unavailable;

    Icalendar calendar;
    VFreeBusy fb;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean getPeriods(final int status,
                       final KeyVals args) {
      // Must have status 200
      if (status != 200) {
        fmsg("        HTTP Status Code Wrong: %d", status);
        return false;
      }

      // Get expected FREEBUSY info
      busy = getPeriods(args.getStrings("busy"));
      tentative = getPeriods(args.getStrings("tentative"));
      unavailable = getPeriods(args.getStrings("unavailable"));

      return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean parseData(final String respdata) {
      // Parse data as calendar object

      calendar = Icalendar.parseText(respdata);

      // Only one component
      var comps = calendar.getComponents("VFREEBUSY");
      if (comps.size() != 1) {
        append("Wrong number or unexpected components in calendar");
        return false;
      }

      // Must be VFREEBUSY
      fb = (VFreeBusy)comps.get(0);

      return true;
    }

    boolean comparePeriods() {
      var busyp = new ArrayList<Period>();
      var tentativep = new ArrayList<Period>();
      var unavailablep = new ArrayList<Period>();

      for (var fp: fb.getProperties("FREEBUSY")) {
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
            return false;
        }
      }

      // Set sizes must match
      if (busy.size() != busyp.size()) {
        append("Busy period list sizes do not match.");
        return false;
      }

      if (unavailable.size() != unavailablep.size()) {
        append("Unavailable period list sizes do not match.");
        return false;
      }

      if (tentative.size() != tentativep.size()) {
        append("Tentative period list sizes do not match.");
        return false;
      }

      // Compare all periods
      if (symmetricDiff(busyp, busy).size() != 0) {
        fmsg("Busy periods do not match: {}",
             symmetricDiff(busyp, busy));
        return false;
      }

      if (symmetricDiff(tentativep, tentative).size() != 0) {
        append("Busy-tentative periods do not match");
        return false;
      }

      if (symmetricDiff(unavailablep, unavailable)
              .size() != 0) {
        append("Busy-unavailable periods do not match");
        return false;
      }

      return true;
    }

    protected PeriodList getPeriods(final List<String> vals) {
      final PeriodList pl = new PeriodList();

      for (final var val: vals) {
        try {
          pl.add(new Period(val));
        } catch (final ParseException pe) {
          throwException(pe);
        }
      }

      return pl;
    }
  }
}
