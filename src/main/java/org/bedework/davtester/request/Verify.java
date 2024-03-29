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
import org.bedework.davtester.verifiers.AclItems;
import org.bedework.davtester.verifiers.AddressDataMatch;
import org.bedework.davtester.verifiers.DataMatch;
import org.bedework.davtester.verifiers.DataString;
import org.bedework.davtester.verifiers.FreeBusy;
import org.bedework.davtester.verifiers.IcalendarDataMatch;
import org.bedework.davtester.verifiers.MultistatusItems;
import org.bedework.davtester.verifiers.PostFreeBusy;
import org.bedework.davtester.verifiers.Prepostcondition;
import org.bedework.davtester.verifiers.PropfindItems;
import org.bedework.davtester.verifiers.PropfindValues;
import org.bedework.davtester.verifiers.StatusCode;
import org.bedework.davtester.verifiers.Verifier;
import org.bedework.davtester.verifiers.Verifier.VerifyResult;
import org.bedework.davtester.verifiers.XmlDataMatch;
import org.bedework.davtester.verifiers.XmlElementMatch;

import org.apache.http.Header;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bedework.davtester.Utils.throwException;
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
public class Verify extends DavTesterBase {
  private static final Map<String, Verifier> verifiers = new HashMap<>();

  static {
    addVerifier("addressDataMatch", new AddressDataMatch());
    addVerifier("aclItems", new AclItems());
    addVerifier("calendarDataMatch", new IcalendarDataMatch());
    addVerifier("dataMatch", new DataMatch());
    addVerifier("dataString", new DataString());
    addVerifier("freeBusy", new FreeBusy());
    addVerifier("header", new org.bedework.davtester.verifiers.Header());
    addVerifier("multistatusItems", new MultistatusItems());
    addVerifier("postFreeBusy", new PostFreeBusy());
    addVerifier("prepostcondition", new Prepostcondition());
    addVerifier("propfindItems", new PropfindItems());
    addVerifier("propfindValues", new PropfindValues());
    addVerifier("statusCode", new StatusCode());
    addVerifier("xmlDataMatch", new XmlDataMatch());
    addVerifier("xmlElementMatch", new XmlElementMatch());
  }

  private String callback;
  private final KeyVals args = new KeyVals();

  public Verify(final Manager manager) {
    super(manager);

    // Initialise the verifiers
    for (final var verifier: verifiers.values()) {
      verifier.init(manager);
    }
  }

  public VerifyResult doVerify(final String ruri,
                               final List<Header> responseHeaders,
                               final int status,
                               final String respdata) {

    // Re-do substitutions from values generated during the current test run
    if (manager.serverInfo.hasextrasubs()) {
      for (final var name: args.keySet()) {
        final var values = args.getStrings(name);
        final var newvalues = new ArrayList<>();
        for (final var value: values) {
          newvalues.add(manager.serverInfo.extrasubs(value));
        }
        args.put(name, newvalues);
      }
    }

    final var verifier = verifiers.get(callback);

    if (verifier == null) {
      return throwException("Unknown verifier: " + callback);
    }

    // Always clone the args as this verifier may be called multiple times
    final var newargs = new KeyVals(args);

    manager.currentTestfile.applyDefaultFilters(callback, newargs);

    return verifier.doVerify(ruri, responseHeaders, status, respdata, newargs);
  }

  @Override
  public String getKind() {
    return "VERIFY";
  }

  @Override
  public boolean xmlNode(final Element node) {
    if (nodeMatches(node, XmlDefs.ELEMENT_CALLBACK)) {
      callback = contentUtf8(node);
      name = callback;
      return true;
    }

    if (nodeMatches(node, XmlDefs.ELEMENT_ARG)) {
      parseArgXML(node);
      return true;
    }

    return super.xmlNode(node);
  }

  public void parseArgXML(final Element node) {
    String name = null;
    final List<Object> values = new ArrayList<>();

    for (final var child: children(node)) {
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

  private static void addVerifier(final String name, final Verifier verifier) {
    verifiers.put(name, verifier);
  }

  /* create a verifier from class name ...
    final static String verifierClassPrefix =
          "org.bedework.davtester.verifiers.";

    var verifierClassName = verifierClassPrefix + Utils.upperFirst(callback);

    var verifier = (Verifier)Util.getObject(verifierClassName, Verifier.class);

   */
}