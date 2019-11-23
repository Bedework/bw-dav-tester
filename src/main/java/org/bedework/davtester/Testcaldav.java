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

import org.bedework.util.args.Args;
import org.bedework.util.misc.Util;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class Testcaldav {

  public static void main(final String[] args) {
    var sname = "scripts/server/serverinfo.xml";
    var dname = "scripts/tests";
    var fnames = new ArrayList<String>();
    var ssl = false;
    var all = false;
    var excludes = new TreeSet<String>();
    String subdir = null;
    String baseDir = null;
    var pidfile = "../CalendarServer/logs/caldavd.pid";
    var randomOrder = false;
    //var random_seed = String.valueOf(new Random.randint(0, 1000000));
    var observerNames = new ArrayList<String>();

    /*
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
        )*/

    // Process single options
    try {
      final Args pargs = new Args(args);
      final Manager manager = new Manager();

      while (pargs.more()) {
        if (pargs.ifMatch("-m")) {
          manager.memUsage = true;
          continue;
        }

        if (pargs.ifMatch("-o")) {
          manager.logFileName = pargs.next();
          manager.logFile = new FileWriter(new File(manager.logFileName));
          continue;
        }

        if (pargs.ifMatch("-s")) {
          sname = pargs.next();
          continue;
        }

        if (pargs.ifMatch("-x")) {
          dname = pargs.next();
          continue;
        }

        if (pargs.ifMatch("--ssl")) {
          ssl = true;
          continue;
        }

        if (pargs.ifMatch("--all")) {
          all = true;
          continue;
        }

        if (pargs.ifMatch("--basedir")) {
          baseDir = pargs.next();
          sname = Util.buildPath(false, baseDir, "/serverinfo.xml");
          dname = Util.buildPath(true, baseDir, "tests");
          manager.dataDir = Util.buildPath(true, baseDir, "data");

          continue;
        }

        if (pargs.ifMatch("--subdir")) {
          subdir = pargs.next() + "/";
          continue;
        }

        if (pargs.ifMatch("--exclude")) {
          excludes.add(pargs.next());
          continue;
        }

        if (pargs.ifMatch("--pretest")) {
          manager.pretestFile = pargs.next();
          continue;
        }

        if (pargs.ifMatch("--posttest")) {
          manager.posttestFile = pargs.next();
          continue;
        }

        if (pargs.ifMatch("--pid")) {
          pidfile = pargs.next();
          continue;
        }

        if (pargs.ifMatch("--observer")) {
          observerNames.add(pargs.next());
          continue;
        }

        if (pargs.ifMatch("--postgres-log")) {
          manager.postgresLog = pargs.next();
          continue;
        }

        if (pargs.ifMatch("--stop")) {
          manager.stoponfail = true;
          continue;
        }

        if (pargs.ifMatch("--print-details-onfail")) {
          manager.printRequestResponseOnError = true;
          continue;
        }

        if (pargs.ifMatch("--always-print-request")) {
          manager.printRequest = true;
          continue;
        }

        if (pargs.ifMatch("--always-print-response")) {
          manager.printResponse = true;
          continue;
        }

        if (pargs.ifMatch("--random")) {
          randomOrder = true;
          continue;
        }

        if (pargs.ifMatch("--random-seed")) {
          manager.randomSeed = pargs.next();
          continue;
        }

        if (pargs.isMinusArg()) {
          //warn("Bad argument: " + pargs.current());
          return;
        }

        // Treat as filename

        fnames.add(normPath(pargs.next()));
      }

      if (sname == null) {
        excludes.add("serverinfo.xml");
      }

      if (all) {
        File f = new File(dname);
        Path stDir = Paths.get(f.getAbsolutePath());
        FileLister fl = new FileLister(fnames, excludes, subdir);
        Files.walkFileTree(stDir, fl);
      }

      if (manager.pretestFile != null) {
        manager.pretestFile = normPath(manager.pretestFile);
      }
      if (manager.posttestFile != null) {
        manager.posttestFile = normPath(manager.posttestFile);
      }

      // Randomize file list
      if (randomOrder && fnames.size() > 1) {
        Collections.shuffle(fnames);
      }

      // Load observers
      // DOTHIS map(lambda name: loadObserver(name), observer_names if observer_names } else ["log", ])

      manager.readXML(sname, fnames, ssl, all, null);

      /* MEMUSAGE
      if (manager.memUsage) {
        fd = open(pidfile, "r");
        s = fd.read();
        pid = int(s);
      }
       */

      var result = manager.runAll();

      System.out.println(result.toString());
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private static String normPath(final String path) {
    File f = new File(path);

    return f.getAbsolutePath();
  }
}