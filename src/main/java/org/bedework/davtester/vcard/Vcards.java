/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester.vcard;

import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import net.fortuna.ical4j.vcard.VCardBuilder;

import java.io.StringReader;

import static org.bedework.davtester.Utils.throwException;

/**
 * User: mike Date: 12/13/19 Time: 15:24
 */
public class Vcards {
  public static VCard parse(final String val) {
    final StringReader sr = new StringReader(val);

    final VCardBuilder bldr = new VCardBuilder(sr);

    try {
      return bldr.build();
    } catch (final Throwable t) {
      return throwException(t);
    }
  }

  private Property getProperty(final VCard card,
                               final String name) {
    Property.Id id;
    try {
      id = Property.Id.valueOf(name);
    } catch (final Throwable t) {
      id = null;
    }

    if (id != null) {
      return card.getProperty(id);
    }

    return card.getExtendedProperty(name);
  }
}
