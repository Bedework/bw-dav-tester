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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static org.bedework.davtester.Utils.throwException;

/** Parameters from the XML specifications (tests and config) or
 * from resulting data and responses.
 *
 * User: mike Date: 11/21/19 Time: 17:54
 */
public class KeyVals extends HashMap<String, List<?>> {
  public KeyVals() {
    super();
  }

  public KeyVals(final Map<String, List<?>> val) {
    super(val);
  }

  public KeyVals(final String key, final String val) {
    super();

    put(key, val);
  }

  public KeyVals(final String key, final Integer val) {
    super();

    put(key, val);
  }

  public Object getOnly(final String key) {
    var val = get(key);
    if (Util.isEmpty(val)) {
      return null;
    }

    if (val.size() > 1) {
      throwException("Expected a single value");
    }

    return val.get(0);
  }

  public String getOnlyString(final String key) {
    return (String)getOnly(key);
  }

  public Integer getInt(final String key) {
    var val = getOnly(key);
    if (val == null) {
      return null;
    }

    if (val instanceof Integer) {
      return (Integer)val;
    }

    return Integer.parseInt((String)val);
  }

  public int getOnlyInt(final String key) {
    var val = getOnly(key);
    if (val instanceof Integer) {
      return (Integer)val;
    }

    return Integer.parseInt((String)val);
  }

  public boolean getOnlyBool(final String key) {
    if (!containsKey(key)) {
      return false;
    }

    var val = getOnly(key);
    if (val instanceof Boolean) {
      return (Boolean)val;
    }

    return Boolean.parseBoolean((String)val);
  }

  public List<String> getStrings(final String key,
                                 final String... defaults) {
    var val = get(key);
    if (Util.isEmpty(val)) {
      if (defaults.length == 0) {
        return new ArrayList<>();
      }

      return Arrays.asList(defaults);
    }

    var len = val.size();
    List<String> res = new ArrayList<>(len);
    ListIterator si = val.listIterator();
    for (int i = 0; i < len; i++) {
      var snext = si.next();
      if (!(snext instanceof String)) {
        throwException("Not a string: " + snext);
      }
      res.add((String)snext);
    }

    return res;
  }

  public List<Integer> getInts(final String key,
                               final Integer... defaults) {
    var val = get(key);
    if (Util.isEmpty(val)) {
      if (defaults.length == 0) {
        return new ArrayList<>();
      }

      return Arrays.asList(defaults);
    }

    var len = val.size();
    List<Integer> res = new ArrayList<>(len);
    ListIterator si = val.listIterator();
    for (int i = 0; i < len; i++) {
      var snext = si.next();
      if (!(snext instanceof Integer)) {
        res.add(Integer.parseInt((String)snext));
      } else {
        res.add((Integer)snext);
      }
    }

    return res;
  }

  public KeyVals addAll(final KeyVals src) {
    for (var nm: src.keySet()) {
      var val = src.get(nm);
      if (val == null) {
        remove(nm);
        continue;
      }

      put(nm, val);
    }

    return this;
  }

  /**
   *
   * @param val single string to set or replace current value
   */
  public void put(final String name, final String val) {
    if (val == null) {
      return;
    }
    List<String> vals = new ArrayList<>(1);

    vals.add(val);

    super.put(name, vals);
  }

  /**
   *
   * @param val single Integer to set or replace current value
   */
  public void put(final String name, final Integer val) {
    if (val == null) {
      return;
    }
    List<Integer> vals = new ArrayList<>(1);

    vals.add(val);

    super.put(name, vals);
  }

  /**
   *
   * @param val single Integer to set or replace current value
   */
  public void put(final String name, final Long val) {
    if (val == null) {
      return;
    }
    List<Long> vals = new ArrayList<>(1);

    vals.add(val);

    super.put(name, vals);
  }

  /**
   *
   * @param val single Integer to set or replace current value
   */
  public void put(final String name, final KeyVals val) {
    if (val == null) {
      return;
    }
    List<KeyVals> vals = new ArrayList<>(1);

    vals.add(val);

    super.put(name, vals);
  }
}
