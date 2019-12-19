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

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.XParameter;
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

    for (var afilter: new ArrayList<>(filters)) {
      if (afilter.startsWith("!")) {
        filters.remove(afilter.substring(1));
      }
      filters.remove(afilter);
    }

    final boolean doTimezones;

    if (!args.containsKey("doTimezones")) {
      doTimezones = !featureSupported("timezones-by-reference");
    } else {
      doTimezones = args.getOnlyBool("doTimezones");
    }

    try {
      var respCalendar = Icalendar.parseText(respdata);
      removePropertiesParameters(respCalendar, doTimezones, filters);

      var dataCalendar = Icalendar.parseText(data);
      removePropertiesParameters(dataCalendar, doTimezones, filters);

      // Why is this being done?
      //reconcileRecurrenceOverrides(respCalendar, dataCalendar);

      var respLines = respCalendar.toLines(/*Calendar.NO_TIMEZONES*/);
      var dataLines = dataCalendar.toLines(/*Calendar.NO_TIMEZONES*/);

      Patch<String> patch = DiffUtils.diff(dataLines, respLines);

      if (Util.isEmpty(patch.getDeltas())) {
        return;
      }

      /*var result = respCalendar == dataCalendar;
      if (!result) {
        var respdata2 = respdata.replace("\r\n ", "");
        var data2 = data
                .replace("\r\n ", "")
                .replace("urn:x-uid:",
                         "urn:uuid:");
        result = respdata2.equals(data2);
      }

      if (result) {
        return new VerifyResult();
      }
*/

      var errorDiff = new StringBuilder();

      for (AbstractDelta<String> delta : patch.getDeltas()) {
        errorDiff.append(delta.toString());
        errorDiff.append('\n');
      }

      fmsg("        Response data does not " +
                   "exactly match file data%s",
           errorDiff);
    } catch (final Throwable t) {
      fmsg("        Response data is not calendar data: %s",
           t.getMessage());
    }
  }

  /*
  private void addOverrides(final Icalendar calendar,
                            final Component master,
                            final List<Property> missingRids){
    /*
    Derive instances for the missing overrides in the specified calendar object.
    * /
    if ((master == null) || Util.isEmpty(missingRids)) {
      return;
    }

    for (var rid : missingRids) {
      // if (we were fed an already derived component, use that, otherwise make a new one
      var newcomp = calendar.deriveComponent(rid);
      if (newcomp != null) {
        calendar.addComponent(newcomp);
      }
    }
  }

  static class MasterRids {
    Component master;
    List<Property> rids;

    void add(final Property p) {
      if (rids == null) {
        rids = new ArrayList<>()
      }

      rids.add(p);
    }
  }

  /* this assumes the calendar represents a single event. True for CalDAV
   * /
  private MasterRids getRids(final Icalendar calendar){
    /*
    Get all the recurrence ids of the specified calendar.
    * /
    var res = new MasterRids();

    for (var subcomponent : calendar.getComponents()) {
      var p = subcomponent.getProperty(Property.RECURRENCE_ID);

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

  private void reconcileRecurrenceOverrides(final Icalendar calendar1,
                                            final Icalendar calendar2) {
    /*
      Make sure that the same set of overridden components appears in both calendar objects.
    * /

    var rids1 = getRids(calendar1);
    var rids2 = getRids(calendar2);

    addOverrides(calendar1, rids1.master,
                 diff(rids2.rids, rids1.rids);
    addOverrides(calendar2, rids2.master,
                 diff(rids1.rids, rids2.rids));
  }
*/

  private static final Set<String> attendeeProps =
          new TreeSet<>(Arrays.asList("ATTENDEE",
                        "X-CALENDARSERVER-ATTENDEE-COMMENT"));

  private void removePropertiesParameters(final Component component,
                                          final boolean doTimezones,
                                          final List<String> filters) {
    if (!doTimezones) {
      var toRemove = new ArrayList<Component>();

      for (var subcomponent: component.getComponents()) {
        if (subcomponent instanceof VTimeZone) {
          toRemove.add(subcomponent);
          continue;
        }
        removePropertiesParameters(subcomponent, doTimezones, filters);
      }

      for (var c: toRemove) {
        component.getComponents().remove(c);
      }
    }

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

    var allProps = new ArrayList<>(component.getProperties());

    for (var property : allProps) {
      // Always reset DTSTAMP on these properties
      if (attendeeProps.contains(property.getName())) {
        Parameter par = property.getParameter("X-CALENDARSERVER-DTSTAMP");
        if (par != null) {
          property.getParameters().remove(par);
          property.getParameters().add(new XParameter("X-CALENDARSERVER-DTSTAMP",
                                                      "20080101T000000Z"));
        }
      }

      for (var filter: filters) {
        if (filter.contains(":")) {
          var split = filter.split(":");
          if (property.getName().equals(split[0])) {
            Parameter par = property.getParameter(split[1]);
            if (par != null) {
              property.getParameters().remove(par);
            }
          }
          continue;
        }

        if (filter.contains("=")) {
          var split = filter.split("=");
          if (property.getName().equals(split[0]) &&
                  property.getValue().equals(split[1])) {
            component.getProperties().remove(property);
          }
          continue;
        }

        if (property.getName().equals(filter)) {
          component.getProperties().remove(property);
        }
      }
    }
  }
}
