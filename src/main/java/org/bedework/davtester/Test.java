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

import org.bedework.davtester.request.Request;
import org.bedework.util.misc.ToString;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static org.bedework.davtester.XmlUtils.attr;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.getIntAttributeValue;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 *
 A single test which can be comprised of multiple requests. The test can
 be run more than once, and timing information gathered and averaged across
 all runs.
 */
class Test extends DavTesterBase {
  boolean details;
  boolean stats;
  boolean ignore;
  int count = 1;

  List<Request> requests = new ArrayList<>();

  public Test(final Manager manager) {
    super(manager);
  }

  @Override
  public String getKind() {
    return "TEST";
  }

  public void parseXML(final Element node) {
    name = attr(node, XmlDefs.ATTR_NAME);
    details = getYesNoAttributeValue(node, XmlDefs.ATTR_DETAILS);
    count = getIntAttributeValue(node, XmlDefs.ATTR_COUNT, 1);
    stats = getYesNoAttributeValue(node, XmlDefs.ATTR_STATS);
    ignore = getYesNoAttributeValue(node, XmlDefs.ATTR_IGNORE);
    only = getYesNoAttributeValue(node, XmlDefs.ATTR_ONLY);
    httpTrace = getYesNoAttributeValue(node, XmlDefs.ATTR_HTTP_TRACE,
                                       false);

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
        parseFeatures(child, true);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
        parseFeatures(child, false);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_DESCRIPTION)) {
        description = content(child);
      }
    }

    // get request
    requests = Request.parseList(manager, node);
  }

  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    //for (var req: requests) {
    //  req.dump();
    //}
  }
}
