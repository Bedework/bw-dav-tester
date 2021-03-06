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
package org.bedework.davtester;

//import datetime;
//import XmlDefs;
//import urlparse;
//import uuid4;

import org.bedework.davtester.Utils.DtParts;
import org.bedework.util.misc.ToString;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.getDtParts;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.davtester.XmlUtils.floatVal;
import static org.bedework.davtester.XmlUtils.intVal;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

//from uuid
//        from urlparse

/**
 * Class that encapsulates the server information for a CalDAV test run.
 */
public class Serverinfo {
  final private Manager manager;

  public String host;
  public int port;
  public String afunix;

  /* unused?
  public String host2 = "";
  public int port2;
  public String afunix2;
  int nonsslport2 = 80;
  int sslport2 = 443;
   */

  int nonsslport = 80;
  int sslport = 443;
  String authtype = "basic";
  String certdir = "";

  boolean ssl;

  Set<String> features = new TreeSet<>();
  public String user = null;
  public String pswd = null;
  public int waitcount = 120;
  public long waitdelay = 250; // .25 second
  int waitsuccess = 10;

  final KeyVals subsKvs = new KeyVals();
  final KeyVals extrasubsKvs = new KeyVals();

  // Used so we can flag which test a uid came from
  public Map<String, String> uidmaps = new HashMap<>();

  final KeyVals defaultFilters = new KeyVals();
  public List<String> calendardatafilters = new ArrayList<>();
  List<String> addressdatafilters = new ArrayList<>();

  public static class KeyVal {
    public final String key;
    public final String val;

    public KeyVal(final String key,
                  final String val) {
      this.key = key;
      this.val = val;
    }
  }

  Serverinfo(final Manager manager,
             final String baseDir) {
    this.manager = manager;
    subsKvs.put("$basedir:", baseDir);
  }

  public String getScheme() {
    if (ssl) {
      return "https";
    }
    return "http";
  }

  // dtnow needs to be fixed to a single date at the start of the tests just in case the tests
  // run over a day boundary.

  public String subs(final String subval) {
    return subs(subval, null);
  }

  public String subs(final String subval,
                     final KeyVals db) {
    if (subval == null) {
      return null;
    }
    var sub = subval;

    // Special handling for relative date-times
    var pos = sub.indexOf("$now.");
    while (pos != -1) {
      final var endpos = sub.indexOf(":", pos);
      final var subpos = sub.substring(pos);
      final String value;

      final DtParts dtp = getDtParts();

      if (subpos.startsWith("$now.year.")) {
        final var yearoffset = ival(sub, pos + 10, endpos);
        value = String.format("%d", dtp.year + yearoffset);
      } else if (subpos.startsWith("$now.month.")) {
        final var monthoffset = ival(sub, pos + 11, endpos);
        var month = dtp.month + monthoffset;
        final var year = dtp.year + (month / 12);
        month = month % 12;
        value = String.format("%d%02d", year, month);
      } else {
        final int dayOffset;
        if (sub.substring(pos).startsWith("$now.week.")) {
          final var weekoffset = ival(sub, pos + 10, endpos);
          dayOffset = 7 * weekoffset;
        } else {
          dayOffset = ival(sub, pos + 5, endpos);
        }

        final var offDtp = getDtParts(dayOffset);

        value = String.format("%d%02d%02d",
                              offDtp.year,
                              offDtp.month,
                              offDtp.dayOfMonth);
      }

      sub = String.format("%s%s%s", sub.substring(0, pos), value, sub.substring(endpos + 1));
      pos = sub.indexOf("$now.");
    }

    if (sub.contains("$uidrandom:")) {
      sub = sub.replace("$uidrandom:", UUID.randomUUID().toString());
    }

    final KeyVals kv;

    if (db == null) {
      kv = subsKvs;
    } else {
      kv = db;
    }

    return propertyReplace(sub, kv);
  }

  void addsubs(final KeyVals items,
               final KeyVals db) {
    final KeyVals dbActual;

    dbActual = Objects.requireNonNullElse(db, subsKvs);

    for (final var key: items.keySet()){
      dbActual.put(key, items.get(key));
    }

    if (db == null) {
      updateParams();
    }
  }

  public boolean hasextrasubs () {
    return extrasubsKvs.size() > 0;
  }

  public String extrasubs(final String str) {
    return subs(str, extrasubsKvs);
  }

  public void addextrasubs(final KeyVals items) {
    final KeyVals processed = new KeyVals();

    // Various "functions" might be applied to a variable name to cause the value to
    // be changed in various ways
    for (var variable: items.keySet()) {
      // basename() - extract just the URL last path segment from the value
      if (variable.startsWith("basename(")) {
        variable = variable.substring("basename(".length(),
                                      variable.length() - 1);
        String value = items.getOnlyString(variable).trim();
        if (value.endsWith("/")) {
          value = value.substring(0, value.length() - 1);
        }

        final var els = value.split("/");

        value = els[els.length - 1];

        // urlpath() - extract just the URL path segment from the value
        processed.put(variable, value);
      } else if (variable.startsWith("urlpath(")) {
        variable = variable.substring("urlpath(".length(),
                                      variable.length() - 1);
        final URL url;
        String value = items.getOnlyString(variable);
        try {
          url = new URL(value);
        } catch (final MalformedURLException e) {
          throw new RuntimeException(e);
        }
        value = url.getPath();
        processed.put(variable, value);
      } else {
        processed.put(variable, items.get(variable));
      }
    }

    addsubs(processed, extrasubsKvs);
  }

  /**
   *
   */
  public void newUIDs () {
    uidmaps.clear();
    for (int i = 1; i <= 21; i++) {
      final var key = String.format("$uid%d:", i);
      final var val = UUID.randomUUID().toString();
      subsKvs.put(key, val);
      extrasubsKvs.put(key, val);

      uidmaps.put(val, format("%s - %s", key,
                              manager.currentTestfile.name));
    }
  }

  public void parseXML(final Node node) {
    for (final var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_HOST)) {
        try {
          host = contentUtf8(child);
        } catch (final Throwable t){
          host = "localhost";
        }
      } else if (nodeMatches(child, XmlDefs.ELEMENT_NONSSLPORT)) {
        nonsslport = Integer.parseInt(content(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SSLPORT)) {
        sslport = Integer.parseInt(content(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_UNIX)) {
        afunix = content(child);
        /*HOST2
      } else if (nodeMatches(child, XmlDefs.ELEMENT_HOST2)) {
        try {
          host2 = textUtf8;
        } catch (final Throwable t) {
          host2 = "localhost";
        }
      } else if (nodeMatches(child, XmlDefs.ELEMENT_NONSSLPORT2)) {
        nonsslport2 = Integer.valueOf(text);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SSLPORT2)) {
        sslport2 = Integer.valueOf(text);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_UNIX2)) {
        afunix2 = text;
         */
      } else if (nodeMatches(child, XmlDefs.ELEMENT_AUTHTYPE)) {
        authtype = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_CERTDIR)) {
        certdir = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITCOUNT)) {
        waitcount = intVal(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITDELAY)) {
        waitdelay = (long)(floatVal(child) * 1000);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITSUCCESS)) {
        waitsuccess = Integer.parseInt(content(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_FEATURES)) {
        parseFeatures(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTIONS)) {
        parseSubstitutionsXML(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_DEFAULTFILTERS)) {
        parseDefaultFilters(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_CALENDARDATAFILTER)) {
        calendardatafilters.add(contentUtf8(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_ADDRESSDATAFILTER)) {
        addressdatafilters.add(contentUtf8(child));
      }
    }

    updateParams();
  }

  public void parseFeatures (final Node node) {
    for (final var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_FEATURE)) {
        features.add(contentUtf8(child));
      }
    }
  }

  public void updateParams () {
    // Expand substitutions fully at this point
    for (final var key: subsKvs.keySet()) {
      subsKvs.put(key,
                  propertyReplace(subsKvs.getOnlyString(key),
                                  subsKvs));
    }

    // Now cache some useful substitutions
    final String uname;
    if (subsKvs.containsKey("$userid1:")) {
      uname = "$userid1:";
    } else {
      uname = "$userid01:";
    }

    final String pname;
    if (subsKvs.containsKey("$pswd1:")) {
      pname = "$pswd1:";
    } else {
      pname = "$pswd01:";
    }

    user = subsKvs.getOnlyString(uname);
    if (user == null) {
      throw new RuntimeException("Must have userid substitution");
    }

    pswd = subsKvs.getOnlyString(pname);
    if (pswd == null) {
      throw new RuntimeException("Must have pswd substitution");
    }
  }

  public void parseRepeatXML(final Element node){
    // Look for count
    final var count = XmlUtils.getIntAttributeValue(node, XmlDefs.ATTR_COUNT, 1);

    for (final var child: children(node)) {
      if(nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTION)) {
        parseSubstitutionXML(child, count);
      }
    }
  }

  public void parseSubstitutionsXML(final Node node){
    for (final var child: children(node)) {
      if(nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTION)){
        parseSubstitutionXML(child, 0);
      } else if(nodeMatches(child, XmlDefs.ELEMENT_REPEAT)){
        parseRepeatXML(child);
      }
    }
  }

  public void parseDefaultFilters(final Node node){
    String name = null;
    final List<Object> values = new ArrayList<>();

    for (final var child: children(node)) {
      if(nodeMatches(child, XmlDefs.ELEMENT_NAME)){
        name = contentUtf8(child);
      } else if(nodeMatches(child, XmlDefs.ELEMENT_VALUE)){
        if (content(child) != null) {
          values.add(subs(contentUtf8(child), null));
        } else {
          values.add("");
        }
      }
    }

    if (name != null) {
      defaultFilters.put(name, values);
    }
  }

  public void parseSubstitutionXML(final Node node, final int repeat) {
    String key = null;
    String value = null;
    for (final var schild: children(node)) {
      if (nodeMatches(schild, XmlDefs.ELEMENT_KEY)) {
        key = contentUtf8(schild);
      } else if (nodeMatches(schild, XmlDefs.ELEMENT_VALUE)) {
        final var str = contentUtf8(schild);

        value = Objects.requireNonNullElse(str, "");
      }
    }

    if ((key == null) || (value == null)) {
      return;
    }

    if (repeat == 0) {
      subsKvs.put(key, value);
      return;
    }

    // Key is a format
    // Value might be
    for (var count = 1; count <= repeat; count++) {
      if (value.contains("%")) {
        subsKvs.put(String.format(key, count),
                    String.format(value, count));
      } else {
        subsKvs.put(String.format(key, count), value);
      }
    }
  }

  public static String propertyReplace(final String val,
                                       final KeyVals props) {
    if (val == null) {
      return null;
    }

    var res = val;

    while (true) {
      final var newval = replace1Pass(res, props);
      if (newval.equals(res)) {
        return res;
      }

      res = newval;
    }
  }

  private static String replace1Pass(final String val,
                                     final KeyVals props) {
    int pos = val.indexOf("$");

    if (pos < 0) {
      return val;
    }

    final StringBuilder sb = new StringBuilder(val.length());
    int segStart = 0;

    while (true) {
      if (pos > segStart) {
        sb.append(val, segStart, pos);
      }

      final int end = val.indexOf(":", pos);

      if (end < 0) {
        //No matching close. Just append rest and return.
        sb.append(val.substring(pos));
        break;
      }

      final var remPos = end + 1;
      final String matched = val.substring(pos, remPos).trim();

      final String pval = props.getOnlyString(matched);

      if (pval != null) {
        sb.append(pval);
        sb.append(val.substring(remPos));
        break;
      }

      // No match - just append the unmatched key.
      sb.append(matched);
      segStart = remPos;

      if (segStart >= val.length()) {
        break;
      }

      pos = val.indexOf("$", segStart);

      if (pos < 0) {
        //Done.
        sb.append(val.substring(segStart));
        break;
      }
    }

    return sb.toString();
  }

  public static int ival(final String val, final int start, final int end) {
    return Integer.parseInt(val.substring(start, end));
  }

  public String toString() {
    final ToString ts = new ToString(this);

     ts.append("host", host);
     ts.append("port", port);
     ts.append("afunix", afunix);

    ts.append("nonsslport", nonsslport);
    ts.append("sslport", sslport);
    ts.append("authtype", authtype);
    ts.append("certdir", certdir);

    ts.append("ssl", ssl);

    ts.append("features", features);
    ts.append("user", user);
    ts.append("pswd", pswd);
    ts.append("waitcount", waitcount);
    ts.append("waitdelay", waitdelay);
    ts.append("waitsuccess", waitsuccess);
    ts.append("subsKvs", subsKvs);
    ts.append("extrasubsKvs", extrasubsKvs);
    ts.append("calendardatafilters", calendardatafilters);
    ts.append("addressdatafilters", addressdatafilters);

    return ts.toString();
  }
}
