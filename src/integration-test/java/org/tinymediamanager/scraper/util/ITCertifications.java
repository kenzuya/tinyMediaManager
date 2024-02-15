package org.tinymediamanager.scraper.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaCertification;
import org.tinymediamanager.scraper.http.OnDiskCachedUrl;
import org.tinymediamanager.scraper.http.Url;

public class ITCertifications {

  private List<String> entries    = new ArrayList<>();
  private int          dupleCount = 0;

  @Test
  public void doAll() throws Exception {

    for (MediaCertification tmm : MediaCertification.values()) {
      String safeenum = tmm.name();
      CountryCode cc = tmm.getCountry();
      if (cc == null) {
        continue;
      }
      String cert = tmm.getName();
      String[] alt = tmm.getPossibleNotations();

      String nota = "\"" + String.join("\", \"", alt) + "\"";
      String javaenum = safeenum + "(CountryCode." + cc.getAlpha2() + ", \"" + cert + "\", new String[] { " + nota + " }),";
      // System.out.println(javaenum);
      entries.add(javaenum);
    }

    getMovieCerts();
    getTvShowCerts();
    getImdbCerts();
    System.out.println("Dupes found: " + dupleCount);

    Collections.sort(entries);
    entries.forEach(System.out::println);
  }

  @Test
  public void getMovieCerts() throws Exception {
    Url url = new OnDiskCachedUrl("https://en.wikipedia.org/wiki/Motion_picture_content_rating_system", 1, TimeUnit.HOURS);
    InputStream is = url.getInputStream();
    Document doc = Jsoup.parse(is, "UTF-8", "");
    Elements tables = doc.getElementsByClass("wikitable");
    Element table = tables.get(1); // 0 == guide table

    Elements rows = table.getElementsByTag("tr");
    String country = "";
    for (Element row : rows) {
      Elements ths = row.getElementsByTag("th");
      if (!ths.isEmpty()) {
        Elements as = ths.get(0).getElementsByTag("a");
        if (as.isEmpty()) {
          // System.err.println(row);
          continue;
        }
        Element a = as.get(0);
        if (!a.text().isEmpty()) {// quebec after Canada, but empty
          country = a.text();
        }
        // if (country.equals("Canada")) {
        // System.out.println("breakpoint");
        // }
      }

      Elements tds = row.getElementsByTag("td");
      for (Element td : tds) {
        String cert = td.text();
        cert = cert.replaceAll("\\(.*", "").strip();
        if (!cert.equals("N/A")) {
          print(country, cert);
        }
      }
    }
  }

  @Test
  public void getTvShowCerts() throws Exception {
    Url url = new OnDiskCachedUrl("https://en.wikipedia.org/wiki/Television_content_rating_system", 1, TimeUnit.HOURS);
    InputStream is = url.getInputStream();
    Document doc = Jsoup.parse(is, "UTF-8", "");
    Elements tables = doc.getElementsByClass("wikitable");
    Element table = tables.get(0);

    Elements rows = table.getElementsByTag("tr");
    String country = "";
    for (Element row : rows) {
      Elements ths = row.getElementsByTag("th");
      if (!ths.isEmpty()) {
        Elements as = ths.get(0).getElementsByTag("a");
        if (as.isEmpty()) {
          // System.err.println(row);
          continue;
        }
        Element a = as.get(0);
        if (!a.text().isEmpty()) {
          country = a.text();
          if (country.equals("Quebec")) {
            country = "Canada";// is no country, no code
          }
        }
        // if (country.equals("United States")) {
        // System.out.println("breakpoint");
        // }
      }

      Elements tds = row.getElementsByTag("td");
      Iterator<Element> iterator = tds.iterator(); // cannot determine if last with for/each
      while (iterator.hasNext()) {
        Element td = iterator.next();
        // omit the LAST column of time restrictions, but only if we have more than 2 columns
        if (iterator.hasNext() || tds.size() < 3) {
          String cert = td.text();
          cert = cert.replaceAll("\\(.*", "").strip();
          if (!cert.equals("N/A")) {
            print(country, cert);
          }
        }
      }
    }
  }

  @Test
  public void getImdbCerts() throws Exception {
    Url url = new OnDiskCachedUrl("https://help.imdb.com/article/contribution/titles/certificates/GU757M8ZJ9ZPXB39#", 1, TimeUnit.HOURS);
    InputStream is = url.getInputStream();
    Document doc = Jsoup.parse(is, "UTF-8", "");

    // no idea, how to filter the start tag better
    Element h2 = null;
    Elements h2s = doc.getElementsByTag("h2");
    for (Element tmp : h2s) {
      if (tmp.ownText().equals("Certificates by country")) {
        h2 = tmp.nextElementSibling().nextElementSibling().nextElementSibling();
      }
    }

    Elements strongs = h2.getElementsByTag("strong");
    for (Element strong : strongs) {
      String country = strong.text();
      if (country.length() < 3) {
        continue; // FIX: weird strong element at USA
      }
      country = country.replaceAll("\\(.*", "").strip();
      if (country.equals("UK")) {
        country = "United Kingdom"; // we do not have UK in our list
      }
      // if (country.equals("USA")) {
      // System.out.println("breakpoint");
      // }

      Elements lis = strong.parent().getElementsByTag("li");
      boolean first = true;
      for (Element li : lis) {
        if (first) { // skip first LI, which is header
          first = false;
          continue;
        }
        String cert = li.text();
        cert = cert.replaceAll("6- Suitable", "6 - Suitable"); // FIX: Sweden
        cert = cert.replaceAll(" - [\\(\\w]{2,}.*", ""); // split on " - " but only if there is TEXT behind (no number)
        cert = cert.replaceAll(" – [\\(\\w]{2,}.*", ""); // FIX: Spain, different dash!
        cert = cert.replaceAll(": .*", ""); // split on ": "
        cert = cert.replaceAll(" \\(No.*", ""); // FIX: Germany, no dash
        cert = cert.replaceAll("-Recommended.*", ""); // FIX: Philippines
        cert = cert.replaceAll("[\\(\\)]", ""); // FIX: France: remove parentheses
        cert = cert.replaceAll("Y or Y13.*", "Y"); // FIX: Fiji
        if (cert.equals("Note")) {
          continue; // FIX: Germany/Switzerland/USA
        }
        print(country, cert);
      }
    }
  }

  private void print(String country, String cert) {
    // System.out.println("Found: " + country + ":" + cert);
    Locale l = LanguageUtils.KEY_TO_COUNTRY_LOCALE_MAP.get(country.toLowerCase(Locale.ROOT));
    if (l != null) {
      cert = cert.replaceAll("[\"\\[\\]]", "");
      String safeenum = cert.replaceAll("[^A-Za-z0-9_]", "");
      String javaenum = l.getCountry() + "_" + safeenum + "(CountryCode." + l.getCountry() + ", \"" + cert + "\", new String[] { \"" + cert
          + "\" }),";
      if (entries.contains(javaenum)) {
        // System.err.println("dupe: " + l.getCountry() + "_" + safeenum);
        dupleCount++;
      }
      else {
        // System.out.println(javaenum);
        entries.add(javaenum);
      }
    }
    else {
      System.err.println("not found: " + country);
    }
  }
}
