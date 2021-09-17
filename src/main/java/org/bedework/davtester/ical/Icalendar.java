/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester.ical;

import org.bedework.util.misc.Util;
import org.bedework.util.timezones.Timezones;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.util.Strings;
import net.fortuna.ical4j.validate.ValidationException;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Comparator;
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
        final TimeZone tz = Timezones.getTz(timezone.getID());
        if (tz != null) {
          // Already three
          return;
        }

        if (localTzs == null) {
          localTzs = new HashMap<>();
        }

        localTzs.put(timezone.getID(), timezone);
      } catch (final Throwable t) {
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
        final TimeZone tz = Timezones.getTz(id);
        if (tz != null) {
          return  tz;
        }

        if (localTzs == null) {
          return null;
        }

        return localTzs.get(id);
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }

  public static final TimeZoneRegistry tzreg =
          TimeZoneRegistryFactory.getInstance().createRegistry();

  public Calendar cal;

  private Icalendar() {
    super("Icalendar");
  }

  public static Icalendar parseText(final String val) {
    final CalendarBuilder bldr =
            new CalendarBuilder(new CalendarParserImpl(),
                                TimeZoneRegistryFactory.getInstance().createRegistry());

    final Reader rdr = new StringReader(val);

    final UnfoldingReader ufrdr = new UnfoldingReader(rdr, true);

    final var ical = new Icalendar();

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

    final var res = new ComponentList<CalendarComponent>();

    for (final var comp: cal.getComponents()) {
      if (comp instanceof VTimeZone) {
        continue;
      }

      res.add(comp);
    }

    cal.getComponents().clear();
    cal.getComponents().addAll(res);
  }

  public CalendarComponent getMaster() {
    if (cal == null) {
      return null;
    }

    for (final var c: cal.getComponents()) {
      if (c.getProperty(Property.RECURRENCE_ID) == null) {
        return c;
      }
    }

    return null;
  }

  public CalendarComponent deriveComponent(final RecurrenceId recurrenceId) {
    final var master = getMaster();
    if (master == null) {
      return null;
    }

    try {
      final var newComp = (CalendarComponent)master.copy();

      removeProps(newComp, Property.RRULE);
      removeProps(newComp, Property.RDATE);
      removeProps(newComp, Property.EXRULE);
      removeProps(newComp, Property.EXDATE);
      removeProps(newComp, Property.RECURRENCE_ID);

      // Make a DTSTART from the recurrence id
      // If not a duration we need to make a DTEND

      final DtStart start =
              newComp.getProperty(Property.DTSTART);

      final DtEnd end =
              newComp.getProperty(Property.DTEND);

      long duration = 0L;
      if (end != null) {
        final var startTm = start.getDate().getTime();
        final var endTm = end.getDate().getTime();

        duration = endTm - startTm;
      }

      final DtStart newStart =
              new DtStart(recurrenceId.getParameters(),
                          recurrenceId.getDate());
      removeProps(newComp, Property.DTSTART);
      addProp(newComp, newStart);

      if (end != null) {
        final DtEnd newEnd =
                new DtEnd(recurrenceId.getParameters(),
                          new Date(recurrenceId.getDate().getTime() +
                                           duration));
        removeProps(newComp, Property.DTEND);
        addProp(newComp, newEnd);
      }

      addProp(newComp, recurrenceId);

      return newComp;
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  private class StartComparator implements Comparator<Component> {
    @Override
    public int compare(final Component c1, final Component c2) {
      final Date start1 = getStartDate(c1);
      final Date start2 = getStartDate(c2);

      return Util.cmpObjval(start1, start2);
    }
  }

  public void sort() {
    if (cal == null) {
      return;
    }

    cal.getComponents().sort(new StartComparator());
  }

  public void addComponent(final CalendarComponent c) {
    if (cal == null) {
      return;
    }

    cal.getComponents().add(c);
  }

  private Date getStartDate(final Component c) {
    final DtStart start =
            c.getProperty(Property.DTSTART);
    if (start != null) {
      return start.getDate();
    }

    return null;
  }

  private void removeProps(final Component c,
                           final String pname) {
    c.getProperties().removeIf(p -> p.getName().equals(pname));
  }

  private void addProp(final Component c,
                       final Property p) {
    c.getProperties().add(p);
  }

  public ComponentList<Component> getComponents() {
    final var res = new ComponentList<>();

    if (cal != null) {
      res.addAll(cal.getComponents());
    }

    return res;
  }

  public ComponentList<Component> getComponents(final String name) {
    final var res = new ComponentList<>();

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
