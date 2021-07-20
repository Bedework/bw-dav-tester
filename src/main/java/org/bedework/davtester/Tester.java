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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static org.bedework.davtester.Utils.throwException;

public class Tester extends DavTesterBase {

  public Tester() {
    super(new Manager());
  }

  public static void main(final String[] args) {
    System.setProperty(
            "net.fortuna.ical4j.timezone.cache.impl",
            MapTimeZoneCache.class.getName());
    final var tester = new Tester();
    if (tester.processArgs(args)) {
      tester.runTests();
    }
  }

  boolean processArgs(final String[] args) {
    var fnames = new ArrayList<String>();
    String testset = null;

    // Process single options
    try {
      final Args pargs = new Args(args);

      while (pargs.more()) {
//        if (pargs.ifMatch("-m")) {
//          manager.memUsage = true;
//          continue;
//        }

        if (pargs.ifMatch("--config")) {
          loadYaml(pargs.next());
          continue;
        }

        if (pargs.ifMatch("--testset")) {
          testset = pargs.next();
          continue;
        }

//        if (pargs.ifMatch("--pid")) {
//          // mem pidfile = pargs.next();
//          continue;
//        }

//        if (pargs.ifMatch("--postgres-log")) {
//          manager.postgresLog = pargs.next();
//          continue;
//        }

//        if (pargs.ifMatch("--random-seed")) {
//          manager.randomSeed = pargs.next();
//          continue;
//        }

        if (pargs.isMinusArg()) {
          System.err.println("Bad argument: " + pargs.current());
          return false;
        }

        // Treat as filename

        fnames.add(pargs.next());
      }

      setSystemProperties();

      if (manager.globals == null) {
        manager.globals = new Globals();
      }

      if (!Util.isEmpty(fnames)) {
        if (manager.globals.getTestsets() == null) {
          manager.globals.setTestsets(new HashMap<>());
        }

        manager.globals.getTestsets().put(null, fnames);
        manager.globals.setTests(null);
      }

      if (manager.globals.getHttptrace()) {
        httpTraceOn();
      }

      /* MEMUSAGE
      if (manager.memUsage) {
        fd = open(pidfile, "r");
        s = fd.read();
        pid = int(s);
      }
       */

      if (testset != null) {
        if (manager.globals.getTests() == null) {
          manager.globals.setTests(new ArrayList<>());
        }

        manager.globals.getTests().clear();
        manager.globals.getTests().add(testset);
      }

      if (!manager.init()) {
        warn("Unable to initialise: terminating");
        return false;
      }

      debug(manager.serverInfo.toString());
      return true;
    } catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private void loadYaml(final String fileName) {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    try {
      manager.globals = mapper.readValue(new File(fileName),
                                         Globals.class);

      System.out.println(manager.globals.toString());
    } catch (final Throwable t) {
      throwException(t);
    }
  }

  void runTests() {
    var result = manager.runAll();

    System.out.println(result.toString());
  }

  @Override
  public String getKind() {
    return "Testdav";
  }

  private static void setSystemProperties() {
    System.setProperty("ical4j.unfolding.relaxed", "true");
    System.setProperty("ical4j.parsing.relaxed", "true");
    System.setProperty("ical4j.compatibility.outlook", "true");
  }
}