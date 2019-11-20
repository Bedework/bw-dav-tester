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

import java.util.ArrayList;
import java.util.TreeSet;

public class Testcaldav {

  public static void main(final String[] args) {
    var sname = "scripts/server/serverinfo.xml";
    var dname = "scripts/tests";
    var fnames = new ArrayList<>();
    var ssl = false;
    var all = false;
    var excludes = new TreeSet<String>();
    String subdir = null;
    var pidfile = "../CalendarServer/logs/caldavd.pid";
    var randomOrder = false;
    //var random_seed = str(new Random.randint(0, 1000000));
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
          manager.logFile = pargs.next();
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
          base_dir = value
          sname = os.path.join(base_dir, "serverinfo.xml")
          dname = os.path.join(base_dir, "tests")
          data_dir = os.path.join(base_dir, "data")

          // Also add parent to PYTHON path
          sys.path.append(os.path.dirname(base_dir))
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
          warn("Bad argument: " + pargs.current());
          return;
        }

        // Treat as filename

        fnames.add(normPath(f));
      }

      if (all || !args) {
        files = []
        // os.path.walk(dname, lambda arg, dir, names: files.extend([os.path.join(dir, name) for name in names]) if not dir.startsWith("test") } else None, None);
        for (file:
             files) {
          if (file.endswith(".xml") &&
                  file[len(dname) + 1:]not in excludes){
            if ((subdir == null) ||
                    (file[len(dname) + 1:].startsWith(subdir))){
              fnames.append(file)
            }
          }
        }
      }

      // Remove any server info file from files enumerated by --all
      fnames[:] = [x for x in fnames if (x != sname)]

      if (manager.pretestFile != null) {
        manager.pretestFile = normPath(manager.pretestFile);
      }
      if (manager.posttestFile != null) {
        manager.posttestFile = normPath(manager.posttestFile);
      }

      // Randomize file list
      if (randomOrder && fnames.size() > 1) {
        random.seed(random_seed)
        random.shuffle(fnames)
        randomSeed = random_seed
      }

      // Load observers
      // DOTHIS map(lambda name: loadObserver(name), observer_names if observer_names } else ["log", ])

      manager.readXML(sname, fnames, ssl, all);

      if (manager.memUsage) {
        fd = open(pidfile, "r");
        s = fd.read();
        pid = int(s);
      }

      result, timing = manager.runAll();
      sys.exit(result);
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private String normPath(f) {
    // paths starting with . or .. or /
    if f[0] in('.', '/') {
      f = os.path.abspath(f)

      // remove unneeded leading path
      fsplit = f.split(dname)
      if 2 == len(fsplit) {
        f = dname + fsplit[1]

        // relative paths
      }else{
        f = os.path.join(dname, f)
      }
      return f
    }
  }
}