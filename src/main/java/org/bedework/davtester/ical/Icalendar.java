/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester.ical;

import org.bedework.util.timezones.Timezones;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.validate.ValidationException;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;

import static org.bedework.davtester.Utils.throwException;

/** Wrapper around ical4j representations
 * User: mike Date: 11/27/19 Time: 00:10
 */
public class Icalendar extends Component {
  private static class TzReg implements TimeZoneRegistry {
    private HashMap<String, TimeZone> localTzs;

    @Override
    public void register(final TimeZone timezone) {
      try {
        TimeZone tz = Timezones.getTz(timezone.getID());
        if (tz != null) {
          // Already three
          return;
        }

        if (localTzs == null) {
          localTzs = new HashMap<>();
        }

        localTzs.put(timezone.getID(), timezone);
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }

    @Override
    public void register(final TimeZone timezone, final boolean update) {
      register(timezone);
    }

    @Override
    public void clear() {
      if (localTzs != null) {
        localTzs.clear();
      }
    }

    @Override
    public TimeZone getTimeZone(final String id) {
      try {
        TimeZone tz = Timezones.getTz(id);
        if (tz != null) {
          return  tz;
        }

        if (localTzs == null) {
          return null;
        }

        return localTzs.get(id);
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }

  private Calendar cal;

  public static Icalendar parseText(final String val) {
    CalendarBuilder bldr =
            new CalendarBuilder(new CalendarParserImpl(),
                                new TzReg());

    final Reader rdr = new StringReader(val);

    UnfoldingReader ufrdr = new UnfoldingReader(rdr, true);

    var ical = new Icalendar();

    try {
      ical.cal = bldr.build(ufrdr);
    } catch (final Throwable t) {
      throwException(t);
    }

    return ical;
  }

  public ComponentList<Component> getComponents() {
    var res = new ComponentList<>();

    if (cal != null) {
      res.addAll(cal.getComponents());
    }

    return res;
  }

  @Override
  public void validate(final boolean recurse)
          throws ValidationException {
    throwException("Not implemented");
  }
}
