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

import org.bedework.util.dav.DavUtil;
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import static org.bedework.davtester.Utils.throwException;
import static org.bedework.util.xml.XmlUtil.getAttrVal;
import static org.bedework.util.xml.XmlUtil.hasChildren;
import static org.bedework.util.xml.XmlUtil.hasContent;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 XML processing utilities.
 */
public class XmlUtils {
  public static Path dtdPath;

  public static Document parseXml(final String fileName) {
    try {
      final File f = new File(fileName);

      final InputStream is = new FileInputStream(f);

      return parseXml(is);
    } catch (final FileNotFoundException fnf) {
      return null;
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static Document parseXmlString(final String val) {
    try {
      final InputStream is = new ByteArrayInputStream(val.getBytes(
              StandardCharsets.UTF_8));

      return parseXml(is);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static Document parseXml(final InputStream is) {
    try {
      final DocumentBuilderFactory factory =
              DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      final DocumentBuilder builder = factory.newDocumentBuilder();

      builder.setEntityResolver((publicId, systemId) -> {
        Path s = Paths.get(systemId);
        final String sname = s.getFileName().toString();
        if (sname.equals("serverinfo.dtd") || sname.equals("caldavtest.dtd")) {
          // One of ours
          return new InputSource(dtdPath.resolve(sname).toAbsolutePath().toString());
        }
        // If no match, returning null makes process continue normally
        return null;
      });
/*
public class CopyrightResolver implements EntityResolver {
    public InputSource resolveEntity(String publicID, String systemID)
        throws SAXException {
    }
 */



      return builder.parse(new InputSource(is));
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static String normalizedString(final Node node) {
    if (node == null) {
      return "";
    }

    var sb = new StringBuilder();

    normalizedString(sb, node);

    return sb.toString();
  }

  public static void normalizedString(final StringBuilder sb,
                                      final Node node) {
    if (node == null) {
      return;
    }

    sb.append("<");
    var qn = new QName(node.getNamespaceURI(), node.getLocalName());
    sb.append(qn);

    var attrs = node.getAttributes();
    for (var i = 0; i < attrs.getLength(); i++) {
      var attr = attrs.item(i);
      var nm = attr.getLocalName();

      if ("xmlns".equals(nm)) {
        continue;
      }

      sb.append(" ");
      sb.append(attr.getLocalName());
      sb.append(" \"");
      sb.append(content(attr).strip());
      sb.append("\"");
    }
    sb.append(">");

    if (hasContent(node)) {
      sb.append(content(node).strip());
    } else if (hasChildren(node)) {
      for (var child : children(node)) {
        normalizedString(sb, child);
      }
    }

    sb.append("</");
    sb.append(qn);
    sb.append(">");
  }

  public static String docToString(final Document doc) {
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = tf.newTransformer();

      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(doc), new StreamResult(writer));
      return writer.getBuffer().toString();
    } catch (final Throwable t) {
      return throwException(t);
    }
  }
  public static Namespaces namespaces = new Namespaces();

  private static DavUtil davUtil = new DavUtil();

  public static MultiStatusResponse multiStatusResponse(
          final String val) {
    try {
      return davUtil.getMultiStatusResponse(val);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static MultiStatusResponse getExtMkcolResponse(
          final String val) {
    try {
      return davUtil.getExtMkcolResponse(val);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static List<Element> childrenMatching(final Node nd,
                                               final QName tag) {
    try {
      var children = XmlUtil.getElements(nd);
      var matched = new ArrayList<Element>();

      for (var ch : children) {
        if (nodeMatches(ch, tag)) {
          matched.add(ch);
        }
      }

      return matched;
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static List<Element> children(Node nd) {
    try {
      return XmlUtil.getElements(nd);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static QName getQName(final Element el) {
    return new QName(el.getNamespaceURI(), el.getLocalName());
  }

  public static String content(Node nd) {
    try {
      return XmlUtil.getElementContent(nd);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static int intVal(final Element nd) {
    try {
      var str = XmlUtil.getElementContent(nd);
      if (str == null) {
        return throwException("integer value expected for " + nd);
      }

      return Integer.parseInt(str);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static float floatVal(final Element nd) {
    try {
      var str = XmlUtil.getElementContent(nd);
      if (str == null) {
        return throwException("float value expected for " + nd);
      }

      return Float.parseFloat(str);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static String contentUtf8(final Element nd) {
    try {
      var str = XmlUtil.getElementContent(nd);
      if (str == null) {
        return null;
      }

      return Utils.encodeUtf8(str);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static String attr(final Element nd,
                            final String attr) {
    try {
      var str =  getAttrVal(nd, attr);
      if ((str == null) || (str.length() == 0)) {
        return null;
      }

      return str;
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static String attrUtf8(final Element nd,
                                final String attr) {
    try {
      var str = getAttrVal(nd, attr);
      if ((str == null) || (str.length() == 0)) {
        return null;
      }
      return Utils.encodeUtf8(str);
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public static boolean getYesNoAttributeValue(final Element node,
                                               final String attr) {
    try {
      return XmlDefs.ATTR_VALUE_YES
              .equals(getAttrVal(node, attr));
    } catch (final Throwable t) {
      return throwException(t);
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
      return throwException(t);
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

      return Integer.parseInt(val);
    } catch (final Throwable t) {
      return throwException(t);
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

  public static List<Element> nodeForPath(final Element root,
                                          final String path) {
    String actualPath;
    String tests;

    if (path.contains("[")) {
      var split = path.split("\\[", 2);
      actualPath = split[0];
      tests = split[1];
    } else {
      actualPath = path;
      tests = null;
    }

    // Handle absolute root element
    if (actualPath.startsWith("/")) {
      actualPath = actualPath.substring(1);
    }

    String rootPath;
    String childPath;
    List<Element> nodes;

    if (actualPath.contains("/")) {
      var split = path.split("/", 2);
      rootPath = split[0];
      childPath = split[1];
      if (!root.getTagName().equals(rootPath)) {
        return null;
      }
      nodes = root.findall(child_path);
    } else {
      rootPath = actualPath;
      childPath = null;
      nodes = Collections.singletonList(root);
    }

    if (nodes.size() == 0) {
      return null;
    }

    results = [];

    if (tests == null) {
      return nodes;
    }

    tests = [item[:-1] for item in tests.split("\\[")];

    for (var test: tests) {
      for (var node in nodes) {
        if (test[0] == "@") {
          if ("=" in test) {
            attr, value = test[1:].split("=")
            value = value[1:-1]
          } else {
            attr = test[1:]
            value = null
          }
          if attr in node.keys() && (value == null|| node.get(attr) == value) {
            results.append(node)
          }
        } else if (test[0] == "=") {
          if (node.text == test[1:]) {
            results.append(node)
          }
        } else if (test[0] == "!") {
          if (node.text != test[1:]) {
            results.append(node)
          }
        } else if (test[0] == "*") {
          if (node.text != null && node.text.find(test[1:]) != -1) {
            results.append(node)
          }
        } else if (test[0] == "+") {
          if (node.text != null && node.text.startsWith(test[1:]) {
            results.append(node)
          }
        } else if (test[0] == "^") {
          if ("=" in test){
            element, value = test[1:].
            split("=", 1);
          } else{
            element = test[1:];
            value = null;
          }
        }
        for (var child: children(node)) {
          if (child.tag == element && (value == null|| child.text == value)
          {
            results.append(node);
          }
        }
      }
    }

    return results;
  }

*/


  public static class XmlSplit {
    public String rootPath;
    public String childPath;

    XmlSplit(final String rootPath,
             final String childPath) {
      this.rootPath = rootPath;
      this.childPath = childPath;
    }
  }

  public static XmlSplit xmlPathSplit(final String xpath) {
    var pos = xpath.indexOf("}");
    if (pos == -1) {
      return new XmlSplit(xpath, null);
    }

    var npos = xpath.indexOf("/", pos);
    if (npos == -1) {
      return new XmlSplit(xpath, null);
    }

    return new XmlSplit(xpath.substring(0, npos),
                        xpath.substring(npos + 1));
  }

  public static String testPathToXpath(final String testPath) {
    var split = xmlPathSplit(testPath);

    // convert rootPath to xpath format

    var rp = split.rootPath;

    if (rp.startsWith("{")) {
      var pos = rp.indexOf("}");

      if (pos > 0) {
        var abbrev = namespaces.getOrAdd(rp.substring(1, pos));

        split.rootPath = abbrev + ":" + rp.substring(pos + 1);
      }
    }

    if (split.childPath == null) {
      return split.rootPath;
    }

    return split.rootPath + "/" + testPathToXpath(split.childPath);
  }

  public static List<Element> findNodes(final Document doc,
                                        final boolean atRoot,
                                        final String testPath) {
    var xp = testPathToXpath(testPath);

    if (!atRoot) {
      xp = "//" + xp;
    } else {
      xp = "/" + xp;
    }

    XPathFactory xpathfactory = XPathFactory.newInstance();
    XPath xpath = xpathfactory.newXPath();
    xpath.setNamespaceContext(namespaces.getResolver());

    try {
      XPathExpression expr = xpath.compile(xp);

      //Search XPath expression
      Object result = expr.evaluate(doc.getDocumentElement(),
                                    XPathConstants.NODESET);

      var nodes = (NodeList)result;
      var res = new ArrayList<Element>(nodes.getLength());

      for (int i = 0; i < nodes.getLength(); i++) {
        res.add((Element)nodes.item(i));
      }

      return res;
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  /*
  public static List<Element> findAll(final Element root,
                                      final String tag) {
    final QName qn = QName.valueOf(tag);
    final List<Element> res = new ArrayList<>();

    for (final var el: children(root)) {
      if (nodeMatches(el, qn)) {
        res.add(el);
      }
    }

    return res;
  }
   */
}