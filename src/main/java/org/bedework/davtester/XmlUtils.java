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

import org.bedework.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.bedework.davtester.Utils.throwException;
import static org.bedework.util.xml.XmlUtil.getAttrVal;

/**
XML processing utilities.
*/
public class XmlUtils {
  public static Document parseXml(final String fileName) {
    try {
      final File f = new File(fileName);

      final InputStream is = new FileInputStream(f);

      return parseXml(is);
    } catch (final Throwable t) {
      throwException(t);
      return null; // fake
    }
  }

  public static Document parseXml(final InputStream is) {
    try {
      final DocumentBuilderFactory factory =
              DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      return builder.parse(new InputSource(is));
    } catch (final Throwable t) {
      throwException(t);
      return null; // fake
    }
  }

  public static List<Element> children(Node nd) {
    try {
      return XmlUtil.getElements(nd);
    } catch (final Throwable t) {
      throwException(t);
      return null; // fake
    }
  }

  public static String content(Element nd) {
    try {
      return XmlUtil.getElementContent(nd);
    } catch (final Throwable t) {
      throwException(t);
      return null; // fake
    }
  }

  public static String contentUtf8(final Element nd) {
    try {
      var str = XmlUtil.getElementContent(nd);
      if (str == null) {
        return null;
      }
      return StandardCharsets.UTF_8.encode(str).toString();
    } catch (final Throwable t) {
      throwException(t);
      return null; // fake
    }
  }

  public static boolean getYesNoAttributeValue(final Element node,
                                        final String attr) {
    try {
      return XmlDefs.ATTR_VALUE_YES
              .equals(getAttrVal(node, attr));
    } catch (final Throwable t) {
      throwException(t);
      return false; // fake
    }
  }

  public static boolean getYesNoAttributeValue(final Element node,
                                               final String attr,
                                               final boolean def) {
    try {
      var val = getAttrVal(node, attr);
      if (val == null) {
        return def;
      }

      return XmlDefs.ATTR_VALUE_YES.equals(val);
    } catch (final Throwable t) {
      throwException(t);
      return false; // fake
    }
  }

  public static int getIntAttributeValue(final Element node,
                                         final String attr,
                                         final int def) {
    try {
      var val = getAttrVal(node, attr);
      if (val == null) {
        return def;
      }

      return Integer.valueOf(val);
    } catch (final Throwable t) {
      throwException(t);
      return 0; // fake
    }
  }
}
/*
public void readStringElementList(node, ename) {

    results = []
    for (var child: children(node)) {
        if child.tag == ename:
            results.append(child.text.decode("utf-8"))
    return results



public void getDefaultAttributeValue(node, attr, default) {
    result = node.getAttribute(attr)
    if result:
        return result
    } else {
        return default


public void readOneStringElement(node, ename) {

    for (var child: children(node)) {
        if child.tag == ename:
            return child.text.decode("utf-8")
    return ""


public void nodeForPath(root, path) {
    if '[' in path:
        actual_path, tests = path.split('[', 1)
    } else {
        actual_path = path
        tests = null

    # Handle absolute root element
    if actual_path[0] == '/':
        actual_path = actual_path[1:]
    if '/' in actual_path:
        root_path, child_path = actual_path.split('/', 1)
        if root.tag != root_path:
            return null
        nodes = root.findall(child_path)
    } else {
        root_path = actual_path
        child_path = null
        nodes = (root,)

    if len(nodes) == 0:
        return null

    results = []

    if tests:
        tests = [item[:-1] for item in tests.split('[')]
        for test in tests:
            for node in nodes:
                if test[0] == '@':
                    if '=' in test:
                        attr, value = test[1:].split('=')
                        value = value[1:-1]
                    } else {
                        attr = test[1:]
                        value = null
                    if attr in node.keys() && (value == null|| node.get(attr) == value) {
                        results.append(node)
                } else if (test[0] == '=':
                    if node.text == test[1:]:
                        results.append(node)
                } else if (test[0] == '!':
                    if node.text != test[1:]:
                        results.append(node)
                } else if (test[0] == '*':
                    if node.text != null && node.text.find(test[1:]) != -1:
                        results.append(node)
                } else if (test[0] == '+':
                    if node.text != null && node.text.startsWith(test[1:]) {
                        results.append(node)
                } else if (test[0] == '^':
                    if "=" in test:
                        element, value = test[1:].split("=", 1)
                    } else {
                        element = test[1:]
                        value = null
                    for (var child: children(node)) {
                        if child.tag == element && (value == null|| child.text == value) {
                            results.append(node)
    } else {
        results = nodes

    return results


public void xmlPathSplit(xpath) {

    pos = xpath.find("}")
    if pos == -1:
        return xpath, ""
    pos = xpath[pos:].find("/") + pos
    if pos == -1:
        return xpath, ""
    } else {
        return xpath[:pos], xpath[pos + 1:]
*/