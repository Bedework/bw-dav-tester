/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.davtester;

import org.bedework.util.xml.tagdefs.AppleServerTags;
import org.bedework.util.xml.tagdefs.BedeworkServerTags;
import org.bedework.util.xml.tagdefs.CaldavDefs;
import org.bedework.util.xml.tagdefs.WebdavTags;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import static org.bedework.davtester.Utils.throwException;

/**
 * User: mike Date: 11/26/19 Time: 14:47
 */
public class Namespaces {
  private HashMap<String, String> abbrevToFull = new HashMap<>();
  private HashMap<String, String> fullToAbbrev = new HashMap<>();
  int nsCounter;

  private class NamespaceResolver implements NamespaceContext {
    public NamespaceResolver() {
    }

    //The lookup for the namespace uris is delegated to the stored document.
    public String getNamespaceURI(String prefix) {
      return abbrevToFull.get(prefix);
    }

    public String getPrefix(String namespaceURI) {
      return fullToAbbrev.get(namespaceURI);
    }

    @SuppressWarnings("rawtypes")
    public Iterator getPrefixes(String namespaceURI) {
      return null;
    }
  }

  public Namespaces() {
    try {
      addNs(WebdavTags.namespace, "DAV");
      addNs(CaldavDefs.caldavNamespace, "C");
      addNs(AppleServerTags.appleCaldavNamespace, "CSS");
      addNs(BedeworkServerTags.bedeworkCaldavNamespace, "BSS");
      addNs(BedeworkServerTags.bedeworkSystemNamespace, "BSYS");
    } catch (Throwable t) {
      throwException(t);
    }
  }

  public NamespaceContext getResolver() {
    return new NamespaceResolver();
  }

  public String getOrAdd(final String namespace) {
    try {
      var ns = fullToAbbrev.get(namespace);

      if (ns != null) {
        return ns;
      }

      var abbr = "n" + nsCounter;
      nsCounter++;

      addNs(namespace, abbr);

      return abbr;
    } catch (Throwable t) {
      return throwException(t);
    }
  }
  
  private void addNs(final String full, 
                     final String abbrev) {
    abbrevToFull.put(abbrev, full);
    fullToAbbrev.put(full, abbrev);
  }
}
