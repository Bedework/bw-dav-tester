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

import getYesNoAttributeValue;
import org.w3c.dom.Node;
import src.xmlDefs;
import test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.bedework.davtester.Serverinfo.*;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.getAttrVal;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Maintains a list of tests to run as part of a 'suite'.
 */
class Testsuite extends TestBase {
  private String name;
  private boolean ignore;
  private boolean only;
  private boolean changeuid;

  private List<Test> tests = new ArrayList<>();

  public Testsuite(final Manager manager) {
    super(manager);
  }

    public List<KeyVal> aboutToRun () {
        /*
        Typically we need the calendar/contact data for a test file to have a common set
        of UIDs, and for each overall test file to have unique UIDs. Occasionally, within
        a test file we also need test suites to have unique UIDs. The "change-uid" attribute
        can be used to reset the active UIDs for a test suite.
        */

      if (changeuid) {
        return manager.serverInfo.newUIDs();
      }

      return new ArrayList<>();
    }

    public void parseXML(final Node node) {
      name = getAttrVal(node, XmlDefs.ATTR_NAME);
      ignore = getYesNoAttributeValue(node, XmlDefs.ATTR_IGNORE);
      only = getYesNoAttributeValue(node, XmlDefs.ATTR_ONLY);
      changeuid = getYesNoAttributeValue(node,
                                         XmlDefs.ATTR_CHANGE_UID);

      for (var child : children(node)) {
        if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
          parseFeatures(child, true);
        } else if (nodeMatches(child,
                               XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
          parseFeatures(child, false)
        } else if (nodeMatches(child, XmlDefs.ELEMENT_TEST)) {
          t = new Test(manager);
          t.parseXML(child);
          tests.append(t);
        }
      }
    }

  public void dump () {
    print "\nTest Suite:"
    print "    name: %s" % name
    for (test:
         tests) {
      test.dump();
    }
  }
