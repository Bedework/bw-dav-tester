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
import org.bedework.util.misc.Util;
import org.bedework.util.misc.Util.PropertiesPropertyFetcher;
import org.bedework.util.xml.XmlUtil;

import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.bedework.davtester.Utils.getDtParts;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

//from uuid
//        from urlparse

/**
 * Class that encapsulates the server information for a CalDAV test run.
 */
public class Serverinfo {
  public String host;
  public int port;
  public String afunix;

  public String host2 = "";
  public int port2;
  public String afunix2;

  int nonsslport = 80;
  int sslport = 443;
  int nonsslport2 = 80;
  int sslport2 = 443;
  String authtype = "basic";
  String certdir = "";

  boolean ssl;

  Set<String> features = new TreeSet<>();
  public String user = "";
  public String pswd = "";
  int waitcount = 120;
  double waitdelay = 0.25;
  int waitsuccess = 10;
  final KeyVals subsKvs = new KeyVals();
  final Properties extrasubsdict = new Properties();
  List<String> calendardatafilters = new ArrayList<>();
  List<String> addressdatafilters = new ArrayList<>();
  Date dtnow = new Date();

  public static class KeyVal {
    public final String key;
    public final String val;

    public KeyVal(final String key,
                  final String val) {
      this.key = key;
      this.val = val;
    }
  }

  private final PropertiesPropertyFetcher subsPfetcher =
          new PropertiesPropertyFetcher(subsKvs);

  private final PropertiesPropertyFetcher extrasubsPfetcher =
          new PropertiesPropertyFetcher(extrasubsdict);


  Serverinfo() {
  }

  // dtnow needs to be fixed to a single date at the start of the tests just in case the tests
  // run over a day boundary.

  public String subs(final String subval) {
    return subs(subval, null);
  }

  public String subs(final String subval,
                     final PropertiesPropertyFetcher db) {
    var sub = subval;

    // Special handling for relative date-times
    var pos = sub.indexOf("$now.");
    while (pos != -1) {
      var endpos = sub.indexOf(":", pos);
      var subpos = sub.substring(pos);
      String value;

      DtParts dtp = getDtParts();

      if (subpos.startsWith("$now.year.")) {
        var yearoffset = ival(sub, pos + 10, endpos);
        value = String.format("%d", dtp.year + yearoffset);
      } else if (subpos.startsWith("$now.month.")) {
        var monthoffset = ival(sub, pos + 11, endpos);
        var month = dtp.month + monthoffset;
        var year = dtp.year + (int)(month - 1 / 12);
        month = ((month - 1) % 12) + 1;
        value = String.format("%d%02d", year, month);
      } else {
        final int dayOffset;
        if (sub.substring(pos).startsWith("$now.week.")) {
          var weekoffset = ival(sub, pos + 10, endpos);
          dayOffset = 7 * weekoffset;
        } else {
          dayOffset = ival(sub, pos + 5, endpos);
        }

        var offDtp = getDtParts(dayOffset);

        value = String.format("%d%02d%02d",
                              offDtp.year,
                              offDtp.month,
                              offDtp.dayOfMonth));
      }

      sub = String.format("%s%s%s", sub.substring(0, pos), value, sub.substring(endpos + 1));
      pos = sub.indexOf("$now.");
    }

    if (sub.contains("$uidrandom:")) {
      sub = sub.replace("$uidrandom:", UUID.randomUUID().toString());
    }

    final Utils.KeyValsPropertyFetcher pfetcher;

    if (db == null) {
      pfetcher = subsPfetcher;
    } else {
      pfetcher = db;
    }

    return propertyReplace(sub, pfetcher);
  }

  void addsubs(final KeyVals items,
               final KeyVals db) {
    final KeyVals dbActual;

    if (db == null) {
      dbActual = subsKvs;
    } else {
      dbActual = db;
    }

    for (var key: items.keySet()){
      dbActual.put(key, items.get(key));
    }

    if (db == null) {
      updateParams();
    }
  }

  public boolean hasextrasubs () {
    return extrasubsdict.size() > 0;
  }

  public String extrasubs(final String str) {
    return subs(str, extrasubsPfetcher);
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

        var els = value.split("/");

        value = els[els.length - 1];

        // urlpath() - extract just the URL path segment from the value
        processed.put(variable, value);
      } else if (variable.startsWith("urlpath(")) {
        variable = variable.substring("urlpath(".length(),
                                      variable.length() - 1);
        URL url = null;
        String value = items.getOnlyString(variable);
        try {
          url = new URL(value);
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
        value = url.getPath();
        processed.put(variable, value);
      } else {
        processed.put(variable, items.get(variable));
      }
    }

    addsubs(processed, extrasubsdict);
  }

  public List<KeyVal> newUIDs () {
    var res = new ArrayList<KeyVal>();

    for (int i = 1; i <= 21; i++) {
      var key = String.format("$uid%d:", i);
      var val = UUID.randomUUID().toString();
      subsKvs.put(key, val);
      extrasubsdict.put(key, val);
      res.add(new KeyVal(key, val));
    }

    return res;
  }

  public void parseXML(final Node node) {
    for (var child: children(node)) {
      var text = content(child);
      var textUtf8 = contentUtf8(child);
          
      if (nodeMatches(child, XmlDefs.ELEMENT_HOST)) {
        try {
          host = textUtf8;
        } catch (final Throwable t){
          host = "localhost";
        }
      } else if (nodeMatches(child, XmlDefs.ELEMENT_NONSSLPORT)) {
        nonsslport = Integer.valueOf(text);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SSLPORT)) {
        sslport = Integer.valueOf(text);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_UNIX)) {
        afunix = text;
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
      } else if (nodeMatches(child, XmlDefs.ELEMENT_AUTHTYPE)) {
        authtype = textUtf8;
      } else if (nodeMatches(child, XmlDefs.ELEMENT_CERTDIR)) {
        certdir = textUtf8;
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITCOUNT)) {
        waitcount = Integer.valueOf(textUtf8);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITDELAY)) {
        waitdelay = Float.valueOf(textUtf8);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_WAITSUCCESS)) {
        waitsuccess = Integer.valueOf(text);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_FEATURES)) {
        parseFeatures(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTIONS)) {
        parseSubstitutionsXML(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_CALENDARDATAFILTER)) {
        calendardatafilters.add(textUtf8);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_ADDRESSDATAFILTER)) {
        addressdatafilters.add(textUtf8);
      }

      updateParams();
    }
  }

  public void parseFeatures (final Node node) {
    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_FEATURE)) {
        features.add(contentUtf8(child));
      }
    }
  }

  public void updateParams () {
    // Expand substitutions fully at this point
    for (var key: subsKvs.keySet()) {
      subsKvs.put(key, propertyReplace(subsKvs.getOnlyString((String)key), subsPfetcher));
    }

    // Now cache some useful substitutions
    String user;
    if (subsKvs.containsKey("$userid1:")) {
      user = "$userid1:";
    } else {
      user = "$userid01:";
    }

    String pswd;
    if (subsKvs.containsKey("$pswd1:")) {
      pswd = "$pswd1:";
    } else {
      pswd = "$pswd01:";
    }

    user = subsKvs.getOnlyString(user);
    if (user == null) {
      throw new RuntimeException("Must have userid substitution");
    }

    pswd = subsKvs.getOnlyString(pswd);
    if (pswd == null) {
      throw new RuntimeException("Must have pswd substitution");
    }
  }

  public void parseRepeatXML(final Node node){
    // Look for count
    var count = XmlUtil.numAttrs(node);

    for (var child: children(node)) {
      parseSubstitutionXML(child, count);
    }
  }

  public void parseSubstitutionsXML(final Node node){
    for (var child: children(node)) {
      if(nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTION)){
        parseSubstitutionXML(child, 0);
      }else if(nodeMatches(child, XmlDefs.ELEMENT_REPEAT)){
        parseRepeatXML(child);
      }
    }
  }

  public void parseSubstitutionXML(final Node node, final int repeat) {
    if (!nodeMatches(node, XmlDefs.ELEMENT_SUBSTITUTION)) {
      return;
    }

    String key = null;
    String value = null;
    for (var schild : children(node)) {
      if (nodeMatches(schild, XmlDefs.ELEMENT_KEY)) {
        key = contentUtf8(schild);
      } else if (nodeMatches(schild, XmlDefs.ELEMENT_VALUE)) {
        var str = contentUtf8(schild);

        if (str == null) {
          value = "";
        } else {
          value = contentUtf8(schild);
        }
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
                                       final Util.PropertyFetcher props) {
    if (val == null) {
      return null;
    }

    int pos = val.indexOf("$");

    if (pos < 0) {
      return val;
    }

    final StringBuilder sb = new StringBuilder(val.length());
    int segStart = 0;

    while (true) {
      if (pos > 0) {
        sb.append(val.substring(segStart, pos));
      }

      final int end = val.indexOf(":", pos);

      if (end < 0) {
        //No matching close. Just append rest and return.
        sb.append(val.substring(pos));
        break;
      }

      final String pval = props.get(val.substring(pos + 2, end).trim());

      if (pval != null) {
        sb.append(pval);
      }

      segStart = end + 1;
      if (segStart > val.length()) {
        break;
      }

      pos = val.indexOf(":", segStart);

      if (pos < 0) {
        //Done.
        sb.append(val.substring(segStart));
        break;
      }
    }

    return sb.toString();
  }

  public static int ival(final String val, final int start, final int end) {
    return Integer.valueOf(val.substring(start, end));
  }
}
