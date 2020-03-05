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

import org.bedework.davtester.observers.BaseResultsObserver;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.bedework.davtester.Utils.throwException;
import static org.bedework.davtester.Utils.upperFirst;
import static org.bedework.util.xml.XmlUtil.nodeMatches;

/**
    Main class that runs test suites defined in an XML config file.
 */
public class Manager implements Logged {
  public static final int RESULT_OK = 0;
  public static final int RESULT_FAILED = 1;
  public static final int RESULT_ERROR = 2;
  public static final int RESULT_IGNORED = 3;

  public Globals globals;

  public TestResult totals = new TestResult();

  public static final String EX_INVALID_CONFIG_FILE = "Invalid Config File";
  public static final String EX_FAILED_REQUEST = "HTTP Request Failed";

  public Serverinfo serverInfo;

  private Path testsDirPath;

  public Path resDirPath;

  Path pretestFile;
  private Testfile pretest;
  Path posttestFile;
  private Testfile posttest;
  private List<Testfile> testFiles = new ArrayList<>();
  public Testfile currentTestfile;

  boolean memUsage;
  String postgresLog;
  FileWriter logFile;

  private CloseableHttpClient httpClient;
  final CredentialsProvider credsProvider = new BasicCredentialsProvider();

  private List<BaseResultsObserver> observers = new ArrayList<>();
  private KeyVals results = new KeyVals();

  /**
   * Call after settings are read.
   *
   * @return true for ok
   */
  public boolean init() {
    try {
      serverInfo = new Serverinfo(this,
                                  globals.getBasedir());

      if (!StringUtils.isEmpty(globals.getOutputName())) {
        logFile = new FileWriter(
                new File(subs(globals.getOutputName())));
      }

      if (globals.getDtds() != null) {
        XmlUtils.dtdPath = Paths.get(subs(globals.getDtds()));
      } else {
        XmlUtils.dtdPath = Paths.get("scripts/dtds");
      }

      setTestsDir(subs(globals.getTestsDir()));
      setResDir(subs(globals.getResDir()));

      if (globals.getPretest() != null) {
        setPretest(subs(globals.getPretest()));
      }

      if (globals.getPosttest() != null) {
        setPosttest(subs(globals.getPosttest()));
      }

      final List<String> testNames;

      if (globals.getAll()) {
        File f = new File(globals.getTestsDir());
        Path stDir = Paths.get(f.getAbsolutePath());

        testNames = new ArrayList<>();

        FileLister fl = new FileLister(testNames,
                                       globals.getExcludes(),
                                       subs(globals.getSubdir()));
        Files.walkFileTree(stDir, fl);
      } else {
        final var testsets = globals.getTestsets();
        final var tests = globals.getTests();
        if (testsets == null) {
          testNames = null;
        } else {
          testNames = new ArrayList<>();

          if (Util.isEmpty(tests)) {
            testNames.addAll(testsets.get(null));
          } else {
            for (final var nm : tests) {
              testNames.addAll(testsets.get(nm));
            }
          }
        }
      }

      // Randomize file list
      if (globals.getRandom() && !Util.isEmpty(testNames)) {
        Collections.shuffle(testNames);
      }

      if (Util.isEmpty(globals.getObservers())) {
        loadObserver("log");
      } else {
        for (var name : globals.getObservers()) {
          loadObserver(name);
        }
      }

      return readXML(subs(globals.getServerInfo()),
                     normTestsPaths(testNames),
                     globals.getSsl(),
                     globals.getAll());
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  public String subs(final String val) {
    return serverInfo.subs(val);
  }

  public CloseableHttpClient getUnauthHttpClient() {
    if (httpClient == null) {
      final HttpClientBuilder clb = HttpClients.custom();
      clb.setDefaultCredentialsProvider(credsProvider);
      httpClient = clb.build();
    }

    credsProvider.clear();

    return httpClient;
  }

  public CloseableHttpClient getHttpClient(final String user,
                                           final String pw) {
    if (httpClient == null) {
      final HttpClientBuilder clb = HttpClients.custom();
      clb.setDefaultCredentialsProvider(credsProvider);
      httpClient = clb.build();
    }

    String u = user;
    String p = pw;

    if (u == null) {
      u = serverInfo.user;
    }

    if (p== null) {
      p = serverInfo.pswd;
    }

    if (u != null) {
      credsProvider.setCredentials(
              new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
              new UsernamePasswordCredentials(u,
                                              p));
    }
    return httpClient;
  }

  public void setPretest(final String path) {
    pretestFile = normTestsPath(path);
  }

  public void setPosttest(final String path) {
    posttestFile = normTestsPath(path);
  }

  public Path normResPath(final String path) {
    var p = Paths.get(path);

    Path np;
    try {
      np = resDirPath.resolve(p);
    } catch (final Throwable t) {
      return throwException("Unable to resolve path " + path +
                                    " against " + resDirPath +
                                    " exception " + t);
    }

    return np.toAbsolutePath();
  }

  public void setTestsDir(final String path) {
    testsDirPath = Paths.get(path);
  }

  public void setResDir(final String path) {
    resDirPath = Paths.get(path);
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
      if (logFile != null) {
        logFile.write(str + "\n");
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
      String cname;
      if (observerName.contains(".")) {
        cname = observerName;
      } else {
        cname = "org.bedework.davtester.observers." + upperFirst(observerName);
      }
      var module = Util
              .getObject(cname,
                         BaseResultsObserver.class);

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
    results.put("tests", res);

    if (resultCode != null) {
      results.put("result", resultCode);
    }

    message("testFile", results);
    return res;
  }

  public KeyVals testSuite(final KeyVals testfile,
                           final String name,
                           final String details,
                           final Integer resultCode) {
    var res = new KeyVals();

    testfile.put("name", name);
    testfile.put("details", details);
    testfile.put("tests", res);

    if (resultCode != null) {
      testfile.put("result", resultCode);
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

    message("testResult", testsuite);
  }

  public void delay() {
    var delay = serverInfo.waitdelay;
    synchronized (this) {
      try {
        Thread.sleep(delay);
      } catch (final InterruptedException e) {
        throwException(e);
      }
    }
  }

  public boolean readXML(final String serverfile,
                         final List<Path> testfilePaths,
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

    if (doc == null) {
      error(format("Unable to parse file - probably not found '%s'",
                   serverfile));
      return false;
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

    for (var testfile : testfilePaths) {
      load(testfile, ctr, testfilePaths.size());
      ctr++;

      // Open and parse the config file
      var test = new Testfile(this, testfile, false);

      // ignore if all mode and ignore is set
      if (!all || !test.ignore) {
        testFiles.add(test);
      }
    }

    if (pretestFile != null) {
      pretest = new Testfile(this, pretestFile, false);
    }
    if (posttestFile != null) {
      posttest = new Testfile(this, posttestFile, false);
    }

    load(null, ctr, testfilePaths.size());
    return true;
  }

  public TestResult runAll() {
    message("start", null);

    var ctr = 1;

    totals.startTimer();

    for (var testFile: testFiles) {
      ctr++;

      if (testFiles.size() > 1) {
        testProgress(ctr + 1, testFiles.size());
      }

      if (pretest != null) {
        currentTestfile = pretest;
        var testResult = pretest.run();

        // Always stop the tests if the pretest fails
        if (testResult.failed != 0) {
          break;
        }
      }

      currentTestfile = testFile;
      var testResult = testFile.run();

      totals.add(testResult);

      if ((testResult.failed != 0) && globals.getStopOnFail()) {
        break;
      }

      if (posttest != null) {
        currentTestfile = posttest;
        var postTestResult = posttest.run();

        // Always stop the tests if the posttest fails
        if (postTestResult.failed != 0) {
          break;
        }
      }
    }

    totals.endTimer();

    message("finish", null);

    if (logFile != null) {
      try {
        logFile.close();
      } catch (Throwable t) {
        throwException(t);
      }
    }

    return totals;
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