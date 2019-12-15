/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.util.misc.ToString;

/**
 * User: mike Date: 12/15/19 Time: 10:53
 */
public class TestResult  extends RequestStats {
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

