/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

/**
 * User: mike Date: 11/26/19 Time: 12:22
 */
public class Result<T> {
  public boolean ok = true;
  public String message;

  public T val;

  public Result() {
  }

  public Result(final T val) {
    this.val = val;
  }

  public static Result<?> ok() {
    return new Result<>();
  }

  public static Result<?> ok(final String val) {
    var res = new Result<>();
    res.message = val;

    return res;
  }

  public static <T extends Result<?>> T fail(final T res,
                                             final String message) {
    res.ok = false;
    res.message = message;

    return res;
  }

  public static <T extends Result<?>> T fail(final T res,
                                             final Result<?> from) {
    res.ok = false;
    res.message = from.message;

    return res;
  }
}
