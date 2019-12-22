/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester.ical;

import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.timezones.Timezones;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.util.Strings;
import net.fortuna.ical4j.validate.ValidationException;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.bedework.davtester.Utils.throwException;

/** Wrapper around ical4j representations.
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

  public static final TimeZoneRegistry tzreg =
          TimeZoneRegistryFactory.getInstance().createRegistry();

  private static class TimezoneGetter implements XcalUtil.TzGetter {

    @Override
    public TimeZone getTz(final String s) throws Throwable {
      return tzreg.getTimeZone(s);
    }
  }

  public static final TimezoneGetter tzGetter = new TimezoneGetter();

  public Calendar cal;

  private Icalendar() {
    super("Icalendar");
  }

  public static Icalendar parseText(final String val) {
    CalendarBuilder bldr =
            new CalendarBuilder(new CalendarParserImpl(),
                                TimeZoneRegistryFactory.getInstance().createRegistry());

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

  public void removeTimeZones() {
    if (cal == null) {
      return;
    }

    var res = new ComponentList<CalendarComponent>();

    for (var comp: cal.getComponents()) {
      if (comp instanceof VTimeZone) {
        continue;
      }

      res.add(comp);
    }

    cal.getComponents().clear();
    cal.getComponents().addAll(res);
  }

  public ComponentList<Component> getComponents() {
    var res = new ComponentList<>();

    if (cal != null) {
      res.addAll(cal.getComponents());
    }

    return res;
  }

  public ComponentList<Component> getComponents(final String name) {
    var res = new ComponentList<>();

    if (cal != null) {
      res.addAll(cal.getComponents(name));
    }

    return res;
  }

  public List<String> toLines() {
    return Arrays.asList(cal.toString().split(Strings.LINE_SEPARATOR));
  }

  @Override
  public void validate(final boolean recurse)
          throws ValidationException {
    throwException("Not implemented");
  }
}
