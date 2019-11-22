package org.bedework.davtester.request;

import org.bedework.davtester.DavTesterBase;
import org.bedework.davtester.KeyVals;
import org.bedework.davtester.Manager;
import org.bedework.davtester.Serverinfo.KeyVal;
import org.bedework.davtester.XmlDefs;
import org.bedework.util.misc.Util;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.fileToString;
import static org.bedework.davtester.Utils.getDtParts;
import static org.bedework.davtester.Utils.uuid;
import static org.bedework.davtester.XmlUtils.attrUtf8;
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
public class Request extends DavTesterBase {
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

  String method;
  List<String> ruris = new ArrayList<>();
  String ruri;
  boolean ruriQuote = true
  private Data data = null
  boolean iterateData;
  int count = 1;
  List<Verify> verifiers = new ArrayList<>();
  String graburi;
  String grabcount;

  final List<KeyVal> headers = new ArrayList<>();
  final List<KeyVal> grabheader = new ArrayList<>();
  final List<KeyVal> grabproperty =  new ArrayList<>();
  final List<KeyVal> grabcalprop = new ArrayList<>();
  final List<KeyVal> grabcalparam = new ArrayList<>();

  public static class GrabElement {
    String path;
    String parent;
    List<String> variables = new ArrayList<>();
  }

  final List<GrabElement> grabjson = new ArrayList<>();
  final List<GrabElement> grabelement = new ArrayList<>();

  //nc = {}  // Keep track of nonce count

  /** Just flags a pause.
   *
   */
  public static class PauseRequest extends Request {
    PauseRequest() {
      super(null);
    }
  }

  public final static Request pause = new PauseRequest();

  public Request(final Manager manager) {
    super(manager);
    host = manager.serverInfo.host
    port = manager.serverInfo.port
    afunix = manager.serverInfo.afunix
  }

  @Override
  public String getKind() {
    return "REQUEST";
  }

  public void __str__ () {
    return "Method: %s; uris: %s" % (method,ruris if len(ruris) > 1  else
    ruri,)
  }

  public void getURI() {
    uri = manager.serverInfo.extrasubs(ruri);
    if ("**" in uri) {
      if ("?" not in uri|| uri.find("?") > uri.find("**") {
        uri = uri.replace("**", String.valueOf(uuid.uuid4()));
      }
    } else (if ("##" in uri) {
      if ("?" not in uri|| uri.find("?") > uri.find("##") {
        uri = uri.replace("##", String.valueOf(count))
      }
    }

    return uri;
  }

  public void getHeaders () {
    var si = manager.serverInfo;

    hdrs = headers
    for key, value in hdrs.items() {
      hdrs[key] = si.extrasubs(value);
    }

    // Content type
    if (data != null) {
      hdrs["Content-Type"] = data.contentType;
    }

    // Auth
    if (auth) {
      if (manager.serverInfo.authtype.lower() == "basic") {
        hdrs["Authorization"] = gethttpbasicauth();
      } else if (si.authtype.lower() == "digest") {
        hdrs["Authorization"] = gethttpdigestauth();
      }
    }

    return hdrs;
  }

    public void gethttpbasicauth () {
      basicauth = [user, manager.serverInfo.user][user == ""]
      basicauth += ":"
      basicauth += [pswd, manager.serverInfo.pswd][pswd == ""]
      basicauth = "Basic " + base64.encodestring(basicauth)
      basicauth = basicauth.replace("\n", "")
      return basicauth;
    }

  public void gethttpdigestauth(wwwauthorize) {
    var si = manager.serverInfo;

    // Check the nonce cache to see if we've used this user before, or if the nonce is more than 5 minutes old
    user = [user, si.user][user == ""];
    pswd = [pswd, si.pswd][pswd == ""];
    details = null;

    if ((manager.digestCache.get(user) != null) && (manager.digestCache[user]["max-nonce-time"] > time.time())) {
      details = manager.digestCache[user];
    } else {
      // Redo digest auth from scratch to get a new nonce etc
      http = SmartHTTPConnection(si.host, si.port, si.ssl, si.afunix);
      try {
        puri = list(urlparse.urlparse(getURI(si)));
        puri[2] = urllib.quote(puri[2]);
        quri = urlparse.urlunparse(puri);
        http.request("OPTIONS", quri);

        response = http.getresponse();

      } finally{
        http.close()
      }

      if (response.status == 401) {

        wwwauthorize = response.msg.getheaders("WWW-Authenticate")
        for (item: wwwauthorize) {
          if (!item.lower().startsWith("digest ") {
            continue;
          }
          wwwauthorize = item[7:]


          parts = wwwauthorize.split(',')

          details = {}

          for ((k, v) in[p.split('=', 1) for p in parts]) {
            details[k.strip()] = unq(v.strip())
          }

          details["max-nonce-time"] = time.time() + 600
          manager.digestCache[user] = details;
          break;
        }
      }
    }

    if (details == null) {
      return null;
    }

    if (details.get('qop') {
      if (nc.get(details.get('nonce')) == null) {
        nc[details.get('nonce')] = 1;
      } else {
        nc[details.get('nonce')] += 1;
      }
      details['nc'] = "%08x" % nc[details.get('nonce')]
      if (details.get('cnonce') == null) {
        details['cnonce'] = "D4AAE4FF-ADA1-4149-BFE2-B506F9264318";
      }
    }
    digest = calcResponse(
            calcHA1(details.get('algorithm', 'md5'), user, details.get('realm'), pswd, details.get('nonce'), details.get('cnonce')),
            details.get('algorithm', 'md5'), details.get('nonce'), details.get('nc'), details.get('cnonce'), details.get('qop'), method, getURI(si), null
    );

    if (details.get('qop')) {
      response = (
              'Digest username="%s", realm="%s", '
      'nonce="%s", uri="%s", '
      'response=%s, algorithm=%s, cnonce="%s", qop=%s, nc=%s' %
              (user, details.get('realm'), details.get('nonce'), getURI(si), digest, details.get('algorithm', 'md5'), details.get('cnonce'), details.get('qop'), details.get('nc'),)
      );
    } else{
      response = (
              'Digest username="%s", realm="%s", '
      'nonce="%s", uri="%s", '
      'response=%s, algorithm=%s' %
              (user, details.get('realm'), details
              .get('nonce'), getURI(si), digest, details
              .get('algorithm'));
      );
    }

    return response;
  }

  private void unq(s){
    if s[0] == s[-1] == '"':
    return s[1:-1]
    return s
  }

  public String getFilePath() {
    if (data == null) {
      return null;
    }

    if (manager.dataDir == null) {
      return data.filepath;
    }

    return Util.buildPath(false, manager.dataDir, "/", data.filepath);
  }

  public String getData() {
    String dataStr = null;

    if (data == null) {
      return null;
    }

    if (data.value != null) {
      dataStr = data.value;
    } else if (data.filepath != null) {
      // read in the file data
      String fname;
      if (data.nextpath != null) {
        fname = data.nextpath;
      } else {
        fname = getFilePath();
      }

      dataStr = fileToString(fname);
    }

    dataStr = String.valueOf(manager.serverInfo.subs(dataStr));
    manager.serverInfo.addextrasubs(new KeyVals("$request_count:",
                                                String.valueOf(count)));
    dataStr = manager.serverInfo.extrasubs(dataStr)

    if (!data.substitutions.isEmpty()) {
      dataStr = manager.serverInfo.subs(dataStr, data.substitutions);
    }

    if (data.generate) {
      if (data.contentType.startsWith("text/calendar")) {
        dataStr = generateCalendarData(dataStr);
      }
    } else if (data.generator != null) {
      dataStr = data.generator.doGenerate();
    }

    return dataStr;
  }

  public boolean getNextData() {
    if (dataList == null) {
      dataList = sorted([path for path in
      if (!path.startsWith(".")) {
        os.listdir(getFilePath());
      }
    }

    if (!Util.isEmpty(dataList)) {
      data.nextpath = os.path.join(getFilePath(), dataList.pop(0));
      return true;
    }

    data.nextpath == null;
    data.dataList == null;

    return false;
  }

  public boolean hasNextData() {
    dataList = sorted([path for path in
    os.listdir(getFilePath()) if not path.
    startsWith(".")]);
    return len(dataList) != 0;

    File folder = new File("your/path");
    File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        System.out.println("File " + listOfFiles[i].getName());
      } else if (listOfFiles[i].isDirectory()) {
        System.out.println("Directory " + listOfFiles[i].getName());
      }
    }
  }

  public String generateCalendarData(final String dataVal) {
    // FIXME: does not work for events with recurrence overrides.

    // Change the following iCalendar data values:
    // DTSTART, DTEND, RECURRENCE-ID, UID

    // This was re.sub(...
    var data = dataVal.replaceAll("UID:.*", "UID:" + uuid());
    data = data.replaceAll("SUMMARY:(.*)", "SUMMARY:\\1 #" + count);

    var now = getDtParts(new Date());

    data = data.replaceAll("(DTSTART;[^:]*) [0-9]{8,8}",
                           format("\\1:%04d%02d%02d",
                                  now.year, now.month, now.dayOfMonth));

    data = data.replaceAll("(DTEND;[^:]*) [0-9]{8,8}",
                           format("\\1:%04d%02d%02d",
                                  now.year, now.month, now.dayOfMonth));

    return data;
  }

  public void parseXML(final Element node) {
    auth = getYesNoAttributeValue(node, XmlDefs.ATTR_AUTH, true);
    user = manager.serverInfo.subs(attrUtf8(node, XmlDefs.ATTR_USER));
    pswd = manager.serverInfo.subs(attrUtf8(node, XmlDefs.ATTR_PSWD));
    cert = manager.serverInfo.subs(attrUtf8(node, XmlDefs.ATTR_CERT));
    endDelete = getYesNoAttributeValue(node,
                                        XmlDefs.ATTR_END_DELETE);
    printRequest = manager.printRequest || getYesNoAttributeValue(
            node, XmlDefs.ATTR_PRINT_REQUEST);
    printResponse = manager.printResponse || getYesNoAttributeValue(
            node, XmlDefs.ATTR_PRINT_RESPONSE);
    iterateData = getYesNoAttributeValue(node,
                                         XmlDefs.ATTR_ITERATE_DATA);
    waitForSuccess = getYesNoAttributeValue(node,
                                            XmlDefs.ATTR_WAIT_FOR_SUCCESS);

    if (getYesNoAttributeValue(node, XmlDefs.ATTR_HOST2, false)) {
      host = manager.serverInfo.host2;
      port = manager.serverInfo.port2;
      afunix = manager.serverInfo.afunix2;
    }

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE)) {
        parseFeatures(child, true);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_EXCLUDE_FEATURE)) {
        parseFeatures(child, false);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_METHOD)) {
        method = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_HEADER)) {
        parseHeader(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_RURI)) {
        ruriQuote = getYesNoAttributeValue(child,
                                           XmlDefs.ATTR_QUOTE,
                                           true);
        ruris.add(manager.serverInfo.subs(contentUtf8(child)));
        if (ruris.size() == 1) {
          ruri = ruris.get(0);
        }
      } else if (nodeMatches(child, XmlDefs.ELEMENT_DATA)) {
        data = new Data(manager);
        data.parseXML(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VERIFY)) {
        var v = new Verify(manager);
        verifiers.add(v);
        v.parseXML(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABURI)) {
        graburi = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCOUNT)) {
        grabcount = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABHEADER)) {
        parseGrab(child, grabheader);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_GRABPROPERTY)) {
        parseGrab(child, grabproperty);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_GRABELEMENT)) {
        parseMultiGrab(child, grabelement);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABJSON)) {
        parseMultiGrab(child, grabjson);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_GRABCALPROP)) {
        parseGrab(child, grabcalprop);
      } else if (nodeMatches(child,
                             XmlDefs.ELEMENT_GRABCALPARAM)) {
        parseGrab(child, grabcalparam);
      }
    }
  }

  public void parseHeader(final Element node) {
    String name = null;
    String value = null;

    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_NAME)) {
        name = contentUtf8(child);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE)) {
        value = manager.serverInfo.subs(contentUtf8(child));
      }
    }

    if ((name != null) && (value != null)) {
      headers.add(new KeyVal(name, value));
    }
  }

  public static List<Request> parseList(final Manager manager,
                                        final Element node) {
    final List<Request> requests = new ArrayList<>();

    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_REQUEST)) {
        var req = new Request(manager);
        req.parseXML(child);
        requests.add(req);
      } else if (nodeMatches(child, XmlDefs.ELEMENT_PAUSE)) {
        requests.add(pause);
      }
    }

    return requests;
  }

//    parseList = staticmethod(parseList)

  public void parseGrab (final Element node, final List<KeyVal>  appendto) {
    String name = null;
    String variable = null;

    for (var child: children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_NAME) ||
              nodeMatches(child, XmlDefs.ELEMENT_PROPERTY)) {
        name = manager.serverInfo.subs(contentUtf8(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE)) {
        variable = manager.serverInfo.subs(contentUtf8(child));
      }
    }

    if ((name != null) && (variable != null)) {
      appendto.add(new KeyVal(name, variable));
    }
  }

  public void parseMultiGrab(final Element node, final List<GrabElement> appendto) {
    final GrabElement ge = new GrabElement();

    for (var child : children(node)) {
      if (nodeMatches(child, XmlDefs.ELEMENT_NAME) ||
              nodeMatches(child, XmlDefs.ELEMENT_PROPERTY) ||
                       nodeMatches(child, XmlDefs.ELEMENT_POINTER)) {
        ge.path = manager.serverInfo.subs(contentUtf8(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_PARENT)) {
        ge.parent = manager.serverInfo.subs(contentUtf8(child));
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE)) {
        ge.variables.add(manager.serverInfo.subs(contentUtf8(child)));
      }
    }

    if ((ge.path != null) && !Util.isEmpty(ge.variables)) {
      appendto.add(ge);
    }
  }
}
