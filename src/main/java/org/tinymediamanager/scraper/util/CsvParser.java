/*
 * Copyright 2012 - 2021 Manuel Laggner
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * the class {@link CsvParser} is a simple utility to parse CSV files (almost RFC4180 compatible, except double quote)
 *
 * @author Manuel Laggner
 */
public class CsvParser {
  private static final char DEFAULT_SEPARATOR  = ',';
  private static final char DOUBLE_QUOTES      = '"';
  private static final char DEFAULT_QUOTE_CHAR = DOUBLE_QUOTES;

  public List<String[]> readFile(File csvFile) throws Exception {
    return readFile(csvFile, 0);
  }

  public List<String[]> readFile(File csvFile, int skipLine) throws Exception {
    List<String[]> result = new ArrayList<>();
    int indexLine = 1;

    try (FileReader fileReader = new FileReader(csvFile); BufferedReader br = new BufferedReader(fileReader)) {

      String line;
      while ((line = br.readLine()) != null) {
        if (indexLine++ <= skipLine) {
          continue;
        }

        String[] csvLineInArray = parseLine(line);
        result.add(csvLineInArray);
      }
    }

    return result;
  }

  public String[] parseLine(String line) throws Exception {
    return parseLine(line, DEFAULT_SEPARATOR);
  }

  public String[] parseLine(String line, char separator) throws Exception {
    return parse(line, separator, DEFAULT_QUOTE_CHAR).toArray(String[]::new);
  }

  private List<String> parse(String line, char separator, char quoteChar) throws Exception {
    List<String> result = new ArrayList<>();

    boolean inQuotes = false;
    boolean isFieldWithEmbeddedDoubleQuotes = false;

    StringBuilder field = new StringBuilder();

    for (char c : line.toCharArray()) {
      if (c == DOUBLE_QUOTES) { // handle embedded double quotes ""
        if (isFieldWithEmbeddedDoubleQuotes) {
          if (field.length() > 0) { // handle for empty field like "",""
            field.append(DOUBLE_QUOTES);
            isFieldWithEmbeddedDoubleQuotes = false;
          }

        }
        else {
          isFieldWithEmbeddedDoubleQuotes = true;
        }
      }
      else {
        isFieldWithEmbeddedDoubleQuotes = false;
      }

      if (c == quoteChar) {
        inQuotes = !inQuotes;
      }
      else {
        if (c == separator && !inQuotes) { // if find separator and not in quotes, add field to the list
          result.add(field.toString());
          field.setLength(0); // empty the field and ready for the next
        }
        else {
          field.append(c); // else append the char into a field
        }
      }
    }

    // line done, what to do next?
    if (inQuotes) {
      // one tangling quote - take the result + that quote
      result.add(field.toString() + quoteChar);
    }
    else {
      result.add(field.toString()); // this is the last field
    }

    return result;
  }
}
