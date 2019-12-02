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

import org.apache.http.message.BasicHeader;

/**
 * User: mike Date: 11/24/19 Time: 16:27
 */
public class UriIdPw {
  final String ruri;
  final String user;
  final String pswd;

  UriIdPw(final String ruri,
          final String user,
          final String pswd) {
    this.ruri = ruri;
    this.user = user;
    this.pswd = pswd;
  }

  void setRequest(final Request req) {
    req.ruris.add(ruri);
    req.ruri = ruri;

    if (user != null) {
      req.setUser(user);
    }
    if (pswd != null) {
      req.setPswd(pswd);
    }
  }

  static UriIdPw fromRequest(final Request req) {
    return new UriIdPw(req.ruri, req.getUser(), req.getPswd());
  }

  Request makeRequest(final Request originalRequest,
                      final String method) {
    return makeRequest(originalRequest, method, null);
  }

  Request makeRequest(final Request originalRequest,
                      final String method,
                      final String depth) {
    var req = new Request(originalRequest.manager);
    req.method = method;
    req.scheme = originalRequest.scheme;
    req.host = originalRequest.host;
    req.port = originalRequest.port;

    if (depth != null) {
      req.headers.add(new BasicHeader("Depth", depth));
    }

    setRequest(req);

    return req;
  }
}
