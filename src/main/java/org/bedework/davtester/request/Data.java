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

import java.nio.file.Paths;

import static org.bedework.davtester.Utils.throwException;
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

  public void parseXML(final Element node) {
    substitute = getYesNoAttributeValue(node,
                                        XmlDefs.ATTR_SUBSTITUTIONS,
                                        true);
    generate = getYesNoAttributeValue(node,
                                      XmlDefs.ATTR_GENERATE,
                                      false);

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_CONTENTTYPE)) {
        contentType = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_FILEPATH)) {
        var fp = Paths.get(contentUtf8(child));
        filepath = manager.resDirPath.resolve(fp).toAbsolutePath().toString();

      } else if (nodeMatches(child, XmlDefs.ELEMENT_GENERATOR)) {
        throwException("generator: unimplemented");

          /* GENERATOR
          generator = new generator(manager);
          generator.parseXML(child);
           */
      } else if (nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTE)) {
        parseSubstituteXML(child);
      }
    }
  }

  public void parseSubstituteXML(final Element node) {
    String name = null;
    String value = null;
    for (var child : children(node)) {
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
}
