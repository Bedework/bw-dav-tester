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

import org.w3c.dom.Node;

import java.util.Set;
import java.util.TreeSet;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Base for test classes
 */
class TestBase {
  protected final Manager manager;

  private Set<String> requireFeatures = new TreeSet<>();
  private Set<String> excludeFeatures = new TreeSet<>();

  public TestBase(final Manager manager) {
    this.manager = manager;
  }

  public TreeSet<String> missingFeatures() {
    var res = new TreeSet<>(requireFeatures);

    res.removeAll(manager.serverInfo.features);

    return res;
  }

  public TreeSet<String> excludedFeatures () {
    var res = new TreeSet<>(excludeFeatures);

    res.retainAll(manager.serverInfo.features);

    return res;
  }


  protected void parseFeatures(final Node node, final boolean require) {
    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_FEATURE)) {
        if (require) {
          requireFeatures.add(contentUtf8(child));
        } else {
          excludeFeatures.add(contentUtf8(child));
        }
      }
    }
  }
}