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

import org.bedework.davtester.Manager;
import org.bedework.davtester.TestBase;
import org.bedework.davtester.Utils;
import org.bedework.davtester.XmlDefs;
import org.bedework.davtester.verifiers.Verifier;
import org.bedework.util.misc.Util;

import org.apache.http.HttpResponse;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
 *
 Defines how the result of a request should be verified. This is done
 by passing the response and response data to a callback with a set of arguments
 specified in the test XML config file. The callback name is in the XML config
 file also and is dynamically loaded to do the verification.
 */
class Verify extends TestBase {
  private String callback;
  private Properties args = new Properties();
  final static String verifierClassPrefix =
          "org.bedework.davtester.verifiers.";

  public Verify(final Manager manager) {
    super(manager);
  }

  public void doVerify(final String uri, final HttpResponse response,
                       Object respdata) {
    // Re-do substitutions from values generated during the current test run
    if (manager.serverInfo.hasextrasubs()) {
      for (var name: args.keySet()) {
        var values = (List<String>)args.get((String)name);
        var newvalues = new ArrayList<String>();
        for (var value: values) {
          newvalues.add(manager.serverInfo.extrasubs(value);
        }
        args.put(name, newvalues);
      }
    }

    var verifierClassName = verifierClassPrefix + Utils.upperFirst(callback);

    var verifier = (Verifier)Util.getObject(verifierClassName, Verifier.class);

    // Always clone the args as this verifier may be called multiple times
    var newargs = new Properties(args);

    return verifier.verify(manager, uri, response, respdata, newargs)
  }

  public void parseXML(final Element node) {
    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
        parseFeatures(child, true);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
        parseFeatures(child, false);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_CALLBACK)) {
        callback = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_ARG)) {
        parseArgXML(child);
      }
    }
  }

  public void parseArgXML(final Element node) {
    String name = null;
    List<String> values = new ArrayList<>();

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_NAME)) {
        name = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE)) {
        if (content(child) != null) {
          values.add(manager.serverInfo.subs(contentUtf8(child),
                                             null));
        } else {
          values.add("");
        }
      }
    }

    if (name != null) {
      args.put(name, values);
    }
  }
}