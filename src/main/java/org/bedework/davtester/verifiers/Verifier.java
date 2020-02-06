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
import org.bedework.davtester.Manager;
import org.bedework.davtester.XmlUtils;
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.diff.NodeDiff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.http.Header;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import static java.lang.String.format;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.multiStatusResponse;
import static org.bedework.davtester.XmlUtils.normalizedString;
import static org.bedework.util.xml.XmlUtil.hasChildren;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/** Base class for verifiers
 *
 * User: mike Date: 11/20/19 Time: 22:55
 */
public abstract class Verifier implements Logged {
  protected static class NameVal implements Comparable<NameVal> {
    final String name;
    final String val;
    final boolean match;

    protected NameVal(final String name,
                      final String val) {
      this(name, val, false);
    }

    protected NameVal(final String name,
                      final String val,
                      final boolean match) {
      this.name = name;
      this.val = val;
      this.match = match;
    }

    public boolean equals(final Object o) {
      if (!(o instanceof NameVal)) {
        return false;
      }

      var that = (NameVal)o;

      return (Util.cmpObjval(name, that.name) == 0) &&
              (Util.cmpObjval(val, that.val) == 0);
    }

    @Override
    public int compareTo(final NameVal that) {
      var res = Util.cmpObjval(name, that.name);

      if (res != 0) {
        return res;
      }
      return Util.cmpObjval(val, that.val);
    }

    public String toString() {
      return "[" + name + ", " + val + "]";
    }
  }

  public static class VerifyResult {
    public boolean ok = true;
    public StringBuilder text = new StringBuilder();

    // An ok result
    public VerifyResult() {
    }

    // A not ok
    public VerifyResult(String text) {
      ok = false;
      this.text.append(text);
    }

    public String getText() {
      return text.toString();
    }

    public void append(final String val) {
      append(val, true);
    }

    public void nl() {
      ok = false;
      text.append("\n");
    }

    public void append(final String val,
                       final boolean addnl) {
      ok = false;
      if ((val == null) || (val.length() == 0)) {
        return;
      }

      if (addnl && (text.length() > 0)) {
        text.append("\n");
      }
      text.append(val);
    }

    public void appendOK(final String val,
                         final boolean addnl) {
      if ((val == null) || (val.length() == 0)) {
        return;
      }

      if (addnl && (text.length() > 0)) {
        text.append("\n");
      }
      text.append(val);
    }
  }

  protected Manager manager;

  // Per verify call
  protected Document doc;
  protected Element docRoot;
  protected VerifyResult result;

  public void init(final Manager manager) {
    this.manager = manager;
  }

  public boolean featureSupported(final String feature) {
    return manager.featureSupported(feature);
  }

  public VerifyResult doVerify(final String ruri,
                               final List<Header> responseHeaders,
                               final int status,
                               final String respdata,
                               final KeyVals args) {
    // Setup for call.
    doc = null;
    result = new VerifyResult();

    return verify(ruri, responseHeaders, status, respdata, args);
  }

  protected abstract VerifyResult verify(final String ruri,
                                         final List<Header> responseHeaders,
                                         final int status,
                                         final String respdata,
                                         final KeyVals args);

  protected void errorDiff(final String msg,
                           final String actual,
                           final String expected) {
    var actLines = Arrays.asList(actual.split("\n"));
    var expLines = Arrays.asList(expected.split("\n"));

    try {
      Patch<String> patch = DiffUtils.diff(expLines, actLines);
      var errorDiff = new StringBuilder();

      for (AbstractDelta<String> delta: patch.getDeltas()) {
        errorDiff.append(delta.toString());
        errorDiff.append('\n');
      }

      fmsg(msg, errorDiff);
    } catch (final Throwable t) {
      fmsg("        Unable to diff data and response: %s",
           t.getMessage());
    }
  }

  protected void errorDiff(final List<NodeDiff.DiffNode> diffs) {
    var errorDiff = new StringBuilder();

    for (var dn: diffs) {
      errorDiff.append(dn.toString());
      errorDiff.append("\n\n");
    }

    fmsg("        Response data does not " +
                 "exactly match file data: \n%s",
         errorDiff);
  }

  protected boolean parseXml(final String str) {
    try {
      doc = XmlUtils.parseXmlString(str);
      docRoot = doc.getDocumentElement();
      return true;
    } catch (final Throwable t) {
      fmsg("           HTTP response is not valid XML: %s\n", str);
      return false;
    }
  }

  protected String normalizeXMLDataToString(final String data) {
    var xml = XmlUtils.docToString(normalizeXMLData(data, null));
    var pos = xml.indexOf('>');
    return xml.substring(pos + 1);
  }

  protected Document normalizeXMLData(final String data,
                                      final List<String> filters) {
    var doc = XmlUtils.parseXmlString(data);
    var root = doc.getDocumentElement();

    if (!Util.isEmpty(filters)) {
      // Apply filters
      for (var filterVal: filters) {
        var fvals = filterVal.split(",");

        for (var filter: fvals) {
          var qn = QName.valueOf(filter);
          removeElement(root, qn);
          /*
          var nl = root.getElementsByTagNameNS(qn.getNamespaceURI(),
                                               qn.getLocalPart());
          for (var i = 0; i < nl.getLength(); i++) {
            var node = nl.item(i);

            XmlUtil.clear(node);
          }
           */
        }
      }
    }

    return doc;
  }

  private boolean removeElement(final Element root,
                                final QName qn) {
    if (!hasChildren(root)) {
      return false;
    }

    boolean res = false;
    boolean removed;
    do {
      removed = false;

      for (final var node: children(root)) {
        if (nodeMatches(node, qn)) {
          root.removeChild(node);
          removed = true;
          break;
        }

        if (removeElement(node, qn)) {
          removed = true;
          break;
        }
      }

      if (removed) {
        res = true;
      }
    } while (removed);

    return res;
  }

  protected String normalizeXML(final String val) {
    if (!val.startsWith("<")) {
      return val;
    }

    var doc = normalizeXMLData(val, null);
    return normalizedString(doc.getDocumentElement());
  }

  protected void fmsg(final String fmt,
                      final Object... args) {
    result.append(format(fmt, args));
  }

  protected void append(final String val) {
    result.append(val);
  }

  protected void append(final String val,
                        final boolean addnl) {
    result.append(val, addnl);
  }

  protected void badHrefs(final String msg,
                          final List<?> hrefs,
                          final Object... args) {
    fmsg(msg, hrefs.size(), args);
    for (var i: hrefs) {
      append(" " + i, false);
    }
    nl();
  }

  protected void nl() {
    result.nl();
  }

  protected MultiStatusResponse getMultiStatusResponse(final String data) {
    try {
      return multiStatusResponse(data);
    } catch (final Throwable t) {
      result.append(format(
              "Bad multi-status response. Message was %s\n" +
                      "Data was %s", t.getMessage(), data));
      return null;
    }
  }

  protected MultiStatusResponse getExtMkcolResponse(final String data) {
    try {
      return XmlUtils.getExtMkcolResponse(data);
    } catch (final Throwable t) {
      result.append(format(
              "Bad multi-staus response. Message was %s\n" +
                      "Data was %s", t.getMessage(), data));
      return null;
    }
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
