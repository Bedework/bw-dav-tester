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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getQName;

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

    for (var path : notexists) {
      matchNode(root, path, null, null, false);
    }

    return result;
  }

  private final static Pattern rootPathsPattern =
          Pattern.compile("(\\{[^}]+}[^/]+)(.*)");

  private List<Element> parentNodesForPath(final Element root,
                                           final String path) {
    String actualPath;
    String tests;
    if (path.contains("[")) {
      var split = path.split("\\[", 2);
      actualPath = split[0];
      tests = split[1];
    } else{
      actualPath = path;
      tests = null;
    }

    final List<Element> nodes = nodesForPath(root, actualPath);
    if (Util.isEmpty(nodes)) {
      return nodes;
    }

    final List<Element> res = new ArrayList<>();

    if (tests != null) {
      var split = tests.split("\\[");

      for (var test: split) {
        for (var node : nodes) {
          if (testNode(node, path, test)) {
            res.add(node);
          }
        }
      }
    } else {
      res.addAll(nodes);
    }

    return res;
  }

  /**
   * @param root where we start
   * @param path path with no tests
   * @return matching nodes.
   */
  private List<Element> nodesForPath(final Element root,
                                     final String path) {
    String actualPath;

    // Handle absolute root element
    if (path.startsWith("/")) {
      actualPath = path.substring(1);
    } else {
      actualPath = path;
    }

    var m = rootPathsPattern.matcher(actualPath);
    List<Element> res = new ArrayList<>();

    if (m.matches() && !StringUtils.isEmpty(m.group(2))) {
      var rootPath = m.group(1);
      var childPath = m.group(2).substring(1);

      if (!rootPath.equals(".") &&
          !getQName(root).toString().equals(rootPath)) {
        return Collections.emptyList();
      }

      for (var child: children(root)) {
        final List<Element> nodes = nodesForPath(child, childPath);
        if (!Util.isEmpty(nodes)) {
          res.addAll(nodes);
        }
      }
    } else if (getQName(root).toString().equals(actualPath)) {
      res.add(root);
    }

    return res;
  }

  private final static Pattern pathsPattern =
          Pattern.compile("(/?\\{[^}]+}[^/]+|\\.)(.*)");

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

    List<Element> nodes = nodesForPath(root, actualXpath);

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
    var nodeTests = new ArrayList<String>();

    for (var s: split) {
      nodeTests.add(s.substring(0, s.length() - 1));
    }

    for (var test: nodeTests) {
      for (var node : nodes) {
        if (!testNode(node, title, test)) {
          return false;
        }
        if (nextPath == null) {
          break;
        }

        matchNode(node, nextPath.substring(1), parentMap, title, exists);
        break;
      }

      if (!result.ok) {
        break;
      }
    }

    return result.ok;
  }

  private boolean testNode(final Element node, 
                           final String nodePath,
                           final String testPar) {
    var nodeText = content(node); 
    if (StringUtils.isEmpty(nodeText)) {
      nodeText = null;
    }

    if ((testPar == null) || (testPar.length() < 2)) {
      fmsg("        Bad test %s\n",
           testPar);
      return false;
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
          fmsg("        Missing attribute returned in XML for %s\n",
               nodePath);
        }

        if ((value != null) && !node.getAttribute(attr).equals(value)) {
          fmsg("        Incorrect attribute value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '=':
        if ((nodeText == null) || !nodeText.equals(test)) {
          fmsg("        Incorrect value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '!':
        if ((nodeText != null) && nodeText.equals(test)) {
          fmsg("        Incorrect value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '*':
        if ((nodeText == null) || !nodeText.contains(test)) {
          fmsg("        Incorrect value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '$':
        if ((nodeText == null) || nodeText.contains(test)) {
          fmsg("        Incorrect value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '+':
        if ((nodeText == null) || (!nodeText.startsWith(test))) {
          fmsg("        Incorrect value returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

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
          if (getQName(child).toString().equals(element) &&
                  (elval == null) || (content(child).equals(elval))) {
            found = true;
            break;
          }
        }

        if (!found) {
          fmsg("        Missing child returned in XML for %s\n",
               nodePath);
        }
        return result.ok;

      case '|':
        if ((test.length() == 1) && (test.equals("|"))) {
          if ((nodeText == null) && Util.isEmpty(children(node))) {
            fmsg("        Empty element returned in XML for %s\n",
                 nodePath);
          }
        } else {
          if ((nodeText != null) || !Util.isEmpty(children(node))) {
            fmsg("        Non-empty element returned in XML for %s\n",
                 nodePath);
          }
        }
        return result.ok;

      default:
        if (testPar.equals("icalendar")) {
          // Try to parse as iCalendar
          try {
            Icalendar.parseText(content(node));
          } catch (final Throwable t) {
            fmsg("        Incorrect value returned in iCalendar for %s\n",
                 nodePath);
          }

          return result.ok;
        }

        if (testPar.equals("json")) {
          // Try to parse as JSON
          try {
            new ObjectMapper().readTree(content(node));
          } catch (final Throwable t) {
            fmsg("        Incorrect value returned in json for %s\n",
                 nodePath);
          }

          return result.ok;
        }
    }

    fmsg("        Bad test %s\n", testPar);
    return false;
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
