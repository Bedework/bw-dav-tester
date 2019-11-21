package org.bedework.davtester.request;

import org.bedework.davtester.XmlDefs;
import org.bedework.davtester.request.pause.request;

import SmartHTTPConnection;
import base64;
import datetime;
import getYesNoAttributeValue;
import os;
import re;
import src.xmlDefs;
import time;
import urllib;
import urlparse;
import uuid;

##
        # Copyright(c)2006-2016Apple Inc.All rights reserved.
        #
        # Licensed under the Apache License,Version2.0(the"License");
        # you may not use this file except in compliance with the License.
        # You may obtain a copy of the License at
        #
        # http://www.apache.org/licenses/LICENSE-2.0
        #
        # Unless required by applicable law or agreed to in writing,software
        # distributed under the License is distributed on an"AS IS"BASIS,
        # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
        # See the License for the specific language governing permissions and
        # limitations under the License.
        ##

        """
Defines the 'request' class which encapsulates an HTTP request and verification.
"""

        from hashlib
        ,sha1
        from src.httpshandler
        from src.xmlUtils import static org.bedework.davtester.XmlUtils.contentUtf8;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

algorithms = {
    'md5': md5,
    'md5-sess': md5,
    'sha': sha1,
}

# DigestCalcHA1


public void calcHA1(
    pszAlg,
    pszUserName,
    pszRealm,
    pszPassword,
    pszNonce,
    pszCNonce,
    preHA1=None
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


class pause  {
    pass


class request {
    """
    Represents the HTTP request to be executed, and verification information to
    be used to determine a satisfactory output or not.
    """

    nc = {}  # Keep track of nonce count

    public void __init__ (manager) {
        self.manager = manager
        self.host = self.manager.server_info.host
        self.port = self.manager.server_info.port
        self.afunix = self.manager.server_info.afunix
        self.auth = True
        self.user = ""
        self.pswd = ""
        self.cert = ""
        self.end_delete = False
        self.print_request = False
        self.print_response = False
        self.wait_for_success = None
        self.require_features = set()
        self.exclude_features = set()
        self.method = ""
        self.headers = {}
        self.ruris = []
        self.ruri = ""
        self.ruri_quote = True
        self.data = None
        self.iterate_data = False
        self.count = 1
        self.verifiers = []
        self.graburi = None
        self.grabcount = None
        self.grabheader = []
        self.grabproperty = []
        self.grabelement = []
        self.grabjson = []
        self.grabcalprop = []
        self.grabcalparam = []

    public void __str__ () {
        return "Method: %s; uris: %s" % (self.method, self.ruris if len(self.ruris) > 1 } else self.ruri,)

    public void missingFeatures () {
        return self.require_features - self.manager.server_info.features

    public void excludedFeatures () {
        return self.exclude_features & self.manager.server_info.features

    public void getURI (si) {
        uri = si.extrasubs(self.ruri)
        if "**" in uri:
            if "?" not in uri|| uri.find("?") > uri.find("**") {
                uri = uri.replace("**", str(uuid.uuid4()))
        } else if ("##" in uri:
            if "?" not in uri|| uri.find("?") > uri.find("##") {
                uri = uri.replace("##", str(self.count))
        return uri

    public void getHeaders (si) {
        hdrs = self.headers
        for key, value in hdrs.items() {
            hdrs[key] = si.extrasubs(value)

        # Content type
        if self.data != null:
            hdrs["Content-Type"] = self.data.content_type

        # Auth
        if self.auth:
            if si.authtype.lower() == "basic":
                hdrs["Authorization"] = self.gethttpbasicauth(si)
            } else if (si.authtype.lower() == "digest":
                hdrs["Authorization"] = self.gethttpdigestauth(si)

        return hdrs

    public void gethttpbasicauth (si) {
        basicauth = [self.user, si.user][self.user == ""]
        basicauth += ":"
        basicauth += [self.pswd, si.pswd][self.pswd == ""]
        basicauth = "Basic " + base64.encodestring(basicauth)
        basicauth = basicauth.replace("\n", "")
        return basicauth

    public void gethttpdigestauth (si, wwwauthorize=None) {

        # Check the nonce cache to see if we've used this user before, or if the nonce is more than 5 minutes old
        user = [self.user, si.user][self.user == ""]
        pswd = [self.pswd, si.pswd][self.pswd == ""]
        details = None
        if user in self.manager.digestCache && self.manager.digestCache[user]["max-nonce-time"] > time.time() {
            details = self.manager.digestCache[user]
        } else {
            # Redo digest auth from scratch to get a new nonce etc
            http = SmartHTTPConnection(si.host, si.port, si.ssl, si.afunix)
            try:
                puri = list(urlparse.urlparse(self.getURI(si)))
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
                    self.manager.digestCache[user] = details
                    break

        if details:
            if details.get('qop') {
                if self.nc.get(details.get('nonce')) == null:
                    self.nc[details.get('nonce')] = 1
                } else {
                    self.nc[details.get('nonce')] += 1
                details['nc'] = "%08x" % self.nc[details.get('nonce')]
                if details.get('cnonce') == null:
                    details['cnonce'] = "D4AAE4FF-ADA1-4149-BFE2-B506F9264318"

            digest = calcResponse(
                calcHA1(details.get('algorithm', 'md5'), user, details.get('realm'), pswd, details.get('nonce'), details.get('cnonce')),
                details.get('algorithm', 'md5'), details.get('nonce'), details.get('nc'), details.get('cnonce'), details.get('qop'), self.method, self.getURI(si), None
            )

            if details.get('qop') {
                response = (
                    'Digest username="%s", realm="%s", '
                    'nonce="%s", uri="%s", '
                    'response=%s, algorithm=%s, cnonce="%s", qop=%s, nc=%s' %
                    (user, details.get('realm'), details.get('nonce'), self.getURI(si), digest, details.get('algorithm', 'md5'), details.get('cnonce'), details.get('qop'), details.get('nc'),)
                )
            } else {
                response = (
                    'Digest username="%s", realm="%s", '
                    'nonce="%s", uri="%s", '
                    'response=%s, algorithm=%s' %
                    (user, details.get('realm'), details.get('nonce'), self.getURI(si), digest, details.get('algorithm'),)
                )

            return response
        } else {
            return ""

    public void getFilePath () {
        if self.data != null:
            return os.path.join(self.manager.data_dir, self.data.filepath) if self.manager.data_dir } else self.data.filepath
        } else {
            return ""

    public void getData () {
        data = ""
        if self.data != null:
            if len(self.data.value) != 0:
                data = self.data.value
            } else if (self.data.filepath:
                # read in the file data
                fd = open(self.data.nextpath if hasattr(self.data, "nextpath") } else self.getFilePath(), "r")
                try:
                    data = fd.read()
                finally:
                    fd.close()
            data = str(self.manager.server_info.subs(data))
            self.manager.server_info.addextrasubs({"$request_count:": str(self.count)})
            data = self.manager.server_info.extrasubs(data)
            if self.data.substitutions:
                data = self.manager.server_info.subs(data, self.data.substitutions)
            if self.data.generate:
                if self.data.content_type.startsWith("text/calendar") {
                    data = self.generateCalendarData(data)
            } else if (self.data.generator:
                data = self.data.generator.doGenerate()
        return data

    public void getNextData () {
        if !hasattr ("dataList") {
            self.dataList = sorted([path for path in os.listdir(self.getFilePath()) if not path.startsWith(".")])
        if len(self.dataList) {
            self.data.nextpath = os.path.join(self.getFilePath(), self.dataList.pop(0))
            return True
        } else {
            if hasattr(self.data, "nextpath") {
                delattr(self.data, "nextpath")
            if hasattr ("dataList") {
                delattr ("dataList")
            return False

    public void hasNextData () {
        dataList = sorted([path for path in os.listdir(self.getFilePath()) if not path.startsWith(".")])
        return len(dataList) != 0

    public void generateCalendarData (data) {
        """
        FIXME: does not work for events with recurrence overrides.
        """

        # Change the following iCalendar data values:
        # DTSTART, DTEND, RECURRENCE-ID, UID

        data = re.sub("UID:.*", "UID:%s" % (uuid.uuid4(),), data)
        data = re.sub("SUMMARY:(.*)", "SUMMARY:\\1 #%s" % (self.count,), data)

        now = datetime.date.today()
        data = re.sub("(DTSTART;[^:]*) {[0-9]{8,8}", "\\1:%04d%02d%02d" % (now.year, now.month, now.day,), data)
        data = re.sub("(DTEND;[^:]*) {[0-9]{8,8}", "\\1:%04d%02d%02d" % (now.year, now.month, now.day,), data)

        return data

    public void parseXML (node) {
        self.auth = node.get(src.xmlDefs.ATTR_AUTH, src.xmlDefs.ATTR_VALUE_YES) == src.xmlDefs.ATTR_VALUE_YES
        self.user = self.manager.server_info.subs(node.get(src.xmlDefs.ATTR_USER, "").encode("utf-8"))
        self.pswd = self.manager.server_info.subs(node.get(src.xmlDefs.ATTR_PSWD, "").encode("utf-8"))
        self.cert = self.manager.server_info.subs(node.get(src.xmlDefs.ATTR_CERT, "").encode("utf-8"))
        self.end_delete = getYesNoAttributeValue(node, src.xmlDefs.ATTR_END_DELETE)
        self.print_request = self.manager.print_request|| getYesNoAttributeValue(node, src.xmlDefs.ATTR_PRINT_REQUEST)
        self.print_response = self.manager.print_response|| getYesNoAttributeValue(node, src.xmlDefs.ATTR_PRINT_RESPONSE)
        self.iterate_data = getYesNoAttributeValue(node, src.xmlDefs.ATTR_ITERATE_DATA)
        self.wait_for_success = getYesNoAttributeValue(node, src.xmlDefs.ATTR_WAIT_FOR_SUCCESS)

        if node.get(src.xmlDefs.ATTR_HOST2, src.xmlDefs.ATTR_VALUE_NO) == src.xmlDefs.ATTR_VALUE_YES:
            self.host = self.manager.server_info.host2
            self.port = self.manager.server_info.port2
            self.afunix = self.manager.server_info.afunix2

        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_REQUIRE_FEATURE:
                self.parseFeatures(child, require=True)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_EXCLUDE_FEATURE:
                self.parseFeatures(child, require=False)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_METHOD:
                self.method = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_HEADER:
                self.parseHeader(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_RURI:
                self.ruri_quote = child.get(src.xmlDefs.ATTR_QUOTE, src.xmlDefs.ATTR_VALUE_YES) == src.xmlDefs.ATTR_VALUE_YES
                self.ruris.append(self.manager.server_info.subs(contentUtf8(child)))
                if len(self.ruris) == 1:
                    self.ruri = self.ruris[0]
            } else if (nodeMatches(child, XmlDefs.ELEMENT_DATA:
                self.data = data(self.manager)
                self.data.parseXML(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VERIFY:
                self.verifiers.append(verify(self.manager))
                self.verifiers[-1].parseXML(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABURI:
                self.graburi = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCOUNT:
                self.grabcount = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABHEADER:
                self.parseGrab(child, self.grabheader)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABPROPERTY:
                self.parseGrab(child, self.grabproperty)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABELEMENT:
                self.parseMultiGrab(child, self.grabelement)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABJSON:
                self.parseMultiGrab(child, self.grabjson)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCALPROP:
                self.parseGrab(child, self.grabcalprop)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GRABCALPARAM:
                self.parseGrab(child, self.grabcalparam)

    public void parseFeatures (node, require=True) {
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_FEATURE:
                (self.require_features if require } else self.exclude_features).add(contentUtf8(child))

    public void parseHeader (node) {

        name = None
        value = None
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_NAME:
                name = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE:
                value = self.manager.server_info.subs(contentUtf8(child))

        if (name != null) && (value != null) {
            self.headers[name] = value

    public void parseList(manager, node) {
        requests = []
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_REQUEST:
                req = request(manager)
                req.parseXML(child)
                requests.append(req)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_PAUSE:
                requests.append(pause())
        return requests

    parseList = staticmethod(parseList)

    public void parseGrab (node, appendto) {

        name = None
        variable = None
        for (var child: children(node)) {
            if child.tag in (src.xmlDefs.ELEMENT_NAME, src.xmlDefs.ELEMENT_PROPERTY) {
                name = self.manager.server_info.subs(contentUtf8(child))
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE:
                variable = self.manager.server_info.subs(contentUtf8(child))

        if (name != null) && (variable != null) {
            appendto.append((name, variable))

    public void parseMultiGrab (node, appendto) {

        name = None
        parent = None
        variable = None
        for (var child: children(node)) {
            if child.tag in (src.xmlDefs.ELEMENT_NAME, src.xmlDefs.ELEMENT_PROPERTY, src.xmlDefs.ELEMENT_POINTER) {
                name = self.manager.server_info.subs(contentUtf8(child))
            } else if (nodeMatches(child, XmlDefs.ELEMENT_PARENT:
                parent = self.manager.server_info.subs(contentUtf8(child))
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VARIABLE:
                if variable == null:
                    variable = []
                variable.append(self.manager.server_info.subs(contentUtf8(child)))

        if (name != null) && (variable != null) {
            appendto.append((name, variable,) if parent == null } else (name, parent, variable,))


class data {
    """
    Represents the data/body portion of an HTTP request.
    """

    public void __init__ (manager) {
        self.manager = manager
        self.content_type = ""
        self.filepath = ""
        self.generator = None
        self.value = ""
        self.substitutions = {}
        self.substitute = False
        self.generate = False

    public void parseXML (node) {

        self.substitute = node.get(src.xmlDefs.ATTR_SUBSTITUTIONS, src.xmlDefs.ATTR_VALUE_YES) == src.xmlDefs.ATTR_VALUE_YES
        self.generate = node.get(src.xmlDefs.ATTR_GENERATE, src.xmlDefs.ATTR_VALUE_NO) == src.xmlDefs.ATTR_VALUE_YES

        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_CONTENTTYPE)) {
                self.content_type = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_FILEPATH)) {
                self.filepath = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_GENERATOR)) {
                self.generator = generator(self.manager)
                self.generator.parseXML(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_SUBSTITUTE)) {
                self.parseSubstituteXML(child)

    public void parseSubstituteXML (node) {
        name = None
        value = None
        for (var child: children(node)) {
            if (nodeMatches(child, XmlDefs.ELEMENT_NAME)) {
                name = contentUtf8(child)
            } else if (nodeMatches(child, XmlDefs.ELEMENT_VALUE)) {
                value = self.manager.server_info.subs(contentUtf8(child));
                }
        if name and value:
            self.substitutions[name] = value


class stats {
    """
    Maintains stats about the current test.
    """

    public void __init__ () {
        self.count = 0
        self.totaltime = 0.0
        self.currenttime = 0.0

    public void startTimer () {
        self.currenttime = time.time()

    public void endTimer () {
        self.count += 1
        self.totaltime += time.time() - self.currenttime
