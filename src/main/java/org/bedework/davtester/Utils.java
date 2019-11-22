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

import org.bedework.util.misc.Util;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

public class Utils {
  public static String upperFirst(String val) {
    if ((val == null) || (val.length() == 0)) {
      return val;
    }

    var first = String.valueOf(Character.toUpperCase(val.charAt(0)));

    if (val.length() == 1) {
      return first;
    }

    return first + val.substring(1);
  }

  /** This can log the message
   *
   * @param message to display
   */
  public static void throwException(final String message) {
    throw new RuntimeException(message);
  }

  /** This can log the message
   *
   * @param t exception
   */
  public static void throwException(final Throwable t) {
    throw new RuntimeException(t);
  }

  public static class KeyValsPropertyFetcher implements Util.PropertyFetcher {
    private final KeyVals props;

    public KeyValsPropertyFetcher(KeyVals props) {
      this.props = props;
    }

    public String get(String name) {
      var val = this.props.get(name);

      if (Util.isEmpty(val)) {
        return null;
      }

      var val0 = val.get(0);
      if (val0 instanceof String) {
        return (String)val0;
      }
      return null;
    }
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }

  public static class DtParts {
    public int year;
    public int month;
    public int dayOfMonth;

    public int hour;
    public int minute;
    public int seconds;
  }

  private static Calendar calStart = new GregorianCalendar();

  public static DtParts getDtParts() {
    return getDtParts(calStart);
  }

  public static DtParts getDtParts(final int dayOffset) {
    final Calendar cal = (Calendar)calStart.clone();

    cal.add(Calendar.DAY_OF_YEAR, dayOffset);

    return getDtParts(cal);
  }

  public static DtParts getDtParts(final Date dt) {
    final Calendar cal = GregorianCalendar.getInstance();

    cal.setTime(dt);

    return getDtParts(cal);
  }

  public static DtParts getDtParts(final Calendar cal) {
    var res = new DtParts();

    res.year = cal.get(Calendar.YEAR);
    res.month = cal.get(Calendar.MONTH);
    res.dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

    res.hour = cal.get(Calendar.HOUR_OF_DAY);
    res.minute = cal.get(Calendar.MINUTE);
    res.seconds = cal.get(Calendar.SECOND);

    return res;
  }

  public static String fileToString(final String fileName) {
    final File f = new File(fileName);

    if (!f.exists()) {
      throwException("File " + fileName + " does not exist");
      return null;// fake
    }

    if (!f.isFile()) {
      throwException("" + fileName + " is not a file");
      return null;// fake
    }

    try (FileInputStream fis = new FileInputStream(f)) {
      return Util.streamToString(fis);
    } catch (final Throwable t) {
      throwException(t);
      return null;// fake
    }
  }
}

/*
public void processHrefSubstitutions(hrefs, prefix) {
    """
    Process the list of hrefs by prepending the supplied prefix. If the href is a
    list of hrefs, then prefix each item in the list and expand into the results. The
    empty string is represented by a single "-" in an href list.

    @param hrefs: list of URIs to process
    @type hrefs: L{list} of L{str}
    @param prefix: prefix to apply to each URI
    @type prefix: L{str}

    @return: resulting list of URIs
    @rtype: L{list} of L{str}
    """

    results = []
    for href in hrefs:
        if href.startsWith("[") {
            children = href[1:-1].split(",")
            results.extend([(prefix + (i if i != "-" } else "")).rstrip("/") for i in children if i])
        } else {
            results.append((prefix + href).rstrip("/"))

    return results
*/