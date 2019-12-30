package org.bedework.davtester.request;

import org.bedework.davtester.DavTesterBase;
import org.bedework.davtester.KeyVals;
import org.bedework.davtester.Manager;
import org.bedework.davtester.RequestStats;
import org.bedework.davtester.Result;
import org.bedework.davtester.Serverinfo.KeyVal;
import org.bedework.davtester.Testfile.RequestPars;
import org.bedework.davtester.UriIdPw;
import org.bedework.davtester.Utils;
import org.bedework.davtester.XmlDefs;
import org.bedework.davtester.ical.Icalendar;
import org.bedework.davtester.verifiers.Verifier.VerifyResult;
import org.bedework.util.dav.DavUtil.MultiStatusResponse;
import org.bedework.util.http.HttpUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.xml.tagdefs.WebdavTags;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.diff;
import static org.bedework.davtester.Utils.encodeUtf8;
import static org.bedework.davtester.Utils.fileToString;
import static org.bedework.davtester.Utils.getDtParts;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.Utils.uuid;
import static org.bedework.davtester.XmlUtils.attrUtf8;
import static org.bedework.davtester.XmlUtils.children;
import static org.bedework.davtester.XmlUtils.content;
import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.davtester.XmlUtils.findNodes;
import static org.bedework.davtester.XmlUtils.getYesNoAttributeValue;
import static org.bedework.davtester.XmlUtils.multiStatusResponse;
import static org.bedework.davtester.XmlUtils.parseXmlString;
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
  public String scheme;
  public String host;
  public int port;
  private String afunix;

  private boolean auth = true;
  private String user;
  private String pswd;
  public String cert;
  public boolean endDelete;
  public boolean printRequest;
  public boolean printResponse;
  public boolean waitForSuccess;

  public String method;
  public List<String> ruris = new ArrayList<>();
  public String ruri;
  boolean ruriQuote = true;
  private Data data;
  public boolean iterateData;
  public int count = 1;
  List<Verify> verifiers = new ArrayList<>();
  public String graburi;
  public String grabcount;

  public final List<Header> headers = new ArrayList<>();
  public final List<KeyVal> grabheader = new ArrayList<>();
  public final List<KeyVal> grabproperty = new ArrayList<>();
  public final List<KeyVal> grabcalprop = new ArrayList<>();
  public final List<KeyVal> grabcalparam = new ArrayList<>();

  public static class GrabElement {
    public String path;
    public List<String> variables = new ArrayList<>();
  }

  public final List<GrabElement> grabjson = new ArrayList<>();
  public final List<GrabElement> grabelement = new ArrayList<>();

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
    if (manager != null) {
      scheme = manager.serverInfo.getScheme();
      host = manager.serverInfo.host;
      port = manager.serverInfo.port;
      afunix = manager.serverInfo.afunix;
    }
  }

  @Override
  public String getKind() {
    return "REQUEST";
  }

  public Data getData() {
    return data;
  }

  public String getURI() {
    var uri = manager.serverInfo.extrasubs(ruri);
    if (uri.contains("**")) {
      if (!uri.contains("?") || (uri.indexOf("?") > uri.indexOf("**"))) {
        uri = uri.replace("**", UUID.randomUUID().toString());
      }
    } else if (uri.contains("##")) {
      if (!uri.contains("?") || uri.indexOf("?") > uri.indexOf("##")) {
        uri = uri.replace("##", String.valueOf(count));
      }
    }

    return uri;
  }

  public List<Header> getHeaders () {
    var si = manager.serverInfo;
    var res = new ArrayList<Header>();

    for (var hdr: headers) {
      res.add(new BasicHeader(hdr.getName(),
                              si.extrasubs(hdr.getValue())));
    }

    // Content type
    if (data != null) {
      res.add(new BasicHeader("Content-Type", data.contentType));
    }

    return res;
  }

  public String getFilePath() {
    if (data == null) {
      return null;
    }

    return data.filepath;
  }

  public void setDataVal(final String val,
                         final String contentType) {
    if (data == null) {
      data = new Data(manager);
    }

    data.value = val;
    data.contentType = contentType;
  }

  public String getDataVal() {
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
    dataStr = manager.serverInfo.extrasubs(dataStr);

    if (!data.substitutions.isEmpty()) {
      dataStr = manager.serverInfo.subs(dataStr, data.substitutions);
    }

    if (data.generate) {
      if (data.contentType.startsWith("text/calendar")) {
        dataStr = generateCalendarData(dataStr);
      }
    //} else if (data.generator != null) {
      //dataStr = data.generator.doGenerate();
    }

    return dataStr;
  }

  private static LinkedList<String> dataList;

  public boolean getNextData() {
    if (!getDataList().ok) {
      //data.nextPath == null;
      return false;
    }

    data.nextpath = dataList.pop();
    return true;

    //data.dataList == null;
  }

  public boolean hasNextData() {
    return getDataList().ok;
  }

  private Result getDataList() {
    if (dataList != null) {
      if (dataList.size() > 0) {
        return Result.fail("No files in list for " + getFilePath());
      }

      return Result.ok();
    }

    dataList = new LinkedList<>();
    File folder = new File(getFilePath());
    if (!folder.isDirectory()) {
      return Result.fail("Not a directory: " + getFilePath());
    }

    var fl = folder.listFiles();
    if (fl == null) {
      return Result.fail("No files in list for " + getFilePath());
    }
    Arrays.sort(fl);

    dataList.clear();

    for (File f: fl) {
      if (!f.isFile()) {
        continue;
      }

      var nm = f.getName();
      if (nm.startsWith(".") || nm.endsWith("~")) {
        continue;
      }

      dataList.add(f.getAbsolutePath());
    }

    if (dataList.size() > 0) {
      return Result.fail("No files in list for " + getFilePath());
    }

    return Result.ok();
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

  public VerifyResult verifyRequest(final String ruri,
                                    final List<Header> responseHeaders,
                                    final int status,
                                    final String respdata) {
    var res = new VerifyResult();

    // check for response
    if (Util.isEmpty(verifiers)) {
      return res;
    }

    for (var verifier: verifiers) {
      if (verifier.hasMissingFeatures()) {
        continue;
      }
      if (verifier.hasExcludedFeatures()) {
        continue;
      }

      var ires = verifier.doVerify(ruri, responseHeaders,
                                              status, respdata);
      if (!ires.ok) {
        res.ok = false;

        res.append(format("Failed Verifier: %s\n", verifier.name));
        res.append(ires.getText());
      } else {
        res.appendOK(format("Passed Verifier: %s\n", verifier.name),
                     false);
      }
    }

    return res;
  }

  public void setUser(final String val) {
    if ((val == null) || (val.length() == 0)) {
      user = null;
    } else {
      user = val;
    }
  }

  public String getUser() {
    return user;
  }

  public void setPswd(final String val) {
    if ((val == null) || (val.length() == 0)) {
      pswd = null;
    } else {
      pswd = val;
    }
  }

  public String getPswd() {
    return pswd;
  }

  /** Send content
   *
   * @param content the content as bytes
   * @param contentType its type
   */
  private static void setContent(final HttpRequestBase req,
                                 final byte[] content,
                                 final String contentType) {
    if (content == null) {
      return;
    }

    if (!(req instanceof HttpEntityEnclosingRequestBase)) {
      throwException("Invalid operation for method " +
                             req.getMethod());
    }

    final HttpEntityEnclosingRequestBase eem = (HttpEntityEnclosingRequestBase)req;

    final ByteArrayEntity entity = new ByteArrayEntity(content);
    entity.setContentType(contentType);
    eem.setEntity(entity);
  }

  public void parseXML(final Element node) {
    auth = getYesNoAttributeValue(node, XmlDefs.ATTR_AUTH, true);
    httpTrace = getYesNoAttributeValue(node, XmlDefs.ATTR_HTTP_TRACE,
                                       false);
    setUser(manager.serverInfo.subs(attrUtf8(node, XmlDefs.ATTR_USER)));
    setPswd(manager.serverInfo.subs(attrUtf8(node, XmlDefs.ATTR_PSWD)));
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

    /* HOST2
    if (getYesNoAttributeValue(node, XmlDefs.ATTR_HOST2, false)) {
      host = manager.serverInfo.host2;
      port = manager.serverInfo.port2;
      afunix = manager.serverInfo.afunix2;
    }
     */

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
      headers.add(new BasicHeader(name, value));
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

  public void parseGrab(final Element node, final List<KeyVal>  appendto) {
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
      } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE)) {
        ge.variables.add(manager.serverInfo.subs(contentUtf8(child)));
      } else {
        throwException("Unknown grap element: " + child);
      }
    }

    if ((ge.path != null) && !Util.isEmpty(ge.variables)) {
      appendto.add(ge);
    }
  }

  public static class DoRequestResult {
    public boolean ok = true;
    public String message;
    //HttpResponse response;
    public int status;
    public String etag;
    public String protocolVersion;
    public String reason;
    public String responseData;
    public List<Header> responseHeaders;

    DoRequestResult() {
    }

    static DoRequestResult ok() {
      return new DoRequestResult();
    }

    static DoRequestResult fail(final String message) {
      var res = new DoRequestResult();
      res.ok = false;
      res.message = message;

      return res;
    }

    public void append(final String val) {
      if ((val == null) || (val.length() == 0)) {
        return;
      }

      if (message == null) {
        message = val;
        return;
      }

      message += "\n";

      message += val;
    }
  }

  public DoRequestResult run(final boolean details,
                             final boolean doverify,
                             final boolean forceverify,
                             final RequestStats stats,
                             final Map<String, String> etags,
                             final String label,
                             final int count) {
    this.count = count;

    if (this instanceof PauseRequest) {
      // Useful for pausing at a particular point
      print("Paused");
      System.console().readLine();

      return DoRequestResult.ok();
    }

    if (hasMissingFeatures()) {
      return DoRequestResult.ok();
    }
    if (hasExcludedFeatures()) {
      return DoRequestResult.ok();
    }

    // Handle special methods
    String methodPar = null;

    var split = method.split(" ");
    if (split.length > 1) {
      methodPar = split[1];
    }

    String method = split[0];

    switch (method) {
      case "DELETEALL":
        for (var requri: ruris) {
          var hrefs = doFindall(new UriIdPw(requri, getUser(), getPswd()),
                                format("%s | %s", label, "DELETEALL"));
          if (!hrefs.ok) {
            return DoRequestResult.fail(hrefs.message);
          }

          if (!doDeleteall(hrefs.val,
                           format("%s | %s", label, "DELETEALL"))) {
            return DoRequestResult.fail(format("DELETEALL failed for: %s",
                                               requri));
          }
        }
        return DoRequestResult.ok();

      case "DELAY":
        // ruri contains a numeric delay in seconds
        var delay = Integer.parseInt(this.ruri);
        synchronized (this) {
          try {
            Thread.sleep(delay * 1000);
          } catch (final InterruptedException e) {
            throwException(e);
          }
        }
        return DoRequestResult.ok();

      case "GETNEW":
      case "GETOTHER":
        final Result<String> dfnRes =
                doFindnew(UriIdPw.fromRequest(this),
                          label,
                          method.equals("GETOTHER"));
        if (!dfnRes.ok) {
          return DoRequestResult.fail(dfnRes.message);
        }
        manager.currentTestfile.grabbedLocation = dfnRes.val;
        if (graburi != null) {
          manager.serverInfo.addextrasubs(
                  new KeyVals(graburi,
                              manager.currentTestfile.grabbedLocation));
        }
        method = "GET";
        ruri = "$";
        break;

      case "FINDNEW":
        final Result<String> dfnRes1 =
                doFindnew(UriIdPw.fromRequest(this),
                          label, false);
        if (!dfnRes1.ok) {
          return DoRequestResult.fail(dfnRes1.message);
        }
        manager.currentTestfile.grabbedLocation = dfnRes1.val;
        if (graburi != null) {
          manager.serverInfo.addextrasubs(
                  new KeyVals(graburi,
                              manager.currentTestfile.grabbedLocation));
        }
        return DoRequestResult.ok();

      case "GETCONTAINS":
        final Result<String> dfcRes =
                doFindcontains(UriIdPw.fromRequest(this),
                                         methodPar, label);
        if (!dfcRes.ok) {
          return DoRequestResult.fail(dfcRes.message);
        }
        manager.currentTestfile.grabbedLocation = dfcRes.val;
        if (manager.currentTestfile.grabbedLocation == null) {
          return DoRequestResult.fail("No matching resource");
        }
        if (graburi != null) {
          manager.serverInfo.addextrasubs(
                  new KeyVals(graburi,
                              manager.currentTestfile.grabbedLocation));
        }
        method = "GET";
        ruri = "$";
        break;

      case "WAITCOUNT":
        var wcount = Integer.parseInt(methodPar);
        for (var wdruri: ruris) {
          var waitres = doWaitcount(new UriIdPw(wdruri, getUser(), getPswd()),
                                    wcount,
                                    label);
          if (!waitres.ok) {
            return DoRequestResult.fail(format("Count did not change: %s",
                                               waitres.val));
          }
        }

        return DoRequestResult.ok();

      case "WAITDELETEALL":
        for (var wdruri: ruris) {
          var waitres = doWaitcount(new UriIdPw(wdruri, getUser(), getPswd()),
                                    Integer.parseInt(methodPar),
                                    label);
          if (!waitres.ok) {
            return DoRequestResult.fail(
                    format("Count did not change: %s",
                           waitres.message));
          }

          var hrefs = doFindall(new UriIdPw(wdruri, getUser(), getPswd()),
                                format("%s | %s", label, "DELETEALL"));
          if (!hrefs.ok) {
            return DoRequestResult.fail(hrefs.message);
          }

          doDeleteall(hrefs.val,
                      format("%s | %s", label, "DELETEALL"));
        }

        return DoRequestResult.ok();
    }

    final DoRequestResult drr = new DoRequestResult();

    ruri = getURI();
    if (ruri.equals("$")) {
      ruri = manager.currentTestfile.grabbedLocation;
    }

    var headers = getHeaders();
    var data = getDataVal();

    // Cache delayed delete
    if (endDelete) {
      manager.currentTestfile.endDeletes.add(new RequestPars(ruri, this));
    }

    if (details) {
      drr.append(format("        %s: %s\n", method, ruri));
    }

    // Special for GETCHANGED
    if (method.equals("GETCHANGED")) {
      if (!doWaitchanged(new UriIdPw(ruri, getUser(), getPswd()),
                         etags.get(ruri),
                         label)) {
        return DoRequestResult.fail("Resource did not change");
      }
      method = "GET";
    }

    if (stats != null) {
      stats.startTimer();
    }

    final URI uri;
    try {
      uri = new URIBuilder(new URI(ruri))
              .setScheme(manager.serverInfo.getScheme())
              .setHost(host)
              .setPort(port)
              .build();
    } catch (final Throwable t) {
      return throwException(t);
    }

    HttpRequestBase meth = HttpUtil.findMethod(method, uri);
    if (meth == null) {
      throwException("No method: " + method);
    }

    var hasUserAgent = false;

    if (!Util.isEmpty(headers)) {
      for (final Header hdr: headers) {
        if (hdr.getName().equalsIgnoreCase("User-Agent")) {
          hasUserAgent = true;
        }
        meth.addHeader(hdr);
      }
    }

    if (!hasUserAgent && (label != null)) {
      meth.addHeader(new BasicHeader("User-Agent",
                                     Utils.encodeUtf8(label)));
    }

    if (data != null) {
      setContent(meth, data.getBytes(), getData().contentType);
    }

    if (httpTrace) {
      httpTraceOn();
    }

    try (CloseableHttpResponse resp =
                 manager.getHttpClient(getUser(),
                                       getPswd()).execute(meth)) {
      final HttpEntity ent = resp.getEntity();

      if (ent != null) {
        final InputStream in = ent.getContent();

        if (in != null) {
          drr.responseData = readContent(in, ent.getContentLength(),
                                         ContentType.getOrDefault(ent)
                                                    .getCharset());
        }
      }

      drr.reason = resp.getStatusLine().getReasonPhrase();
      drr.protocolVersion = resp.getStatusLine().getProtocolVersion().toString();
      drr.etag = HttpUtil.getFirstHeaderValue(resp, "etag");
      drr.responseHeaders = Arrays.asList(resp.getAllHeaders());
      drr.status = HttpUtil.getStatus(resp);
    } catch (final Throwable t) {
      throwException(t);
    } finally {
      if (httpTrace) {
        httpTraceOff();
      }
    }

    if (stats != null) {
      // Stop request timer before verification
      stats.endTimer();
    }

    if (doverify && (drr.responseData != null)) {
      var vres = verifyRequest(ruri,
                               drr.responseHeaders,
                               drr.status,
                               drr.responseData);
      if (!vres.ok) {
        drr.ok = false;
      }
      drr.append(vres.getText());
    } else if (forceverify) {
      drr.ok = (drr.status / 100 == 2);
      if (!drr.ok) {
        drr.append(format("Status Code Error: %d", drr.status));
      }
    }

    if (printRequest ||
            (manager.printRequestResponseOnError &&
                     (!drr.ok && !waitForSuccess))) {
      var requesttxt = "\n-------BEGIN:REQUEST-------\n" +
              data +
              "\n--------END:REQUEST--------\n";
      manager.protocol(requesttxt);
    }

    if (printResponse ||
            (manager.printRequestResponseOnError &&
                     (!drr.ok && (!waitForSuccess)))) {
      var responsetxt = "\n-------BEGIN:RESPONSE-------\n" +
              format("%s %s %s\n",
                     drr.protocolVersion,
                     drr.status, drr.reason) +
//              String.valueOf(drr.response.message) +
              drr.responseData +
              "\n--------END:RESPONSE--------\n";
      manager.protocol(responsetxt);
    }

    if (etags != null && (method.equals("GET"))) {
      if (drr.etag != null) {
        etags.put(ruri, drr.etag);
      }
    }

    if (graburi != null) {
      manager.serverInfo.addextrasubs(
              new KeyVals(graburi,
                          manager.currentTestfile.grabbedLocation));
    }

    if (grabcount != null) {
      var ctr = -1;
      if (drr.ok &&
              (drr.status == 207) &&
              (drr.responseData != null)) {
        final Result<MultiStatusResponse> msr =
                getMultiStatusResponse(drr.responseData);

        if (!msr.ok) {
          drr.ok = false;
          drr.append(msr.message);
          return drr;
        }

        ctr = msr.val.responses.size();
      }

      if (ctr == 0) {
        drr.ok = false;
        drr.append("Could not count resources in response");
      } else {
        manager.serverInfo.addextrasubs(new KeyVals(grabcount,
                                                    String.valueOf(ctr)));
      }
    }

    if (!grabheader.isEmpty()) {
      for (var prop: grabheader) {
        if (!Util.isEmpty(drr.responseHeaders)) {
          manager.serverInfo.addextrasubs(
                  new KeyVals(prop.val,
                              Utils.encodeUtf8(drr.responseHeaders.get(0).getValue())));
        } else {
          drr.ok = false;
          drr.append(format("Header %s was not extracted from response\n",
                            prop.key));
        }
      }
    }

    if (!grabproperty.isEmpty()) {
      if (drr.status == 207) {
        for (var prop: grabproperty){
          // grab the property here
          var epres = extractProperty(prop.key, drr.responseData);
          if (!epres.ok) {
            drr.ok = false;
            drr.append(format("Property %s was not extracted " +
                                      "from multistatus response",
                              prop.key));
          } else {
            manager.serverInfo.addextrasubs(new KeyVals(prop.val,
                                                        Utils.encodeUtf8(epres.val)));
          }
        }
      }
    }

    if (!Util.isEmpty(grabelement)) {
      for (var item: grabelement) {
        var elements = extractElements(item.path, drr.responseData);
        if (Util.isEmpty(elements)) {
          drr.ok = false;
          drr.append(format("Element %s was not extracted from response",
                            item.path));
        } else if (item.variables.size() != elements.size()) {
          drr.ok = false;
          drr.append(format("%d found but expecting %d for element %s from response",
                            elements.size(), item.variables.size(),
                            item.path));
        } else {
          var i = 0;
          for (var v: item.variables) {
            var e = elements.get(i);
            i++;

            manager.serverInfo.addextrasubs(
                    new KeyVals(v, contentUtf8(e)));
          }
        }
      }
    }

    if (!Util.isEmpty(grabjson)) {
      throwException("Unimplemented");
    }

    /* UNUSED
    if (!Util.isEmpty(grabjson)) {
      for (var kv: grabjson) {
        // grab the JSON value here
        var pointervalues = extractPointer(kv.path, drr.responseData);
        if (pointervalues == null) {
          drr.ok = false;
          drr.append(format("Pointer %s was not extracted from response",
                            kv.path));
        } else if (kv.variables.size() != pointervalues.size()) {
          drr.ok = false;
          drr.append(format("%d found but expecting %d for pointer %s from response",
                            pointervalues.size(),
                            kv.variables.size(),
                            kv.path));
        } else {
          var i = 0;
          for (var v: kv.variables) {
            var p = pointervalues.get(i);
            i++;

            manager.serverInfo.addextrasubs(new KeyVals(v,
                                                        encodeUtf8(p)));
          }
        }
      }
    }
     */

    if (!Util.isEmpty(grabcalprop)) {
      for (var kv: grabcalprop) {
        // grab the property here
        var propname = manager.serverInfo.subs(kv.key);
        propname = manager.serverInfo.extrasubs(propname);
        var propvalue = extractCalProperty(propname, drr.responseData);
        if (propvalue == null) {
          drr.ok = false;
          drr.append(format("Calendar property %s was not extracted from response",
                            propname));
        } else {
          manager.serverInfo.addextrasubs(
                  new KeyVals(kv.val,
                              encodeUtf8(propvalue)));
        }
      }
    }

    if (!Util.isEmpty(grabcalparam)) {
      for (var kv: grabcalparam) {
        // grab the property here
        var path = manager.serverInfo.subs(kv.key);
        path = manager.serverInfo.extrasubs(path);
        var paramvalue = extractCalParameter(path, drr.responseData);
        if (paramvalue == null) {
          drr.ok = false;
          drr.append(format("Calendar Parameter was not extracted from response: %s",
                            path));
        } else {
          manager.serverInfo.addextrasubs(new KeyVals(kv.val,
                                                      encodeUtf8(paramvalue)));
        }
      }
    }

    return drr;
  }

  public DoRequestResult doGet(final UriIdPw uip,
                               final String label) {
    var req = uip.makeRequest(this, "GET");

    var reqres = req.run(false, false, false,
                         null, // stats
                         null, // etags
                         label, 1);
    if (reqres.status / 100 != 2) {
      reqres.ok = false;
    }

    return reqres;
  }

  public Result<List<UriIdPw>> doFindall(final UriIdPw uip,
                                         final String label) {
    var hrefs = new ArrayList<UriIdPw>();

    var req = uip.makeRequest(this, "PROPFIND", "1");

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");

    var reqres = req.run(false, false, false,
                         null, // stats
                         null, // etags
                         label, 1);
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {

      var requestUri = req.getURI();
      final Result<MultiStatusResponse> msr =
              getMultiStatusResponse(reqres.responseData);

      if (!msr.ok) {
        return Result.fail(msr);
      }

      for (var response : msr.val.responses) {
        // Get href for this response
        if (!response.href.equals(requestUri)) {
          hrefs.add(new UriIdPw(response.href, uip.user, uip.pswd));
        }
      }
    }

    return new Result<>(hrefs);
  }

  public boolean doDeleteall(final List<UriIdPw> deletes,
                             final String label) {
    if (Util.isEmpty(deletes)) {
      return true;
    }
    for (var uip : deletes) {
      var req = uip.makeRequest(this, "DELETE");

      var reqres = req.run(false, false, false,
                           null, // stats
                           null, // etags
                           label, 1);
      if (reqres.status / 100 != 2) {
        return false;
      }
    }

    return true;
  }

  public Result<String> doFindnew(final UriIdPw uip,
                                  final String label,
                                  final boolean other) {
    String hresult = null;

    var uri = uip.ruri;
    String skip;

    if (other) {
      // Remove last element
      uri = StringUtils.stripEnd(manager.serverInfo.extrasubs(uri),
                                 "/");
      skip = uri;

      var pos = uri.lastIndexOf("/");

      if (pos > 0) {
        uri = uri.substring(0, pos + 1);
      }
    } else {
      skip = null;
    }

    var possibleMatches = new ArrayList<String>();

    var req = uip.makeRequest(this, "PROPFIND", "1");
    req.ruris.add(uri);
    req.ruri = uri;

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "<D:getlastmodified/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");
    var reqres = req.run(false, false, false,
                         null, // stats
                         null, // etags
                         format("%s | %s", label, "FINDNEW"),
                         1);
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {

      long latest = 0;
      var requestUri = req.getURI();
      final Result<MultiStatusResponse> msr =
              getMultiStatusResponse(reqres.responseData);

      if (!msr.ok) {
        return Result.fail(msr);
      }

      for (var response : msr.val.responses) {
        if (!response.href.equals(requestUri) &&
                (!other|| !(response.href.equals(skip)))) {

          for (var propstat: response.propstats) {
            var status = (propstat.status / 100) == 2;

            if (!status) {
              possibleMatches.add(response.href);
              continue;
            }

            for (var prop: propstat.props) {
              if (!nodeMatches(prop, WebdavTags.getlastmodified)) {
                continue;
              }

              var value = content(prop);
              var fmt = DateTimeFormatter.RFC_1123_DATE_TIME;
              ZonedDateTime zdt = fmt.parse (value , ZonedDateTime :: from);
              long tval = Date.from(zdt.toInstant()).getTime();

              if (tval > latest) {
                possibleMatches.clear();
                possibleMatches.add(response.href);
                latest = tval;
              } else if (tval == latest) {
                possibleMatches.add(response.href);
              }
            }
          }
        }
      }
    }

    if (possibleMatches.size() == 1) {
      hresult = possibleMatches.get(0);
    } else if (possibleMatches.size() > 1) {
      var notSeenBefore = diff(possibleMatches,
                               manager.currentTestfile.previouslyFound);
      if (notSeenBefore.size() == 1) {
        hresult = notSeenBefore.get(0);
      }
    }
    if (hresult != null) {
      manager.currentTestfile.previouslyFound.add(hresult);
    }

    return new Result<>(hresult);
  }

  public Result<String> doFindcontains(final UriIdPw uip,
                                       final String match,
                                       final String label) {
    var req = uip.makeRequest(this, "PROPFIND", "1");

    req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                           "<D:propfind xmlns:D=\"DAV:\">" +
                           "<D:prop>" +
                           "<D:getetag/>" +
                           "</D:prop>" +
                           "</D:propfind>",
                   "text/xml");

    var reqres = req.run(false, false, false,
                         null, // stats
                         null, // etags
                         format("%s | %s", label, "FINDNEW"), 1);
    String href = null;
    if (reqres.ok &&
            (reqres.status == 207) &&
            (reqres.responseData != null)) {

      var requestUri = req.getURI();

      final Result<MultiStatusResponse> msr =
              getMultiStatusResponse(reqres.responseData);

      if (!msr.ok) {
        return Result.fail(msr);
      }

      for (var response : msr.val.responses) {
        if (!response.href.equals(requestUri)) {
          var respdata = req.doGet(new UriIdPw(response.href,
                                               uip.user,
                                               uip.pswd),
                                   label);
          if (respdata.responseData.contains(match)) {
            href = response.href;
            break;
          }
        }
      }
    }

    return new Result<>(href);
  }

  public Result doWaitcount(final UriIdPw uip,
                            final int hrefCount,
                            final String label) {
    var hrefs = new ArrayList<String>();

    for (var ignore = 0; ignore < manager.serverInfo.waitcount; ignore++) {
      var req = uip.makeRequest(this, "PROPFIND", "1");

      req.setDataVal("<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                             "<D:propfind xmlns:D=\"DAV:\">" +
                             "<D:prop>" +
                             "<D:getetag/>" +
                             "</D:prop>" +
                             "</D:propfind>",
                     "text/xml");

      var reqres = req.run(false, false, false,
                           null, // stats
                           null, // etags
                           format("%s | %s %d", label,
                                  "WAITCOUNT", hrefCount),
                           1);
      hrefs.clear();

      if (reqres.ok &&
              (reqres.status == 207) &&
              (reqres.responseData != null)) {
        final Result<MultiStatusResponse> msr =
                getMultiStatusResponse(reqres.responseData);

        if (!msr.ok) {
          return msr;
        }

        for (var response : msr.val.responses) {
          // Get href for this response
          var href = response.href;
          if (!StringUtils.stripEnd(href, "/").equals(
                  StringUtils.stripEnd(uip.ruri, "/"))) {
            hrefs.add(href);
          }
        }

        if (hrefs.size() == hrefCount) {
          return Result.ok();
        }
      }
      var delay = manager.serverInfo.waitdelay;
      synchronized (this) {
        try {
          Thread.sleep(delay);
        } catch (final InterruptedException e) {
          throwException(e);
        }
      }
    }

    if (!manager.debug() || Util.isEmpty(hrefs)) {
      return Result.fail(String.valueOf(hrefs.size()));
    }
    // Get the content of each resource
    var rdata = new StringBuilder();

    for (var href: hrefs) {
      var getDrr = doGet(new UriIdPw(href, uip.user, uip.pswd), label);
      String test = "unknown";
      var rd = getDrr.responseData;
      if (rd.startsWith("BEGIN:VCALENDAR")) {
        var uidpos = rd.indexOf("UID:");
        if (uidpos != -1) {
          var end = rd.indexOf("\r\n", uidpos);
          if (end < 0) {
            end = rd.indexOf("\n", uidpos);
          }

          if (end < 0) {
            return Result.fail("No UID end found in\n" + rdata.toString());
          }
          var uid = rd.substring(uidpos + 4, end);
          test = manager.currentTestfile.uidmaps
                  .computeIfAbsent(uid, s -> "unknown");
        }
      }
      rdata.append(format("\n\nhref: %s\ntest: %s\n\n%s\n",
                          href, test, getDrr.responseData));
    }

    return Result.fail(rdata.toString());
  }

  public boolean doWaitchanged(final UriIdPw uip,
                               final String etag,
                               final String label) {
    for (var ignore = 0; ignore < manager.serverInfo.waitcount; ignore++) {
      var req = uip.makeRequest(this, "HEAD");

      var reqres = req.run(false, false, false,
                           null, // stats
                           null, // etags
                           format("%s | %s", label, "WAITCHANGED"), 1);
      if (reqres.ok) {
        if (reqres.status / 100 == 2) {
          if (!etag.equals(reqres.etag)) {
            break;
          }
        } else {
          return false;
        }
      }
      var delay = manager.serverInfo.waitdelay;
      synchronized (this) {
        try {
          Thread.sleep(delay);
        } catch (final InterruptedException e) {
          throwException(e);
        }
      }
    }

    return true;
  }

  private Result<String> extractProperty(final String propertyname,
                                         final String respdata) {
    final Result<MultiStatusResponse> msr =
            getMultiStatusResponse(respdata);

    if (!msr.ok) {
      return Result.fail(msr);
    }

    for (var response : msr.val.responses) {
      for (var propstat: response.propstats) {
        if ((propstat.status / 100) != 2) {
          continue;
        }

        // Get properties for this propstat
        if (propstat.props.size() != 1) {
          return Result.fail("           Wrong number of DAV:prop elements");
        }

        for (var child: children(propstat.props.get(0))) {
          var tag = child.getTagName();
          if (!tag.equals(propertyname)) {
            continue;
          }

          var subch = children(child);

          if (subch.size() == 0) {
            return new Result<>(content(child));
          }

          // Copy sub-element data as text into one long string and strip leading/trailing space
          var value = new StringBuilder();
          for (var p: subch) {
            value.append(content(p).strip());
          }

          return new Result<>(value.toString());
        }
      }
    }

    return Result.fail((String)null);
  }

  private List<Element> extractElements (final String elementpath,
                                         final String respdata) {
    final Element rootEl = parseXmlString(respdata).getDocumentElement();

    final String testPath;
    final boolean atRoot;

    if (elementpath.startsWith("/")) {
      testPath = elementpath.substring(1);
      atRoot = true;
    } else {
      testPath = elementpath;
      atRoot = false;
    }

    return findNodes(parseXmlString(respdata),
                     atRoot,
                     testPath);
  }

  /* UNUSED
  public void extractPointer (final String pointer,
                              final String respdata) {
    jp = JSONMatcher(pointer);

    try {
      j = json.loads(respdata);
    } except {
      return null;
    }

    return jp.match(j);
  }
  */

  public String extractCalParameter(final String path,
                                    final String respdata) {
    /* If the path has a $... segment at the end, split it off
       as the desired property value.
     */
    var pos = path.indexOf('$');
    String pvalue = null;
    String ppath = path;
    if (pos > 0) {
      ppath = path.substring(0, pos);
      pvalue = path.substring(pos + 1);
    }

    // path is a path consisting of component and property names
    // followed by a parameter name
    // e.g. VEVENT/ATTACH/MANAGED-ID
    pos = ppath.lastIndexOf('/');
    var paramName = ppath.substring(pos + 1);
    ppath = ppath.substring(0, pos);

    var prop = calProperty(ppath, pvalue, respdata);

    if (prop == null) {
      return null;
    }

    var param = prop.getParameter(paramName);

    if (param == null) {
      return null;
    }

    return param.getValue();
  }

  public String extractCalProperty(final String path,
                                   final String respdata) {
    /* If the path has a $... segment at the end, split it off
       as the desired property value.
     */
    var pos = path.indexOf('$');
    String pvalue = null;
    String ppath = path;
    if (pos > 0) {
      ppath = path.substring(0, pos);
      pvalue = path.substring(pos + 1);
    }
    var prop = calProperty(ppath, pvalue, respdata);
    if (prop == null) {
      return null;
    }

    return prop.getValue();
  }

  private Property calProperty(final String propertyname,
                               final String propertyValue,
                               final String respdata) {
    Component comp = Icalendar.parseText(respdata);

    // propname is a path consisting of component and property names
    // e.g. VEVENT/ATTACH
    var split = propertyname.split("/");

    var spliti = 0;

    while (spliti < split.length) {
      var name = split[spliti];

      var found = false;
      for (var c: comp.getComponents()) {
        if (c.getName().equals(name)) {
          found = true;
          comp = c;
          spliti++;
          break;
        }
      }

      if (!found) {
        break;
      }
    }

    if (spliti == 0) {
      // Didn't match top level component;
      return null;
    }

    // Try properties

    var name = split[spliti];
    var props = comp.getProperties(name);

    if (propertyValue != null) {
      for (var prop: props) {
        if (prop.getValue().equals(propertyValue)) {
          return prop;
        }
      }

      return null;
    }

    if (Util.isEmpty(props)) {
      return null;
    }

    return props.get(0);
  }

  String readContent(final InputStream in, final long expectedLen,
                     final Charset characterSet) throws Throwable {
    StringBuilder res = new StringBuilder();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int len = 0;
    String charset;

    if (characterSet == null) {
      charset = StandardCharsets.UTF_8.toString();
    } else {
      charset = characterSet.toString();
    }

    //if (logger.debug()) {
    //  System.out.println("Read content - expected=" + expectedLen);
    //}

    boolean hadLf = false;
    boolean hadCr = false;

    while ((expectedLen < 0) || (len < expectedLen)) {
      int ich = in.read();
      if (ich < 0) {
        break;
      }

      len++;

      if (ich == '\n') {
        if (res.length() == 0) {
          continue;
        }
        if (hadLf) {
          res.append('\n');
          hadLf = false;
          hadCr = false;
        } else {
          hadLf = true;
        }
        continue;
      }

      if (ich == '\r') {
        if (hadCr) {
          res.append('\r');
          hadLf = false;
          hadCr = false;
        } else {
          hadCr = true;
        }
        continue;
      }

      if (hadCr || hadLf) {
        hadLf = false;
        hadCr = false;

        if (baos.size() > 0) {
          res.append(new String(baos.toByteArray(), charset));
          if (hadCr) {
            res.append('\r');
          } else {
            res.append('\n');
          }
        }

        baos.reset();
        baos.write(ich);
        continue;
      }

      baos.write(ich);
    }

    if (baos.size() > 0) {
      res.append(new String(baos.toByteArray(), charset));
    }

    return res.toString();
  }

  private Result<MultiStatusResponse> getMultiStatusResponse(
          final String data) {
    try {
      return new Result(multiStatusResponse(data));
    } catch (final Throwable t) {
      return Result.fail(format(
              "Bad multi-staus response. Message was %s\n" +
                      "Data was %s", t.getMessage(), data));
    }
  }
}
