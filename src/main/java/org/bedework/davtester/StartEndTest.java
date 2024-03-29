/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.davtester.request.Request;
import org.bedework.util.misc.Util;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

/**
 * User: mike Date: 3/4/20 Time: 22:33
 */
public class StartEndTest extends DavTesterBase {
  private List<Request> requests = new ArrayList<>();
  private final boolean start;

  public StartEndTest(final Manager manager,
                      final boolean start) {
    super(manager);

    this.start = start;
  }

  @Override
  public String getKind() {
    return "START-END-TEST";
  }

  @Override
  public boolean xmlNode(final Element node) {
    var req = Request.checkNode(node, manager);
    if (req != null) {
      requests.add(req);
      return true;
    }

    return super.xmlNode(node);
  }

  public boolean run() {
    if (Util.isEmpty(requests)) {
      return true;
    }

    var result = true;
    String startEndDesc;
    String label;
    var name = manager.currentTestfile.name;

    if (start) {
      startEndDesc = "Start";
      label = format("%s | %s", name, "START_REQUESTS");
    } else {
      startEndDesc = "End";
      label = format("%s | %s", name, "END_REQUESTS");
    }

    manager.trace(format("Start: %s Requests...", startEndDesc));

    var reqCount = 1;
    StringBuilder resulttxt = new StringBuilder();

    for (final var req: requests) {
      final var resreq = req.run(false,
                                 false, // doverify,
                                 start, // forceverify,
                                 null, // stats
                                 null, // etags
                                 format("%s | #%s", label, reqCount),
                                 1);  // count
      if (resreq.message != null) {
        resulttxt.append(resreq.message);
      }

      if (!resreq.ok &&
              (!req.method.equals("DELETE") ||
                       (resreq.status != HttpServletResponse.SC_NOT_FOUND))) {
        resulttxt.append(format(
                "\nFailure during multiple requests " +
                        "#%d out of %d, request=%s",
                reqCount, requests.size(),
                req));
        result = false;
        break;
      }

      reqCount++;
    }

    final String s;
    if (result) {
      s = "[OK]";
    } else {
      s = "[FAILED]";
      manager.logit(resulttxt.toString());
    }

    manager.trace(format("%s60%s5",
                         "End: " + startEndDesc + " Requests...",
                         s));

    if (resulttxt.length() > 0) {
      manager.trace(resulttxt.toString());
    }

    return result;
  }
}
