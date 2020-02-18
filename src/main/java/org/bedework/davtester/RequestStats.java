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

import org.bedework.util.misc.ToString;

/**
 * Maintains stats about the current test.
 */
public class RequestStats {
  public int count;
  public long total;
  public long start;

  public void startTimer() {
    start = System.currentTimeMillis();
  }

  public void endTimer() {
    count += 1;
    total += System.currentTimeMillis() - start;
  }

  public void toStringSegment(final ToString ts) {
    ts.append("count", count);
    ts.append("total", total);
    ts.append("start", start);
  }
}
