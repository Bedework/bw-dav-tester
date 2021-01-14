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
import org.bedework.davtester.Utils;
import org.bedework.davtester.ical.Icalendar;
import org.bedework.util.misc.Util;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.apache.http.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Verifier that matches an ical response body to a file.
 */
public class IcalendarDataMatch extends FileDataMatch {
  @Override
  public List<Integer> expectedStatus(final KeyVals args) {
    return args.getInts("status", 200, 201, 207);
  }

  @Override
  public void compare(final String ruri,
                      final List<Header> responseHeaders,
                      final int status,
                      final String respdata,
                      final KeyVals args,
                      final String filepath,
                      final List<String> filters,
                      final String data) {
    if (!featureSupported("EMAIL parameter")) {
      filters.add("ATTENDEE:EMAIL");
      filters.add("ORGANIZER:EMAIL");
    }

    // Add default filters
    filters.addAll(manager.serverInfo.calendardatafilters);

    // Prefix of ! indicates remove the filter - used to remove a default

    for (final var afilter: new ArrayList<>(filters)) {
      if (afilter.startsWith("!")) {
        filters.remove(afilter.substring(1));
      }
      filters.remove(afilter);
    }

    /*
    final boolean doTimezones;

    if (!args.containsKey("doTimezones")) {
      doTimezones = !featureSupported("timezones-by-reference");
    } else {
      doTimezones = args.getOnlyBool("doTimezones");
    }
*/
    try {
      final var respCalendar = Icalendar.parseText(respdata);
      removePropertiesParameters(respCalendar,
                                 filters);

      final var dataCalendar = Icalendar.parseText(data);
      removePropertiesParameters(dataCalendar,
                                 filters);

      respCalendar.removeTimeZones();
      dataCalendar.removeTimeZones();

      reconcileRecurrenceOverrides(respCalendar, dataCalendar);

      final var respLines = respCalendar.toLines(/*Calendar.NO_TIMEZONES*/);
      final var dataLines = dataCalendar.toLines(/*Calendar.NO_TIMEZONES*/);

      final Patch<String> patch = DiffUtils.diff(dataLines, respLines);

      if (Util.isEmpty(patch.getDeltas())) {
        return;
      }

      final List<String> unifiedDiff = UnifiedDiffUtils
              .generateUnifiedDiff("Response",
                                   "Expected", dataLines, patch, 0);

      final var errorDiff = new StringBuilder();

      for (final var s: unifiedDiff) {
        errorDiff.append(s);
        errorDiff.append('\n');
      }

      fmsg("        Response data does not " +
                   "exactly match file data%s",
           errorDiff);
    } catch (final Throwable t) {
      t.printStackTrace();
      fmsg("        Response data is not calendar data: %s",
           t.getMessage());
    }
  }

  private static class MasterRids {
    Component master;
    List<RecurrenceId> rids;

    void add(final RecurrenceId p) {
      if (rids == null) {
        rids = new ArrayList<>();
      }

      rids.add(p);
    }
  }

  /* If overrides are missing add an override based on the master event.
   * That way we see what was expected to be changed.
   */
  private void reconcileRecurrenceOverrides(final Icalendar calendar1,
                                            final Icalendar calendar2) {
    /* Make sure that the same set of overridden components appears in
       both calendar objects.
    */

    final var rids1 = getRids(calendar1);
    final var rids2 = getRids(calendar2);

    addOverrides(calendar1, rids1.master,
                 Utils.diff(rids2.rids, rids1.rids));
    addOverrides(calendar2, rids2.master,
                 Utils.diff(rids1.rids, rids2.rids));

    // Need to sort the components so they match.
    calendar1.sort();
    calendar2.sort();
  }

  /* this assumes the calendar represents a single event. True for CalDAV
   */
  private MasterRids getRids(final Icalendar calendar){
    /* Get all the recurrence ids of the specified calendar.
    */
    final var res = new MasterRids();

    for (final var subcomponent: calendar.getComponents()) {
      final var p = (RecurrenceId)subcomponent.getProperty(Property.RECURRENCE_ID);

      if (p == null) {
        if ((subcomponent.getProperty(Property.RDATE) != null) ||
                (subcomponent.getProperty(Property.RRULE) != null)) {
          res.master = subcomponent;
        }
      } else {
        res.add(p);
      }
    }

    return res;
  }

  private void addOverrides(final Icalendar calendar,
                            final Component master,
                            final List<RecurrenceId> missingRids){
    /* Derive instances for the missing overrides in the specified calendar object.
     */
    if ((master == null) || Util.isEmpty(missingRids)) {
      return;
    }

    for (final var rid: missingRids) {
      // if (we were fed an already derived component, use that, otherwise make a new one
      final var newcomp = calendar.deriveComponent(rid);
      if (newcomp != null) {
        calendar.addComponent(newcomp);
      }
    }
  }

  private static final Set<String> attendeeProps =
          new TreeSet<>(Arrays.asList("ATTENDEE",
                        "X-CALENDARSERVER-ATTENDEE-COMMENT"));

  private void removePropertiesParameters(final Object comp,
                                          final List<String> filters) {
    /* why are we setting it to duration - are servers changing the
       representation?
    if (component.getType() == "VEVENT") {
      if (component.hasEnd()) {
        component.editTimingStartDuration(
                component.getStart(),
                component.getEnd() - component
                        .getStart());
      }
    }
     */

    final var newProps = new ArrayList<Property>();
    final PropertyList<Property> pl;
    Icalendar ical = null;
    Component component = null;

    if (comp instanceof Icalendar) {
      ical = (Icalendar)comp;
      pl = ical.cal.getProperties();
    } else {
      component = (Component)comp;
      pl = component.getProperties();
    }

    for (final var property: pl) {
      // Always reset DTSTAMP on these properties
      if (attendeeProps.contains(property.getName())) {
        final Parameter par = property
                .getParameter("X-CALENDARSERVER-DTSTAMP");
        if (par != null) {
          property.getParameters().remove(par);
          property.getParameters()
                  .add(new XParameter("X-CALENDARSERVER-DTSTAMP",
                                      "20080101T000000Z"));
        }
      }

      for (final var filter: filters) {
        if (filter.contains(":")) {
          final var split = filter.split(":");
          if (property.getName().equals(split[0])) {
            final Parameter par = property.getParameter(split[1]);
            if (par != null) {
              property.getParameters().remove(par);
            }
          }

          newProps.add(property);
          continue;
        }

        if (filter.contains("=")) {
          final var split = filter.split("=");
          if (property.getName().equals(split[0]) &&
                  property.getValue().equals(split[1])) {
            continue; // don't preserve
          }

          newProps.add(property);
          continue;
        }

        if (property.getName().equals(filter)) {
          continue; // don't preserve
        }

        newProps.add(property);
      }
    }

    final ComponentList<? extends Component> comps;

    if (ical != null) {
      ical.cal.getProperties().clear();
      ical.cal.getProperties().addAll(newProps);
      comps = ical.cal.getComponents();
    } else {
      component.getProperties().clear();
      component.getProperties().addAll(newProps);
      comps = component.getComponents();
    }

    for (final var c: comps) {
      removePropertiesParameters(c, filters);
    }
  }
}
