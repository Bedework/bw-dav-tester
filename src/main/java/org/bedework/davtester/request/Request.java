package org.bedework.davtester.request;

import org.bedework.davtester.DavTesterBase;
import org.bedework.davtester.Manager;
import org.bedework.davtester.XmlDefs;

import org.w3c.dom.Element;

import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/*
algorithms = {
    'md5': md5,
    'md5-sess': md5,
    'sha': sha1,
}
*/

/*
# DigestCalcHA1


public void calcHA1(
    pszAlg,
    pszUserName,
    pszRealm,
    pszPassword,
    pszNonce,
    pszCNonce,
    preHA1=null
) {
    """
    @param pszAlg: The name of the algorithm to use to calculate the digest.
        Currently supported are md5 md5-sess and sha.

    @param pszUserName: The username
    @param pszRealm: The realm
    @param pszPassword: The password
    @param pszNonce: The nonce
    @param pszCNonce: The cnonce

    @param preHA1: If available this is a str containing a previously
       calculated HA1 as a hex string. If this is given then the values for
       pszUserName, pszRealm, and pszPassword are ignored.
    """

    if (preHA1 && pszUserName|| pszRealm or pszPassword)) {
        raise TypeError(("preHA1 is incompatible with the pszUserName, "
                         "pszRealm, and pszPassword arguments"))

    if preHA1 == null:
        # We need to calculate the HA1 from the username:realm:password
        m = algorithms[pszAlg]()
        m.update(pszUserName)
        m.update(":")
        m.update(pszRealm)
        m.update(":")
        m.update(pszPassword)
        HA1 = m.digest()
    } else {
        # We were given a username:realm:password
        HA1 = preHA1.decode('hex')

    if pszAlg == "md5-sess":
        m = algorithms[pszAlg]()
        m.update(HA1)
        m.update(":")
        m.update(pszNonce)
        m.update(":")
        m.update(pszCNonce)
        HA1 = m.digest()

    return HA1.encode('hex')
*/
/*
# DigestCalcResponse
public void calcResponse(
    HA1,
    algo,
    pszNonce,
    pszNonceCount,
    pszCNonce,
    pszQop,
    pszMethod,
    pszDigestUri,
    pszHEntity,
) {
    m = algorithms[algo]()
    m.update(pszMethod)
    m.update(":")
    m.update(pszDigestUri)
    if pszQop == "auth-int":
        m.update(":")
        m.update(pszHEntity)
    HA2 = m.digest().encode('hex')

    m = algorithms[algo]()
    m.update(HA1)
    m.update(":")
    m.update(pszNonce)
    m.update(":")
    if pszNonceCount && pszCNonce && pszQop:
        m.update(pszNonceCount)
        m.update(":")
        m.update(pszCNonce)
        m.update(":")
        m.update(pszQop)
        m.update(":")
    m.update(HA2)
    respHash = m.digest().encode('hex')
    return respHash
*/

//class pause  {
//    pass
//

/**
 * Represents the HTTP request to be executed, and verification information to
 * be used to determine a satisfactory output or not.
 */
class Request extends DavTesterBase {
  private String host;
  private int port;
  private String afunix;

  private boolean auth = true;
  private String user;
  private String pswd;
  private String cert;
  private boolean endDelete;
  private boolean printRequest;
  private boolean printResponse;
  private boolean waitForSuccess;

  method = ""
  headers = {}
  ruris = []
  ruri = ""
  ruri_quote = true
  private Data data = null
  iterate_data = false
  count = 1
  verifiers = []
  graburi = null
  grabcount = null
  grabheader = []
  grabproperty = []
  grabelement = []
  grabjson = []
  grabcalprop = []
  grabcalparam = []

  //nc = {}  // Keep track of nonce count

    public Request(final Manager manager) {
      super(manager);
      host = manager.serverInfo.host
      port = manager.serverInfo.port
      afunix = manager.serverInfo.afunix
    }

    public void __str__ () {
      return "Method: %s; uris: %s" % (method,ruris if len(ruris) > 1  else
      ruri,)
    }

    public void getURI (si) {
        uri = si.extrasubs(ruri)
        if "**" in uri:
            if "?" not in uri|| uri.find("?") > uri.find("**") {
                uri = uri.replace("**", String.valueOf(uuid.uuid4()))
        } else if ("##" in uri:
            if "?" not in uri|| uri.find("?") > uri.find("##") {
                uri = uri.replace("##", String.valueOf(count))
        return uri

    public void getHeaders (si) {
        hdrs = headers
        for key, value in hdrs.items() {
            hdrs[key] = si.extrasubs(value)

        # Content type
        if data != null:
            hdrs["Content-Type"] = data.content_type

        # Auth
        if auth:
            if si.authtype.lower() == "basic":
                hdrs["Authorization"] = gethttpbasicauth(si)
            } else if (si.authtype.lower() == "digest":
                hdrs["Authorization"] = gethttpdigestauth(si)

        return hdrs

    public void gethttpbasicauth (si) {
        basicauth = [user, si.user][user == ""]
        basicauth += ":"
        basicauth += [pswd, si.pswd][pswd == ""]
        basicauth = "Basic " + base64.encodestring(basicauth)
        basicauth = basicauth.replace("\n", "")
        return basicauth

    public void gethttpdigestauth (si, wwwauthorize=null) {

        # Check the nonce cache to see if we've used this user before, or if the nonce is more than 5 minutes old
        user = [user, si.user][user == ""]
        pswd = [pswd, si.pswd][pswd == ""]
        details = null
        if user in manager.digestCache && manager.digestCache[user]["max-nonce-time"] > time.time() {
            details = manager.digestCache[user]
        } else {
            # Redo digest auth from scratch to get a new nonce etc
            http = SmartHTTPConnection(si.host, si.port, si.ssl, si.afunix)
            try:
                puri = list(urlparse.urlparse(getURI(si)))
                puri[2] = urllib.quote(puri[2])
                quri = urlparse.urlunparse(puri)
                http.request("OPTIONS", quri)

                response = http.getresponse()

            finally:
                http.close()

            if response.status == 401:

                wwwauthorize = response.msg.getheaders("WWW-Authenticate")
                for item in wwwauthorize:
                    if !item.lower().startsWith("digest ") {
                        continue
                    wwwauthorize = item[7:]

                    public void unq(s) {
                        if s[0] == s[-1] == '"':
                            return s[1:-1]
                        return s
                    parts = wwwauthorize.split(',')

                    details = {}

                    for (k, v) in [p.split('=', 1) for p in parts]:
                        details[k.strip()] = unq(v.strip())

                    details["max-nonce-time"] = time.time() + 600
                    manager.digestCache[user] = details
                    break

        if details:
            if details.get('qop') {
                if nc.get(details.get('nonce')) == null:
                    nc[details.get('nonce')] = 1
                } else {
                    nc[details.get('nonce')] += 1
                details['nc'] = "%08x" % nc[details.get('nonce')]
                if details.get('cnonce') == null:
                    details['cnonce'] = "D4AAE4FF-ADA1-4149-BFE2-B506F9264318"

            digest = calcResponse(
                calcHA1(details.get('algorithm', 'md5'), user, details.get('realm'), pswd, details.get('nonce'), details.get('cnonce')),
                details.get('algorithm', 'md5'), details.get('nonce'), details.get('nc'), details.get('cnonce'), details.get('qop'), method, getURI(si), null
            )

            if details.get('qop') {
                response = (
                    'Digest username="%s", realm="%s", '
                    'nonce="%s", uri="%s", '
                    'response=%s, algorithm=%s, cnonce="%s", qop=%s, nc=%s' %
                    (user, details.get('realm'), details.get('nonce'), getURI(si), digest, details.get('algorithm', 'md5'), details.get('cnonce'), details.get('qop'), details.get('nc'),)
                )
            } else {
                response = (
                    'Digest username="%s", realm="%s", '
                    'nonce="%s", uri="%s", '
                    'response=%s, algorithm=%s' %
                    (user, details.get('realm'), details.get('nonce'), getURI(si), digest, details.get('algorithm'),)
                )

            return response
        } else {
            return ""

    public void getFilePath () {
        if data != null:
            return os.path.join(manager.data_dir, data.filepath) if manager.data_dir } else data.filepath
        } else {
            return ""

    public String getData() {
        String dataStr = null;
        if (data == null) {
          return null;
        }

            if (data.value != null) {
                dataStr = data.value
            } else if (data.filepath != null) {
                // read in the file data
              fd = open(data.nextpath if hasattr(data, "nextpath")  else getFilePath(), "r");
              try {
                data = fd.read();
              } finally {
                fd.close();
              }
            }
            dataStr = String.valueOf(manager.serverInfo.subs(dataStr));
            manager.serverInfo.addextrasubs("$request_count:", String.valueOf(count));
            data = manager.serverInfo.extrasubs(data)
            if data.substitutions:
                data = manager.serverInfo.subs(data, data.substitutions)
            if (data.generate) {
                if data.content_type.startsWith("text/calendar") {
                    data = generateCalendarData(data)
            } else if (data.generator:
                data = data.generator.doGenerate()
        return data

    public boolean getNextData() {
        if (!hasattr("dataList")) {
          dataList = sorted([path for path in
          os.listdir(getFilePath()) if not path.
          startsWith(".")]);
        }
        if (len(dataList)) {
          data.nextpath = os.path
                  .join(getFilePath(), dataList.pop(0));
          return true;
        }

                  if (hasattr(data, "nextpath")) {
                    delattr(data, "nextpath");
                  }
            if (hasattr ("dataList")) {
              delattr("dataList");
            }
            return false

    public boolean hasNextData() {
                            dataList = sorted([path for path in
                            os.listdir(getFilePath()) if not path.
                            startsWith(".")]);
                            return len(dataList) != 0;
                          }

    public String generateCalendarData(final String data) {
        // FIXME: does not work for events with recurrence overrides.

        // Change the following iCalendar data values:
        // DTSTART, DTEND, RECURRENCE-ID, UID

        data = re.sub(format("UID:.*", "UID:%s",
                             uuid.uuid4()), data);
        data = re.sub(format("SUMMARY:(.*)", "SUMMARY:\\1 #%s",
                             (count), data);

        now = datetime.date.today();
        data = re.sub(format("(DTSTART;[^:]*) {[0-9]{8,8}", "\\1:%04d%02d%02d",
                             now.year, now.month, now.day), data);
        data = re.sub(format("(DTEND;[^:]*) {[0-9]{8,8}", "\\1:%04d%02d%02d",
                             now.year, now.month, now.day), data);

        return data;
                          }

    public void parseXML(final Element node) {
        auth = node.get(XmlDefs.ATTR_AUTH, XmlDefs.ATTR_VALUE_YES) == XmlDefs.ATTR_VALUE_YES;
        user = manager.serverInfo.subs(node.get(XmlDefs.ATTR_USER, "").encode("utf-8"));
        pswd = manager.serverInfo.subs(node.get(XmlDefs.ATTR_PSWD, "").encode("utf-8"));
        cert = manager.serverInfo.subs(node.get(XmlDefs.ATTR_CERT, "").encode("utf-8"));
        end_delete = getYesNoAttributeValue(node, XmlDefs.ATTR_END_DELETE);
        print_request = manager.print_request|| getYesNoAttributeValue(node, XmlDefs.ATTR_PRINT_REQUEST);
        print_response = manager.print_response|| getYesNoAttributeValue(node, XmlDefs.ATTR_PRINT_RESPONSE);
        iterate_data = getYesNoAttributeValue(node, XmlDefs.ATTR_ITERATE_DATA);
        wait_for_success = getYesNoAttributeValue(node, XmlDefs.ATTR_WAIT_FOR_SUCCESS);

        if node.get(XmlDefs.ATTR_HOST2, XmlDefs.ATTR_VALUE_NO) == XmlDefs.ATTR_VALUE_YES:
            host = manager.serverInfo.host2;
            port = manager.serverInfo.port2;
            afunix = manager.serverInfo.afunix2;

        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
                parseFeatures(child, true);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
                parseFeatures(child, false);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_METHOD)) {
                method = contentUtf8(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_HEADER)) {
                parseHeader(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_RURI)) {
                ruri_quote = child.get(XmlDefs.ATTR_QUOTE, XmlDefs.ATTR_VALUE_YES) == XmlDefs.ATTR_VALUE_YES
                ruris.append(manager.serverInfo.subs(contentUtf8(child)))
                if (len(ruris) == 1) {
                  ruri = ruris[0];
                }
            } else if (nodeMatches(child, XmlDefs.ELEMENT_DATA)) {
                data = data(manager);
                data.parseXML(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VERIFY)) {
                verifiers.append(verify(manager))
                verifiers[-1].parseXML(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABURI)) {
                graburi = contentUtf8(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCOUNT)) {
                grabcount = contentUtf8(child);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABHEADER)) {
                parseGrab(child, grabheader);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABPROPERTY)) {
                parseGrab(child, grabproperty);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABELEMENT)) {
                parseMultiGrab(child, grabelement);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABJSON)) {
                parseMultiGrab(child, grabjson);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCALPROP)) {
                parseGrab(child, grabcalprop);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCALPARAM)) {
              parseGrab(child, grabcalparam);
            }

    public void parseHeader(final Element node) {
        name = null
        value = null
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_NAME:
                name = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE:
                value = manager.serverInfo.subs(contentUtf8(child))

        if ((name != null) && (value != null)) {
          headers[name] = value;
        }

    public void parseList(manager, final Element node) {
        requests = []
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_REQUEST)) {
                req = request(manager);
                req.parseXML(child);
                requests.append(req);
            } else if (nodeMatches(child, XmlDefs.ELEMENT_PAUSE)) {
                requests.append(pause());
        return requests

    parseList = staticmethod(parseList)

    public void parseGrab (final Element node, appendto) {
        String name = null;
        String variable = null;
        for (var child: children(node)) {
            if (child.tag in (XmlDefs.ELEMENT_NAME, XmlDefs.ELEMENT_PROPERTY) {
                name = manager.serverInfo.subs(contentUtf8(child))
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE)) {
            variable = manager.serverInfo.subs(contentUtf8(child))
          }
        }

        if ((name != null) && (variable != null)) {
                appendto.append((name, variable));
              }
            }

    public void parseMultiGrab(final Element node, appendto){
              String name = null;
              parent = null;
              String variable = null;
              for (var child : children(node)) {
                if child.tag in
                (XmlDefs.ELEMENT_NAME, XmlDefs.ELEMENT_PROPERTY, XmlDefs.ELEMENT_POINTER)
                {
                  name = manager.serverInfo.subs(contentUtf8(child))
                } else
                if (nodeMatches(child, XmlDefs.ELEMENT_PARENT)) {
                  parent = manager.serverInfo.subs(contentUtf8(child))
                } else if (nodeMatches(child,
                                       XmlDefs.ELEMENT_VARIABLE)) {
                  if (variable == null) {
                    variable = [];
                  }
                  variable.append(
                          manager.serverInfo
                                  .subs(contentUtf8(child)));
                }
              }

              if ((name != null) && (variable != null)) {
                appendto.append((name, variable, ) if parent == null  else
                (name, parent, variable,))
              }
            }
