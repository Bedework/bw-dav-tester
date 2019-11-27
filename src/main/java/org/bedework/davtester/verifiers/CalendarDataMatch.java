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

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.apache.http.Header;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.String.format;
import static org.bedework.util.xml.tagdefs.CaldavTags.calendar;

/**
 * Verifier that checks the response body for a semantic match to data in a file.
 */
public class CalendarDataMatch extends Verifier {
  @Override
  public VerifyResult verify(final URI uri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var files = args.getStrings("filepath");
    if (manager.data_dir) {
      files = map(lambda x:
      os.path.join(manager.data_dir, x), files)
    }
    var caldata = args.getStrings("data");
    var filters = args.getStrings("filter");
    var statusCode = args.getStrings("status", "200", "201", "207");

    if (!featureSupported("EMAIL parameter")){
      filters.append("ATTENDEE:EMAIL");
      filters.append("ORGANIZER:EMAIL");
    }
    filters.extend(manager.serverInfo.calendardatafilters);

    for (afilter in tuple(filters)) {
      if (afilter[0] == "!" and afilter[ 1:]in filters){
        filters.remove(afilter[1:])
      }
    }
    filters = filter(lambda x: x[0] != "!", filters);

    final boolean doTimezones;

    if (!args.containsKey("doTimezones")) {
      doTimezones = !featureSupported("timezones-by-reference");
    } else {
      doTimezones = args.getOnlyBool("doTimezones");
    }

    // status code must be 200, 201, 207 or explicitly specified code

    if (!statusCode.contains(String.valueOf(status))) {
      new VerifyResult(format("        HTTP Status Code Wrong: %d",
                              status));
    }

    // look for response data
    if (respdata == null) {
      new VerifyResult("        No response body";
    }

    // look for one file
    if ((files.size() != 1) && (len(caldata) != 1)) {
      return new VerifyResult(
              "        No file to compare response to");
    }

    // read in all data from specified file or use provided data
    if (len(files)) {
      fd = open(files[0], "r");
      try {
        try {
          data = fd.read();
        } finally {
          fd.close();
        }
      } except {
        data = null;
      }
    } else {
      if (caldata.size() == 0) {
        data = null;
      } else {
        data = caldata[0];
      }
    }

    if (data == null) {
      return new VerifyResult("        Could not read data file");
    }

    data = manager.serverInfo.extrasubs(manager.serverInfo.subs(data));

    try {
      final String format;
      if (is_json) {
        format = Calendar.sFormatJSON;
      } else {
        format = Calendar.sFormatText;
      }

      resp_calendar = Calendar.parseData(respdata, format);
      removePropertiesParameters(resp_calendar);

      data_calendar = Calendar.parseData(data, format);
      removePropertiesParameters(data_calendar);

      reconcileRecurrenceOverrides(resp_calendar, data_calendar);

      respdata = resp_calendar.getText(includeTimezones=Calendar.NO_TIMEZONES, format);
      data = data_calendar.getText(includeTimezones=Calendar.NO_TIMEZONES, format);

      result = resp_calendar == data_calendar;
      if (!result) {
        respdata2 = respdata.replace("\r\n ", "");
        data2 = data
                .replace("\r\n ", "")
                .replace("urn:x-uid:",
                         "urn:uuid:");
        result = respdata2 == data2;
      }

      if (result) {
        return new VerifyResult();
      }
      error_diff = "\n".join([line for line in unified_diff(data.split("\n"), respdata.split("\n"))])
      return new VerifyResult(format("        Response data does not exactly match file data%s", error_diff);
    } except Exception, e) {
      return new VerifyResult(format("        Response data is not calendar data: %s", e);
    }
  }

  private void addOverrides(calendar, master, missing_rids){
    /*
    Derive instances for the missing overrides in the specified calendar object.
    */
    if ((master == null) || !missing_rids) {
      return;
    }
    for (var rid : missing_rids) {
      // if (we were fed an already derived component, use that, otherwise make a new one
      newcomp = calendar.deriveComponent(rid)
      if (newcomp != null) {
        calendar.addComponent(newcomp);
      }
    }
  }

  private Set<RecurrenceId> getRids(calendar)){
    /*
    Get all the recurrence ids of the specified calendar.
    */
    var results = new TreeSet<RecurrenceId>();
    master = null;
    for (var subcomponent : calendar
            .getComponents()) {
      if (isinstance(subcomponent,
                     ComponentRecur)) {
        rid = subcomponent
                .getRecurrenceID();
        if (rid) {
          results.add(
                  rid.duplicateAsUTC());
        } else {
          master = subcomponent;
        }
      }
    }
    return results,master
  }

  private void reconcileRecurrenceOverrides(calendar1, calendar2) {
    /*
      Make sure that the same set of overridden components appears in both calendar objects.
    */

    rids1, master1 = getRids(calendar1);
    rids2, master2 = getRids(calendar2);

    addOverrides(calendar1, master1,
                 rids2 - rids1);
    addOverrides(calendar2, master2,
                 rids1 - rids2);
  }

  private void removePropertiesParameters(final Component component) {

    if (!doTimezones) {
      for (var subcomponent: tuple(component.getComponents())) {
        if (subcomponent.getType() == "VTIMEZONE") {
          component.removeComponent(subcomponent);
        }
      }
    }

    for (var subcomponent : component.getComponents()) {
      removePropertiesParameters(subcomponent)
    }

    if (component.getType() == "VEVENT") {
      if (component.hasEnd()) {
        component.editTimingStartDuration(
                component.getStart(),
                component.getEnd() - component
                        .getStart());
      }
    }

    var allProps = new ArrayList<Property>();
    for (var properties : component
            .getProperties()) {
      allProps.extend(properties);
    }
    for (var property : allProps) {
      // Always reset DTSTAMP on these properties
      if (property.getName()
      in("ATTENDEE",
         "X-CALENDARSERVER-ATTENDEE-COMMENT")){
        if (property.hasParameter(
                "X-CALENDARSERVER-DTSTAMP")) {
          property.replaceParameter(
                  Parameter(
                          "X-CALENDARSERVER-DTSTAMP",
                          "20080101T000000Z"));
        }
      }

      for (var filter : filters) {
        if (filter.contains(":")) {
          propname, parameter = filter.split(":");
          if (property.getName() == propname) {
            if (property.hasParameter(parameter)) {
              property.removeParameters(parameter);
            }
          }
        } else {
          if (filter.contains("=")) {
            filter_name, filter_value = filter
                    .split("=")
            if (property.getName()
                        .equals(filterName) &&
                    property.getValue()
                            .getValue() == filter_value) {
              component.removeProperty(property);
            }
          } else {
            if (property.getName() == filter) {
              component.removeProperty(property)
            }
          }
        }
      }
    }
  }
}
