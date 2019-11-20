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
import src.xmlDefs
import sys
import time

# Exceptions

EX_INVALID_CONFIG_FILE = "Invalid Config File"
EX_FAILED_REQUEST = "HTTP Request Failed"
 */

import org.bedework.davtester.observers.BaseResultsObserver;
import org.bedework.util.logging.Logged;
import org.bedework.util.misc.Util;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
    Main class that runs test suites defined in an XML config file.
 */
public class Manager implements Logged {
  public final int RESULT_OK = 0;
  public final int RESULT_FAILED = 1;
  public final int RESULT_ERROR = 2;
  public final int RESULT_IGNORED = 3;
  
  // 1 for each of above
  private int[] totals = {0, 0, 0, 0};

  private Serverinfo serverInfo = new Serverinfo();
  private String baseDir = "";
  private String dataDir;
  private String pretest;
  private String posttest;
  private List<String> tests = new ArrayList<>();
  private boolean textMode;
  private int pid;
  private String memUsage;
  private String randomSeed;
  private String digestCache;
  private String postgresLog;
  private String logFile;
  private Writer logFileWriter;
  
  private List<BaseResultsObserver> observers = new ArrayList<>();
  private Properties results = new Properties();
  
  private boolean stoponfail = false;
  private boolean printRequest = false;
  private boolean printResponse = false;
  private boolean printRequestResponseOnError = false;

  public void Manager(final boolean textMode) {
    this.textMode = textMode;
  }

  public void logit(final String str) {
    try {
      if (logFileWriter != null) {
        logFileWriter.write(str + "\n");
      }
      print(str);
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }
    
    public void loadObserver(final String observerName) {
      var module = Util.getObject(observerName, BaseResultsObserver.class);
      
      var observer = (BaseResultsObserver)module;
      
      observer.init(this);

      observers.add(observer);
    }

    public void message(final String message, final Properties args) {
      for (final BaseResultsObserver obs: observers) {
        obs.process(message, args);
      }
    }

    public void testProgress(final int count, final int total) {
      final Properties results = new Properties();
      results.put("count", count);
      results.put("total", total);

      message("testProgress", results);
    }

  public void trace(final String message) {
    final Properties results = new Properties();
    results.put("message", message);

    message("trace", results);
  }
    
    public void testFile (final String name, 
                          final String details, final Integer resultCode) {
      final Properties results = new Properties();

      results.put("name", name);
      results.put("details", details);
      results.put("result", result);
      // results.put("tests", []);

      if (resultCode != null) {
        totals[resultCode]++;
      }
      message("testFile", results);
      return results[-1]["tests"];
    }

    public void testSuite(final Properties testfile,
                          final String name,
                          final String details,
                          final Integer resultCode) {
      final Properties results = new Properties();

      results.put("name", name);
      results.put("details", details);
      results.put("result", result);
      // results.put("tests", []);

      if (resultCode != null) {
        totals[resultCode]++;
      }

      message("testSuite", testfile);
      return testfile[-1]["tests"];
    }

    public void testResult(final Properties testsuite,
                            final String name,
                            final String details,
                            final Integer resultCode, 
                            final Object addons) {
      final Properties results = new Properties();

      results.put("name", name);
      results.put("details", details);
      results.put("result", result);

      if (addons != null) {
        resultDetails.update(addons);
      }
      testsuite.addAll(resultDetails);
      totals[resultCode]++;
      message("testResult", testsuite);
    }

    public void readXML(final String serverfile,
                        final String testfiles,
                        final boolean ssl,
                        final boolean all, 
                        final List moresubs) {
      trace(String.format("Reading Server Info from \"%s\"",
                          serverfile));

      // Open and parse the server config file
      try {
        tree = ElementTree(file = serverfile)
      } catch (final Throwable t) {
        raise RuntimeError
        ("Unable to parse file '%s' because: %s" % (serverfile, e,))
      }

      // Verify that top-level element is correct
      serverinfo_node = tree.getroot()
      if (serverinfo_node.tag != src.xmlDefs.ELEMENT_SERVERINFO) {
        raise EX_INVALID_CONFIG_FILE
      }
      if (not len (serverinfo_node)){
        raise EX_INVALID_CONFIG_FILE;
      }

      serverInfo.parseXML(serverinfo_node)

      // Setup ssl stuff
      serverInfo.ssl = ssl;
      if (ssl) {
        serverInfo.port = serverInfo.nonsslport;
        serverInfo.port2 = serverInfo.nonsslport2;
      } else {
        serverInfo.port = serverInfo.sslport;
        serverInfo.port2 = serverInfo.sslport2;
      }

      if (serverInfo.certdir != null) {
//        serverInfo.certdir = os.path
//                .join(base_dir, serverInfo.certdir)
      }

      if (ssl) {
        moresubs["$host:"] = String.format("https://%s", serverInfo.host);
        moresubs["$host2:"] = String.format("https://%s", serverInfo.host2);
      } else {
        moresubs["$host:"] = String.format("http://%s", serverInfo.host);
        moresubs["$host2:"] = String.format("http://%s", serverInfo.host2);
      }

        if ((ssl && (serverInfo.port != 443)) || (!ssl && (serverInfo.port != 80))) {
          moresubs["$host:"] += String.format(":%d", serverInfo.port);
        }
        moresubs["$hostssl:"] = String.format("https://%s", serverInfo.host);
        if (serverInfo.sslport != 443) {
          moresubs["$hostssl:"] += String.format(":%d", serverInfo.sslport);
        }

        if ((ssl && (serverInfo.port2 != 443)) || (!ssl && (serverInfo.port2 != 80))) {
          moresubs["$host2:"] += String.format(":%d", serverInfo.port2);
        }
        moresubs["$hostssl2:"] = String.format("https://%s", serverInfo.host2);
        if (serverInfo.sslport2 != 443) {
          moresubs["$hostssl2:"] += String
                  .format(":%d", serverInfo.sslport2);
        }

        serverInfo.addsubs(moresubs)

        public void _loadFile(fname, ignore_root=True) {
            # Open and parse the config file
            try:
                tree = ElementTree(file=fname)
            except ExpatError, e:
                raise RuntimeError("Unable to parse file '%s' because: %s" % (fname, e,))
            caldavtest_node = tree.getroot()
            if caldavtest_node.tag != src.xmlDefs.ELEMENT_CALDAVTEST:
                if ignore_root:
                    message("trace", "Ignoring file \"{f}\" because it is not a test file".format(f=fname))
                    return None
                } else {
                    raise EX_INVALID_CONFIG_FILE
            if not len(caldavtest_node) {
                raise EX_INVALID_CONFIG_FILE

            message("Reading Test Details from \"{f}\"".format(f=fname))
            if base_dir:
                fname = fname[len(base_dir) + 1:]
            test = caldavtest (fname)
            test.parseXML(caldavtest_node)
            return test

        for ctr, testfile in enumerate(testfiles) {
            message("load", testfile, ctr + 1, len(testfiles))

            # Open and parse the config file
            test = _loadFile(testfile)
            if test == null:
                continue

            # ignore if all mode and ignore-all is set
            if !all|| !test.ignore_all:
                tests.append(test)

        if pretest != null:
            pretest = _loadFile(pretest, False)
        if posttest != null:
            posttest = _loadFile(posttest, False)

        message("load", None, ctr + 1, len(testfiles))

    public void readCommandLine () {
        sname = "scripts/server/serverinfo.xml"
        dname = "scripts/tests"
        fnames = []
        ssl = False
        all = False
        excludes = set()
        subdir = None
        pidfile = "../CalendarServer/logs/caldavd.pid"
        random_order = False
        random_seed = str(random.randint(0, 1000000))
        observer_names = []

        options, args = getopt.getopt(
            sys.argv[1:],
            "s:mo:x:",
            [
                "ssl",
                "all",
                "basedir=",
                "subdir=",
                "exclude=",
                "pretest=",
                "posttest=",
                "observer=",
                "pid=",
                "postgres-log=",
                "random",
                "random-seed=",
                "stop",
                "print-details-onfail",
                "always-print-request",
                "always-print-response",
                "debug"
            ],
        )

        # Process single options
        for option, value in options:
            if option == "-s":
                sname = value
            } else if (option == "-x":
                dname = value
            } else if (option == "--ssl":
                ssl = True
            } else if (option == "--all":
                all = True
            } else if (option == "--basedir":
                base_dir = value
                sname = os.path.join(base_dir, "serverinfo.xml")
                dname = os.path.join(base_dir, "tests")
                data_dir = os.path.join(base_dir, "data")

                # Also add parent to PYTHON path
                sys.path.append(os.path.dirname(base_dir))

            } else if (option == "--subdir":
                subdir = value + "/"
            } else if (option == "--exclude":
                excludes.add(value)
            } else if (option == "--pretest":
                pretest = value
            } else if (option == "--posttest":
                posttest = value
            } else if (option == "-m":
                memUsage = True
            } else if (option == "-o":
                logFile = open(value, "w")
            } else if (option == "--pid":
                pidfile = value
            } else if (option == "--observer":
                observer_names.append(value)
            } else if (option == "--postgres-log":
                postgresLog = value
            } else if (option == "--stop":
                stoponfail = True
            } else if (option == "--print-details-onfail":
                print_request_response_on_error = True
            } else if (option == "--always-print-request":
                print_request = True
            } else if (option == "--always-print-response":
                print_response = True
            } else if (option == "--random":
                random_order = True
            } else if (option == "--random-seed":
                random_seed = value
            } else if (option == "--debug":
                debug = True

        if all|| !args:
            files = []
            os.path.walk(dname, lambda arg, dir, names: files.extend([os.path.join(dir, name) for name in names]) if not dir.startsWith("test") } else None, None)
            for file in files:
                if file.endswith(".xml") && file[len(dname) + 1:] not in excludes:
                    if subdir == null|| file[len(dname) + 1:].startsWith(subdir) {
                        fnames.append(file)

        # Remove any server info file from files enumerated by --all
        fnames[:] = [x for x in fnames if (x != sname)]

        public void _normPath(f) {
            # paths starting with . or .. or /
            if f[0] in ('.', '/') {
                f = os.path.abspath(f)

                # remove unneeded leading path
                fsplit = f.split(dname)
                if 2 == len(fsplit) {
                    f = dname + fsplit[1]

            # relative paths
            } else {
                f = os.path.join(dname, f)
            return f

        # Process any file arguments as test configs
        for f in args:
            fnames.append(_normPath(f))

        if pretest != null:
            pretest = _normPath(pretest)
        if posttest != null:
            posttest = _normPath(posttest)

        # Randomize file list
        if random_order && len(fnames) > 1:
            random.seed(random_seed)
            random.shuffle(fnames)
            randomSeed = random_seed

        # Load observers
        map(lambda name: loadObserver(name), observer_names if observer_names } else ["log", ])

        readXML(sname, fnames, ssl, all)

        if memUsage:
            fd = open(pidfile, "r")
            s = fd.read()
            pid = int(s)

    public void runAll () {

        startTime = time.time()

        message("start")

        ok = 0
        failed = 0
        ignored = 0
        try:
            for ctr, test in enumerate(tests) {
                if len(tests) > 1:
                    testProgress(ctr + 1, len(tests))
                if pretest != null:
                    o, f, i = pretest.run()

                    # Always stop the tests if the pretest fails
                    if f != 0:
                        break

                o, f, i = test.run()
                ok += o
                failed += f
                ignored += i

                if failed != 0 && stoponfail:
                    break

                if posttest != null:
                    o, f, i = posttest.run()

                    # Always stop the tests if the posttest fails
                    if f != 0:
                        break

        except:
            failed += 1
            import traceback
            traceback.print_exc()

        endTime = time.time()

        timeDiff = endTime - startTime
        message("finish")

        if logFile != null:
            logFile.close()

        return failed, endTime - startTime

    public void getMemusage () {
        """

        @param pid: numeric pid of process to get memory usage for
        @type pid:  int
        @retrun:    tuple of (RSS, VSZ) values for the process
        """

        fd = os.popen("ps -l -p %d" % (pid,))
        data = fd.read()
        lines = data.split("\n")
        procdata = lines[1].split()
        return int(procdata[6]), int(procdata[7])

    public void getDataPath (fpath) {
        return os.path.join(data_dir, fpath) if data_dir } else fpath
