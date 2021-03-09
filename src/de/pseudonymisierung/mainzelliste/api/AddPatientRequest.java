/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste.api;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class AddPatientRequest {

  private Map<String, String> fields;
  /** Linked HashMap of ArrayList */
  private Map<String, List<String>> ids;
  private boolean sureness;

  public Map<String, String> getFields() {
    return fields;
  }

  public Map<String, List<String>> getIds() {
    return ids;
  }

  public boolean isSureness() {
    return sureness;
  }

  public static AddPatientRequest fromJson(String jsonString) {
    return new GsonBuilder()
        .registerTypeAdapter(new TypeToken<Map<String, List<String>>>() {}.getType(),
            new MapOfStringListDeserializer())
        .create().fromJson(jsonString, AddPatientRequest.class);
  }

  private static class MapOfStringListDeserializer implements
      JsonDeserializer<Map<String, List<String>>> {

    @Override
    public Map<String, List<String>> deserialize(JsonElement elem, Type type,
        JsonDeserializationContext jsonDeserializationContext) {
      try {
        return elem.getAsJsonObject()
            .entrySet()
            .stream()
            // ignore empty arrays !
            .filter(e -> !e.getValue().isJsonArray() || e.getValue().getAsJsonArray().size() > 0)
            .collect(Collectors.toMap(Entry::getKey, e -> {
                  List<String> values = new ArrayList<>();
                  if (e.getValue().isJsonPrimitive()) { // handle string
                    try {
                      values.add(e.getValue().getAsString());
                    } catch (ClassCastException | IllegalStateException exec) {
                      throw new JsonParseException("Invalid ids object : the value of '" + e.getKey()
                          + "' must be a string");
                    }
                  } else if (e.getValue().isJsonArray()) { // handle string array
                    e.getValue().getAsJsonArray().forEach(v -> {
                      try {
                        values.add(v.getAsString());
                      } catch (ClassCastException | IllegalStateException exec) {
                        throw new JsonParseException("Invalid ids object : the value of '"
                            + e.getKey() + "' must be a string");
                      }
                    });
                  } else {
                    throw new JsonParseException("Invalid ids object : the value of '" + e.getKey()
                        + "' must be a string or a string array");
                  }
                  return values;
                }, (u, v) -> {
                  throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                LinkedHashMap::new));
      } catch (IllegalStateException e) {
        throw new JsonParseException("Invalid Json : ids must be a JSON Object");
      }
    }
  }
}
