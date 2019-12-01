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

/**
Class to manage the testing process.

from src.serverinfo import serverinfo
from xml.etree.cElementTree import ElementTree
from xml.parsers.expat import ExpatError
import getopt
import os
import random
import XmlDefs
import sys
import time

# Exceptions

 */

import org.bedework.davtester.observers.BaseResultsObserver;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
    Main class that runs test suites defined in an XML config file.
 */
public class Manager implements Logged {
  public static final int RESULT_OK = 0;
  public static final int RESULT_FAILED = 1;
  public static final int RESULT_ERROR = 2;
  public static final int RESULT_IGNORED = 3;

  // 1 for each of above
  public int[] totals = {0, 0, 0, 0};
  public long totalTime;

  public static final String EX_INVALID_CONFIG_FILE = "Invalid Config File";
  public static final String EX_FAILED_REQUEST = "HTTP Request Failed";

  public Serverinfo serverInfo = new Serverinfo();
  private String baseDir = "";
  public String dataDir;
  private Path dataDirPath;

  private String testsDir;
  private Path testsDirPath;

  Path pretestFile;
  private Caldavtest pretest;
  Path posttestFile;
  private Caldavtest posttest;
  private List<Caldavtest> tests = new ArrayList<>();
  private boolean textMode;
  private int pid;
  boolean memUsage;
  String randomSeed;
  private String digestCache;
  String postgresLog;
  String logFileName;
  FileWriter logFile;
  private Writer logFileWriter;

  private CloseableHttpClient httpClient;
  final CredentialsProvider credsProvider = new BasicCredentialsProvider();

  private List<BaseResultsObserver> observers = new ArrayList<>();
  private KeyVals results = new KeyVals();

  boolean stoponfail = false;
  public boolean printRequest = false;
  public boolean printResponse = false;
  boolean printRequestResponseOnError = false;

  public static class TestResult extends RequestStats {
    public int ok;
    public int failed;
    public int ignored;

    public void add(final TestResult tr) {
      ok += tr.ok;
      failed += tr.failed;
      ignored += tr.ignored;
    }

    public TestResult() {
    }

    public TestResult(final int ok,
                      final int failed,
                      final int ignored) {
      this.ok = ok;
      this.failed = failed;
      this.ignored = ignored;
    }

    public static TestResult ok() {
      return new TestResult(1, 0, 0);
    }

    public static TestResult failed() {
      return new TestResult(0, 1, 0);
    }

    public static TestResult ignored() {
      return new TestResult(0, 0, 1);
    }

    public String toString() {
      final var ts = new ToString(this);

      ts.append("ok", ok);
      ts.append("failed", failed);
      ts.append("ignored", ignored);

      ts.newLine();

      super.toStringSegment(ts);

      return ts.toString();
    }
  }

  public void Manager(final boolean textMode) {
    this.textMode = textMode;
  }

  public CloseableHttpClient getHttpClient() {
    // Need credentials provider?
    if (httpClient != null) {
      return httpClient;
    }

    final HttpClientBuilder clb = HttpClients.custom();

    credsProvider.setCredentials(
            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
            new UsernamePasswordCredentials(null,
                                            null));

    httpClient = clb.build();
    return httpClient;
  }

  public void setCredentials(final String user, final String pw) {
    credsProvider.setCredentials(
            new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
            new UsernamePasswordCredentials(user,
                                            pw));
  }

  public void setPretest(final String path) {
    pretestFile = normDataPath(path);
  }

  public void setPosttest(final String path) {
    posttestFile = normDataPath(path);
  }

  public void setDataDir(final String path) {
    dataDir = path;
    dataDirPath = Paths.get(path);
  }

  public Path normDataPath(final String path) {
    var p = Paths.get(path);

    Path np = null;
    try {
      np = dataDirPath.resolve(p);
    } catch (final Throwable t) {
      throwException("Unable to resolve path " + path +
                             " against " + dataDirPath +
                             " exception " + t);
    }

    return np;
  }

  public List<Path> normDataPaths(final List<String> path) {
    return path.stream().map(this::normDataPath).collect(Collectors.toList());
  }

  public void setTestsDir(final String path) {
    testsDir = path;
    testsDirPath = Paths.get(path);
  }

  public Path normTestsPath(final String path) {
    var p = Paths.get(path);

    Path np = null;
    try {
      np = testsDirPath.resolve(p);
    } catch (final Throwable t) {
      throwException("Unable to resolve path " + path +
                             " against " + testsDirPath +
                             " exception " + t);
    }

    return np;
  }

  public List<Path> normTestsPaths(final List<String> path) {
    return path.stream().map(this::normTestsPath).collect(Collectors.toList());
  }

  public boolean featureSupported(final String feature) {
    return serverInfo.features.contains(feature);
  }

  public void logit(final String str) {
    try {
      if (logFileWriter != null) {
        logFileWriter.write(str + "\n");
      }
      System.out.println(str);
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public Object getResults() {
    return results;
  }

  public void loadObserver(final String observerName) {
    try {
      var module = Util
              .getObject(observerName, BaseResultsObserver.class);

      var observer = (BaseResultsObserver)module;

      observer.init(this);

      observers.add(observer);
    } catch (final Throwable t) {
      throwException(t);
    }
  }

  public void message(final String message, final KeyVals args) {
    for (final BaseResultsObserver obs : observers) {
      obs.process(message, args);
    }
  }

  public void testProgress(final int count, final int total) {
    final KeyVals results = new KeyVals();
    results.put("count", count);
    results.put("total", total);

    message("testProgress", results);
  }

  public void load(final Path file,
                   final int current,
                   final int total) {
    final var kvs = new KeyVals("total", total);
    if (file != null) {
      kvs.put("name", file.toString());
    }
    kvs.put("current", current);
    kvs.put("total", total);

    message("load", kvs);
  }

  public void protocol(final String message) {
    message("protocol", new KeyVals("protocol", message));
  }

  public void trace(final String message) {
    message("trace", new KeyVals("message", message));
  }

  public KeyVals testFile(final String name,
                          final String details,
                          final Integer resultCode) {
    var res = new KeyVals();

    results.put("name", name);
    results.put("details", details);
    results.put("result", resultCode);
    results.put("tests", res);

    if (resultCode != null) {
      totals[resultCode]++;
    }
    message("testFile", res);
    return res;
  }

  public KeyVals testSuite(final KeyVals testfile,
                           final String name,
                           final String details,
                           final Integer resultCode) {
    var res = new KeyVals();

    testfile.put("name", name);
    testfile.put("details", details);
    testfile.put("result", resultCode);
    testfile.put("tests", res);

    if (resultCode != null) {
      totals[resultCode]++;
    }

    message("testSuite", testfile);
    return res;
  }

  public void testResult(final KeyVals testsuite,
                         final String name,
                         final String details,
                         final Integer resultCode,
                         final KeyVals addons) {
    testsuite.put("name", name);
    testsuite.put("details", details);
    testsuite.put("result", resultCode);

    if (addons != null) {
      testsuite.addAll(addons);
    }

    totals[resultCode]++;
    message("testResult", testsuite);
  }

  public void readXML(final String serverfile,
                      final List<Path> testfiles,
                      final boolean ssl,
                      final boolean all) {
    trace(format("Reading Server Info from \"%s\"",
                 serverfile));

    // Open and parse the server config file
    Document doc = null;
    try {
      doc = XmlUtils.parseXml(serverfile);
    } catch (final Throwable t) {
      error(format("Unable to parse file '%s' because: %s",
                   serverfile, t.getMessage()));

      throwException(t);
    }

    // Verify that top-level element is correct
    Element serverinfoNode = doc.getDocumentElement();

    if (!nodeMatches(serverinfoNode, XmlDefs.ELEMENT_SERVERINFO)) {
      throwException(EX_INVALID_CONFIG_FILE);
    }

    serverInfo.parseXML(serverinfoNode);

    // Setup ssl stuff
    serverInfo.ssl = ssl;
    if (ssl) {
      serverInfo.port = serverInfo.nonsslport;
      // HOST2 serverInfo.port2 = serverInfo.nonsslport2;
    } else {
      serverInfo.port = serverInfo.sslport;
      // HOST2 serverInfo.port2 = serverInfo.sslport2;
    }

    if (serverInfo.certdir != null) {
//        serverInfo.certdir = os.path
//                .join(base_dir, serverInfo.certdir)
    }

    final KeyVals moresubs = new KeyVals();
    moresubs.put("$host:", format("%s://%s", serverInfo.getScheme(), serverInfo.host));
    //HOST2 moresubs.put("$host2:", format("https://%s", serverInfo.host2));

    if ((ssl && (serverInfo.port != 443)) ||
            (!ssl && (serverInfo.port != 80))) {
      var val = moresubs.getOnlyString("$host:");
      moresubs.put("$host:",
                   val + format(":%d", serverInfo.port));
    }
    moresubs.put("$hostssl:",
                 format("https://%s", serverInfo.host));
    if (serverInfo.sslport != 443) {
      var val = moresubs.getOnlyString("$hostssl:");
      moresubs.put("$hostssl:",
                   val + format(":%d", serverInfo.sslport));
    }

    /*HOST2
    if ((ssl && (serverInfo.port2 != 443)) ||
            (!ssl && (serverInfo.port2 != 80))) {
      var val = moresubs.getOnlyString("$host2:");
      moresubs.put("$host2:",
                   val + format(":%d", serverInfo.port2));
    }
    moresubs.put("$hostssl2:",
                 format("https://%s", serverInfo.host2));
    if (serverInfo.sslport2 != 443) {
      var val = moresubs.getOnlyString("$hostssl2:");
      moresubs.put("$hostssl2:",
                   val + format(":%d", serverInfo.sslport2));
    }
    */

    serverInfo.addsubs(moresubs, null);

    var ctr = 1;

    for (var testfile : testfiles) {
      load(testfile, ctr, testfiles.size());
      ctr++;

      // Open and parse the config file
      var test = new Caldavtest(this, testfile, false);

      // ignore if all mode and ignore-all is set
      if (!all || !test.ignoreAll) {
        tests.add(test);
      }
    }

    if (pretestFile != null) {
      pretest = new Caldavtest(this, pretestFile, false);
    }
    if (posttestFile != null) {
      posttest = new Caldavtest(this, posttestFile, false);
    }

    load(null, ctr, testfiles.size());
  }

  public TestResult runAll() {
    message("start", null);

    var count = new TestResult();

    var ctr = 1;

    var res = new TestResult();
    res.startTimer();

    for (var test : tests) {
      ctr++;

      if (tests.size() > 1) {
        testProgress(ctr + 1, tests.size());
      }

      if (pretest != null) {
        var testResult = pretest.run();

        // Always stop the tests if the pretest fails
        if (testResult.failed != 0) {
          break;
        }
      }

      var testResult = test.run();

      res.add(testResult);

      if ((testResult.failed != 0) && stoponfail) {
        break;
      }

      if (posttest != null) {
        var postTestResult = posttest.run();

        // Always stop the tests if the posttest fails
        if (postTestResult.failed != 0) {
          break;
        }
      }
    }

    res.endTimer();
    totalTime = res.total;

    message("finish", null);

    if (logFile != null) {
      try {
        logFile.close();
      } catch (Throwable t) {
        throwException(t);
      }
    }

    return res;
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
/*
    public void getMemusage () {
          """

        @param pid: numeric pid of process to get memory usage for
        @type pid:  int
        @retrun:    tuple of (RSS, VSZ) values for the process
        """

          fd = os.popen("ps -l -p %d" % (pid, ))
          data = fd.read()
          lines = data.split("\n")
          procdata = lines[1].split()
          return int(procdata[6]), int(procdata[7])

          private String getDataPath (fpath){
          return os.path.join(data_dir, fpath) if data_dir } else
          fpath
        }
*/