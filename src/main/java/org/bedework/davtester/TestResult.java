/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.util.misc.ToString;

/**
 * User: mike Date: 12/15/19 Time: 10:53
 */
public class TestResult  extends RequestStats {
  public int tests;
  public int ok;
  public int failed;
  public int ignored;
  public int errorSkipped;
  public int errors;

  public void add(final TestResult tr) {
    tests += tr.tests;
    ok += tr.ok;
    failed += tr.failed;
    ignored += tr.ignored;
    errorSkipped += tr.errorSkipped;
    errors += tr.errors;
  }

  public TestResult() {
  }

  public TestResult(final int tests,
                    final int ok,
                    final int failed,
                    final int ignored,
                    final int errors) {
    this.tests = tests;
    this.ok = ok;
    this.failed = failed;
    this.ignored = ignored;
    this.errors = errors;
  }

  public static TestResult ok() {
    return new TestResult(1, 1, 0, 0, 0);
  }

  public static TestResult failed() {
    return new TestResult(1, 0, 1, 0, 0);
  }

  public static TestResult ignored() {
    return new TestResult(1, 0, 0, 1, 0);
  }

  public static TestResult error() {
    return new TestResult(1, 0, 0, 0, 1);
  }

  public String toString() {
    final var ts = new ToString(this);

    ts.append("test", tests);
    ts.append("ok", ok);
    ts.append("failed", failed);
    ts.append("ignored", ignored);
    ts.append("errors", errors);
    ts.append("skipped in error", errorSkipped);

    ts.newLine();

    super.toStringSegment(ts);

    return ts.toString();
  }
}

