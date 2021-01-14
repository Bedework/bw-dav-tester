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
package org.bedework.davtester.request;

import org.bedework.davtester.DavTesterBase;
import org.bedework.davtester.KeyVals;
import org.bedework.davtester.Manager;
import org.bedework.davtester.XmlDefs;

import org.w3c.dom.Element;

import java.util.Date;
import java.util.Objects;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.fileToString;
import static org.bedework.davtester.Utils.getDtParts;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.Utils.uuid;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Represents the data/body portion of an HTTP request.
 */
public class Data extends DavTesterBase {
  public String contentType;
  String filepath;
  //Generator generator;
  String value;
  KeyVals substitutions = new KeyVals();
  boolean substitute;
  boolean generate;

  String nextpath;

  public Data(final Manager manager) {
    super(manager);
  }

  @Override
  public String getKind() {
    return "REQUEST.DATA";
  }

  @Override
  public void parseAttributes(final Element node) {
    super.parseAttributes(node);

    substitute = getYesNoAttributeValue(node,
                                        XmlDefs.ATTR_SUBSTITUTIONS,
                                        true);
    generate = getYesNoAttributeValue(node,
                                      XmlDefs.ATTR_GENERATE,
                                      false);
  }

  @Override
  public boolean xmlNode(final Element node) {
    if (nodeMatches(node, XmlDefs.ELEMENT_CONTENTTYPE)) {
      contentType = contentUtf8(node);
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_FILEPATH)) {
      filepath = manager.normResPath(contentUtf8(node)).toString();
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_GENERATOR)) {
      throwException("generator: unimplemented");

      /* GENERATOR
          generator = new generator(manager);
          generator.parseXML(child);
           */
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_SUBSTITUTE)) {
      parseSubstituteXML(node);
      return true;
    }

    return super.xmlNode(node);
  }

  public void parseSubstituteXML(final Element node) {
    String name = null;
    String value = null;
    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_NAME)) {
        name = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE)) {
        value = manager.serverInfo.subs(contentUtf8(child));
      }
      if ((name != null) && (value != null)) {
        substitutions.put(name, value);
      }
    }
  }

  public String getValue(final int count) {
    String dataStr = null;

    manager.serverInfo.addextrasubs(new KeyVals("$request_count:",
                                                String.valueOf(count)));

    if (value != null) {
      dataStr = value;
    } else if (filepath != null) {
      // read in the file data
      final String fname =
              Objects.requireNonNullElseGet(nextpath, () -> filepath);

      dataStr = fileToString(fname);
    }

    dataStr = manager.serverInfo.subs(dataStr);
    dataStr = manager.serverInfo.extrasubs(dataStr);

    if (!substitutions.isEmpty()) {
      dataStr = manager.serverInfo.subs(dataStr, substitutions);
    }

    if (generate) {
      if (contentType.startsWith("text/calendar")) {
        dataStr = generateCalendarData(dataStr, count);
      }
      //} else if (generator != null) {
      //dataStr = generator.doGenerate();
    }

    return dataStr;
  }

  private String generateCalendarData(final String dataVal,
                                      final int count) {
    // FIXME: does not work for events with recurrence overrides.

    // Change the following iCalendar data values:
    // DTSTART, DTEND, RECURRENCE-ID, UID

    // This was re.sub(...
    var data = dataVal.replaceAll("UID:.*", "UID:" + uuid());
    data = data.replaceAll("SUMMARY:(.*)", "SUMMARY:\\1 #" + count);

    var now = getDtParts(new Date());

    data = data.replaceAll("(DTSTART;[^:]*) [0-9]{8,8}",
                           format("\\1:%04d%02d%02d",
                                  now.year, now.month, now.dayOfMonth));

    data = data.replaceAll("(DTEND;[^:]*) [0-9]{8,8}",
                           format("\\1:%04d%02d%02d",
                                  now.year, now.month, now.dayOfMonth));

    return data;
  }
}
