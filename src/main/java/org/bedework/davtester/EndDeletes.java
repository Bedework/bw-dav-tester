/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.davtester.request.Request;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * User: mike Date: 3/4/20 Time: 23:01
 */
public class EndDeletes extends DavTesterBase {
  public static class RequestPars {
    final String uri;
    final Request req;

    public RequestPars(final String uri,
                       final Request req) {
      this.uri = uri;
      this.req = req;
    }

    public Request makeRequest(final String method,
                               final Manager manager) {
      var nreq = new Request(manager);

      nreq.method = method;

      nreq.scheme = req.scheme;
      nreq.host = req.host;
      nreq.port = req.port;

      nreq.ruris.add(uri);
      nreq.ruri = uri;
      if (req.getUser() != null) {
        nreq.setUser(req.getUser());
      }
      if (req.getPswd() != null) {
        nreq.setPswd(req.getPswd());
      }
      nreq.cert = req.cert;

      return nreq;
    }
  }

  private Map<String, RequestPars> deletions = new HashMap<>();

  public EndDeletes(final Manager manager) {
    super(manager);
  }

  @Override
  public String getKind() {
    return "END-DELETES";
  }

  public void add(final String uri,
                  final Request req) {
    if (deletions.containsKey(uri)) {
      return;
    }

    deletions.put(uri, new RequestPars(uri, req));
  }

  public void run() {
    if (deletions.isEmpty()) {
      return;
    }

    var label = format("%s | %s", manager.currentTestfile.name,
                       "END_DELETE");

    manager.trace("Start: Deleting Requests...");
    for (var delReq: deletions.values()) {
      var req = delReq.makeRequest("DELETE", manager);
      req.run(false, false, false, null, null, label, 0);
    }

    manager.trace(format("%s60%s", "End: Deleting Requests...", "[DONE]"));
  }
}
