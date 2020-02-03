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

import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 * Base for many dav tester classes
 */
public abstract class DavTesterBase implements Logged {
  protected final Manager manager;

  protected Document doc;

  public String name;
  String description = "";

  protected boolean only;

  public boolean httpTrace = false;
  private Stack<Level> savedHttpLevels = new Stack<>();

  private Set<String> requireFeatures = new TreeSet<>();
  private Set<String> excludeFeatures = new TreeSet<>();

  private Map<String, String> defaultFiltersApplied = new HashMap<>();

  public DavTesterBase(final Manager manager) {
    this.manager = manager;
  }

  /**
   *
   * @return kind ffor labelling - e.g. TEST, REQUEST etc
   */
  public abstract String getKind();

  public boolean hasMissingFeatures() {
    return missingFeatures().size() > 0;
  }

  public TreeSet<String> missingFeatures() {
    var res = new TreeSet<>(requireFeatures);

    res.removeAll(manager.serverInfo.features);

    return res;
  }

  public boolean hasExcludedFeatures() {
    return excludedFeatures().size() > 0;
  }

  public TreeSet<String> excludedFeatures() {
    var res = new TreeSet<>(excludeFeatures);

    res.retainAll(manager.serverInfo.features);

    return res;
  }

  protected void parseFeatures(final Element node, final boolean require) {
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

  public void parseDefaultFiltersApplied(final Node node) {
    String callback = null;
    String name = null;

    for (var schild : children(node)) {
      if (nodeMatches(schild, XmlDefs.ELEMENT_CALLBACK)) {
        callback = contentUtf8(schild);
      } else if (nodeMatches(schild, XmlDefs.ELEMENT_NAME)) {
        var str = contentUtf8(schild);

        name = Objects.requireNonNullElse(str, "");
      }
    }

    if ((callback == null) || (name == null)) {
      return;
    }

    defaultFiltersApplied.put(callback, name);
  }

  public void applyDefaultFilters(final String callback,
                                  final KeyVals args) {
    final String name = defaultFiltersApplied.get(callback);

    if (name == null) {
      return;
    }

    final List<Object> defaults =
            manager.serverInfo.defaultFilters.get(name);

    if (Util.isEmpty(defaults)) {
      return;
    }

    args.addAll("filter", defaults);
  }

  protected void httpTraceOn() {
    savedHttpLevels.push(getLogLevel("org.apache.http"));
    setLogLevel("org.apache.http", Level.FINE);
  }

  protected void httpTraceOff() {
    if (!savedHttpLevels.empty()) {
      setLogLevel("org.apache.http", savedHttpLevels.pop());
    }
  }

  protected void print(String msg) {
    System.out.println(msg);
  }

  public void toStringSegment(final ToString ts) {
    ts.append(getKind(), name);
    ts.append("description", description);
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}