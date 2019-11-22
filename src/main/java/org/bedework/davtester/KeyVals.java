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
import java.util.Collections;
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
public class KeyVals extends HashMap<String, List<Object>> {
  public KeyVals() {
    super();
  }

  public KeyVals(final String key, final Object val) {
    super();

    put(key, val);
  }

  public String getOnlyString(final String key) {
    var val = get(key);
    if (Util.isEmpty(val)) {
      return null;
    }

    if (val.size() > 1) {
      throwException("Expected a single value");
    }

    return (String)val.get(0);
  }

  public List<String> getStrings(final String key) {
    var val = get(key);
    if (Util.isEmpty(val)) {
      return Collections.EMPTY_LIST;
    }

    var len = val.size();
    List<String> res = new ArrayList<>(len);
    ListIterator<String> di = res.listIterator();
    ListIterator si = val.listIterator();
    for (int i = 0; i < len; i++) {
      di.next();

      var snext = si.next();
      if (!(snext instanceof String)) {
        throwException("Not a string: " + snext);
      }
      di.set((String)snext);
    }

    return res;
  }

  public KeyVals addAll(final Map<String, Object> src) {
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
  public void put(final String name, final Object val) {
    List<Object> vals = new ArrayList<>(1);

    vals.add(val);

    put(name, vals);
  }
}
