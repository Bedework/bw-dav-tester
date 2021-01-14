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
import org.bedework.util.misc.response.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getQName;
import static org.bedework.util.xml.XmlUtil.hasContent;

/**
 * Verifier that checks the response body for an exact match to data in a file.
 */
public class XmlElementMatch extends Verifier {
  @Override
  public VerifyResult verify(final String ruri,
                             final List<Header> responseHeaders,
                             final int status,
                             final String respdata,
                             final KeyVals args) {
    // Get arguments
    var parent = args.getOnlyString("parent");
    var exists = args.getStrings("exists");
    var notexists = args.getStrings("notexists");

    // status code must be 200, 207
    if ((status != 200) && (status != 207)){
      fmsg("        HTTP Status Code Wrong: %d", status);
      return result;
    }

    // look for response data
    if (StringUtils.isEmpty(respdata)) {
      append("        No response body");
      return result;
    }

    // Read in XML
    if (!parseXml(respdata)) {
      fmsg("        Response data is not xml data: %s",
           respdata);
      return result;
    }

    final Element root;
    
    if (parent != null) {
      var nodes = nodesForPath(docRoot, parent);
      if (Util.isEmpty(nodes)) {
        fmsg("        Response data is missing parent node: %s",
             parent);
        return result;
      }
      
      if (nodes.size() > 1) {
        fmsg("        Response data has too many parent nodes: %s",
             parent);
        return result;
      }
      
      root = nodes.get(0);
    } else {
      root = docRoot;
    }

    for (var path: exists) {
      matchNode(root, path, null, null, true);
    }

    for (var path: notexists) {
      matchNode(root, path, null, null, false);
    }

    return result;
  }

  private final static Pattern rootPathsPattern =
          Pattern.compile("(\\{[^}]+}[^/]+)(.*)");

  private final static Pattern pathsPattern =
          Pattern.compile("(/?\\{[^}]+}[^/]+|\\.)(.*)");

  /** Only used to locate parent
   *
   * @param root where we start
   * @param path path with no tests
   * @return matching nodes.
   */
  private List<Element> nodesForPath(final Element root,
                                     final String path) {
    String actualPath;
    String tests;

    if (path.contains("[")) {
      var splits = path.split("\\[", 2);
      actualPath = splits[0];
      tests = splits[1];
    } else {
      actualPath = path;
      tests = null;
    }

    // Handle absolute root element
    if (path.startsWith("/")) {
      actualPath = actualPath.substring(1);
    }

    var m = rootPathsPattern.matcher(actualPath);
    List<Element> nodes = new ArrayList<>();

    if (m.matches() && !StringUtils.isEmpty(m.group(2))) {
      var rootPath = m.group(1);
      var childPath = m.group(2).substring(1);

      if (!rootPath.equals(".") &&
          !getQName(root).toString().equals(rootPath)) {
        return Collections.emptyList();
      }

      for (var child: children(root)) {
        final List<Element> ns = nodesForPath(child, childPath);
        if (!Util.isEmpty(ns)) {
          nodes.addAll(ns);
        }
      }
    } else if (getQName(root).toString().equals(actualPath)) {
      nodes.add(root);
    }

    if (tests == null) {
      return nodes;
    }

    final List<Element> res = new ArrayList<>();

    var split = tests.split("\\[");

    for (var t1: split) {
      var test = t1.substring(0, t1.length() - 1);
      for (var node: nodes) {
        if (testNode(node, path, test).isOk()) {
          res.add(node);
        }
      }
    }

    return res;
  }

  private boolean matchNode(final Element rootEl,
                            final String xpath,
                            Map<Element, Element> parentMapPar,
                            final String theTitle,
                            final boolean exists) {
    var root = rootEl;
    String title;

    if (theTitle == null) {
      title = xpath;
    } else {
      title = theTitle;
    }

    String actualXpath;
    String tests;

    // Find the first test in the xpath
    if (xpath.contains("[")) {
      var splits = xpath.split("\\[", 2);
      actualXpath = splits[0];
      tests = splits[1];
    } else {
      actualXpath = xpath;
      tests = null;
    }

    final Map<Element, Element> parentMap;

    if (parentMapPar == null) {
      parentMap = new HashMap<>();

      for (var ch: children(root)) {
        parentMap.put(ch, root);
      }
    } else {
      parentMap = parentMapPar;
    }

    // Handle parents
    if (actualXpath.startsWith("../")) {
      root = parentMap.get(root);
      actualXpath = "./" + actualXpath.substring(3);
    }

    // Handle absolute root element and find all matching nodes
    var m = pathsPattern.matcher(actualXpath);

    //List<Element> nodes = nodesForPath(root, actualXpath);
    List<Element> nodes = new ArrayList<>();

    /*
    if (m.matches() && m.group(2) != null) {
      var rootPath = Util.buildPath(false, m.group(1));
      if (rootPath.startsWith("/")) {
        rootPath = rootPath.substring(1);
      }
      var childPath = m.group(2).substring(1);
      if (!rootPath.equals(".") &&
              (!getQName(root).toString().equals(rootPath))) {
        fmsg("        Items not returned in XML for %s\n",
             title);
        return false;
      }
      nodes = findAll(root, childPath);
    } else {
      nodes = Collections.singletonList(root);
    }
     */

    if (m.matches() && !StringUtils.isEmpty(m.group(2))) {
      var rootPath = m.group(1);
      if (rootPath.startsWith("/")) {
        rootPath = rootPath.substring(1);
      }

      var childPath = m.group(2).substring(1);

      if (!rootPath.equals(".") &&
              !getQName(root).toString().equals(rootPath)) {
        return false;
      }

      for (var child: children(root)) {
        final List<Element> ns = nodesForPath(child, childPath);
        if (!Util.isEmpty(ns)) {
          nodes.addAll(ns);
        }
      }
    } else {
      nodes.add(root);
    }

    if (nodes.size() == 0) {
      if (exists) {
        fmsg("        Items not returned in XML for %s\n",
             title);
        return false;
      }
      return result.ok;
    }


    if (tests == null) {
      if (!exists) {
        fmsg("        Items returned in XML for %s\n", title);
        return false;
      }
      return result.ok;
    }

    // Split the tests into tests plus additional path
    final String nodeTestsSeg;
    final String nextPath;
    var pos = tests.indexOf("]/");
    if (pos != -1) {
      nodeTestsSeg = tests.substring(0, pos + 1);
      nextPath = tests.substring(pos + 1);
    } else {
      nodeTestsSeg = tests;
      nextPath = null;
    }

    var split = nodeTestsSeg.split("\\[");

    for (var t1: split) {
      var test = t1.substring(0, t1.length() - 1);
      Response resp = null;

      for (var node: nodes) {
        resp = testNode(node, title, test);
        if (!resp.isOk()) {
          continue;
        }

        // Found a match
        if (nextPath == null) {
          break;
        }

        matchNode(node, nextPath.substring(1), parentMap, title, exists);
        break;
      }

      if ((resp != null) && !resp.isOk()) {
        fmsg(resp.getMessage());
        break;
      }
    }

    return result.ok;
  }

  private String contentFor(final Node n) {
    if (!hasContent(n)) {
      return null;
    }

    var nodeText = content(n);
    if (StringUtils.isEmpty(nodeText)) {
      return null;
    }

    return nodeText;
  }

  private Response testNode(final Element node,
                            final String nodePath,
                            final String testPar) {
    var resp = new Response();

    if ((testPar == null) || (testPar.length() < 2)) {
      return Response.error(resp,
                            String.format("        Bad test %s\n",
                                          testPar));
    }

    var test = testPar.substring(1);

    switch (testPar.charAt(0)) {
      case '@':
        String attr;
        String value;

        if (test.contains("=")) {
          var split = test.split("=", 2);
          attr = split[0];
          value = split[1];
          value = value.substring(1, value.length() - 1);
        } else {
          attr = test;
          value = null;
        }

        if (!node.hasAttribute(attr)) {
          return Response.error(
                  resp,
                  String.format("        Missing attribute returned in XML for %s\n",
                                nodePath));
        }

        if ((value != null) && !node.getAttribute(attr).equals(value)) {
          return Response.error(
                  resp,
                  String.format("        Incorrect attribute value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '=':
        if (!test.equals(contentFor(node))) {
          return Response.error(
                  resp,
                  String.format("        Incorrect value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '!':
        if (test.equals(contentFor(node))) {
          return Response.error(
                  resp,
                  String.format("        Incorrect value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '*':
        var n1 = contentFor(node);
        if ((n1 == null) || !n1.contains(test)) {
          return Response.error(
                  resp,
                  String.format("        Incorrect value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '$':
        var n2 = contentFor(node);
        if ((n2 == null) || n2.contains(test)) {
          return Response.error(
                  resp,
                  String.format("        Incorrect value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '+':
        var n3 = contentFor(node);
        if ((n3 == null) || (!n3.startsWith(test))) {
          return Response.error(
                  resp,
                  String.format("        Incorrect value returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '^':
        String element;
        String elval;

        if (test.contains("=")) {
          var split = test.split("=", 2);
          element = split[0];
          elval = split[1];
        } else {
          element = test;
          elval = null;
        }

        var found = false;
        for (var child: children(node)) {
          if (!getQName(child).toString().equals(element)) {
            continue;
          }

          if ((elval == null) ||
                  (elval.equals(contentFor(child)))) {
            found = true;
            break;
          }
        }

        if (!found) {
          return Response.error(
                  resp,
                  String.format("        Missing child returned in XML for %s\n",
                                nodePath));
        }
        break;

      case '|':
        var n4 = contentFor(node);
        if ((test.length() == 1) && (test.equals("|"))) {
          if ((n4 == null) && Util.isEmpty(children(node))) {
            return Response.error(
                    resp,
                    String.format("        Empty element returned in XML for %s\n",
                                  nodePath));
          }
        } else {
          if ((n4 != null) || !Util.isEmpty(children(node))) {
            return Response.error(
                    resp,
                    String.format("        Non-empty element returned in XML for %s\n",
                                  nodePath));
          }
        }
        break;

      default:
        if (testPar.equals("icalendar")) {
          // Try to parse as iCalendar
          try {
            Icalendar.parseText(content(node));
          } catch (final Throwable t) {
            return Response.error(
                    resp,
                    String.format("        Incorrect value returned in iCalendar for %s\n",
                                  nodePath));
          }

          break;
        }

        if (testPar.equals("json")) {
          // Try to parse as JSON
          try {
            new ObjectMapper().readTree(content(node));
          } catch (final Throwable t) {
            return Response.error(
                    resp,
                    String.format("        Incorrect value returned in json for %s\n",
                                  nodePath));
          }

          break;
        }

        return Response.error(
                resp,
                String.format("        Bad test %s\n", testPar));
    }

    return Response.ok(resp);
  }
}

/*
# Tests
if __name__ == '__main__':
    xmldata = """
<D:test xmlns:D="DAV:">
    <D:a>A</D:a>
    <D:b>
        <D:c>C</D:c>
        <D:d>D</D:d>
    </D:b>
    <D:b>
        <D:c>C</D:c>
        <D:d>F</D:d>
    </D:b>
</D:test>
"""

    node = ElementTree(file=StringIO.StringIO(xmldata)).getroot()

    assert Verifier.matchNode(node, "/{DAV:}test/{DAV:}b/{DAV:}c[=C]/../{DAV:}d[=D]")[0]
    assert not Verifier.matchNode(node, "/{DAV:}test/{DAV:}b/{DAV:}c[=C]/../{DAV:}d[=E]")[0]
*/
