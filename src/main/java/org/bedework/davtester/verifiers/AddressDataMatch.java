/*
// Copyright (c) 2006-2016 Apple Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.bedework.davtester.verifiers;

import org.bedework.davtester.KeyVals;
import org.bedework.davtester.vcard.Vcards;

import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.VCard;
import org.apache.http.Header;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Verifier that checks the response body for a semantic match to
 * VCARD data in a file.
 */
public class AddressDataMatch extends FileDataMatch {
  @Override
  public List<Integer> expectedStatus(final KeyVals args) {
    return Arrays.asList(200, 201, 207);
  }

  @Override
  public void compare(final URI uri,
                      final List<Header> responseHeaders,
                      final int status,
                      final String respdata,
                      final KeyVals args,
                      final String filepath,
                      final List<String> filters,
                      final String data) {
    var card = Vcards.parse(respdata);
    removePropertiesParameters(card, filters);
    var cardStr = card.toString();

    var dcard = Vcards.parse(data);
    removePropertiesParameters(dcard, filters);
    var dcardStr = dcard.toString();

    if (!card.equals(dcard)) {
      errorDiff(
              "        Response data does not exactly match file data%s",
              cardStr, dcardStr);
    }
  }

  private void removePropertiesParameters(final VCard card,
                                          final List<String> filters) {
    var plist = card.getProperties();
    var all = new ArrayList<>(plist);

    for (var property: all) {
      for (var filter: filters) {
        if (filter.contains(":")) {
          var split = filter.split(":");

          var propname = split[0];
          var parameter = split[1];
          if (pname(property).equals(propname)) {
            Parameter par = property.getExtendedParameter(parameter);

            if (par != null) {
              property.getParameters().remove(par);
            }
          }

          continue;
        }

        if (filter.contains("=")) {
          var split = filter.split("=");

          var propname = split[0];
          var value = split[1];

          if (pname(property).equals(propname) &&
                  (property.getValue().equals(value))) {
            plist.remove(property);
          }

          continue;
        }

        if (pname(property).equals(filter)) {
          plist.remove(property);
        }
      }
    }
  }

  private String pname(final Property p) {
    if (p.getId() == Property.Id.EXTENDED) {
      return p.getExtendedName();
    }

    return p.getId().name();
  }
}
