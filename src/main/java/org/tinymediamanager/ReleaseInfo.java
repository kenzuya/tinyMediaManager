/*
 * Copyright 2012 - 2024 Manuel Laggner
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
package org.tinymediamanager;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * The Class ReleaseInfo.
 *
 * @author Manuel Laggner
 */
public class ReleaseInfo {
  private static String version;
  private static String humanVersion;
  private static String build;
  private static String buildDate;

  static {
    try (InputStream inputStream = Files.newInputStream(Paths.get("version"))) { // NOSONAR
      Properties releaseInfoProp = new Properties();
      releaseInfoProp.load(inputStream);
      version = releaseInfoProp.getProperty("version");
      humanVersion = releaseInfoProp.getProperty("human.version");
      build = releaseInfoProp.getProperty("build");
      buildDate = releaseInfoProp.getProperty("date");
    }
    catch (IOException e) {
      try (InputStream inputStream = Files.newInputStream(Paths.get("target/classes/eclipse.properties"))) {
        Properties releaseInfoProp = new Properties();
        releaseInfoProp.load(inputStream);
        version = releaseInfoProp.getProperty("version");
        humanVersion = releaseInfoProp.getProperty("human.version");
        build = "git";
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        buildDate = formatter.format(new Date());
      }
      catch (IOException e2) {
        version = "";
        humanVersion = "";
        build = "git"; // no file - we must be in GIT
        buildDate = "";
      }
    }
  }

  private ReleaseInfo() {
    // hide contructor for utility classes
  }

  /**
   * Gets the version.
   *
   * @return the version
   */
  public static String getVersion() {
    String v = version;
    if (v.isEmpty()) {
      if (isNightly()) {
        v = "NIGHTLY";
      }
      else if (isPreRelease()) {
        v = "PRE-RELEASE";
      }
      else {
        v = "GIT";
      }
    }
    return v;
  }

  /**
   * Gets the builds the.
   *
   * @return the builds the
   */
  public static String getBuild() {
    return build;
  }

  /**
   * human readable version with tier (like 5.0-NIGHTLY)
   *
   * @return
   */
  public static String getHumanVersion() {
    return humanVersion;
  }

  /**
   * Gets the builds the date.
   *
   * @return the builds the date
   */
  public static String getBuildDate() {
    return buildDate;
  }

  // @formatter:off
  /*
    Specification-Title: tinyMediaManager
    Specification-Version: 5.0
    Specification-Vendor: tinyMediaManager
    Implementation-Title: tinyMediaManager
    Implementation-Version: 5.0-SNAPSHOT
    Implementation-Vendor: tinyMediaManager
    Main-Class: org.tinymediamanager.TinyMediaManager
    Build-By: Myron
    Build-Date: 2022-09-09 09:56
    Build-Nr: b45805a
    Human-Version: 5.0-NIGHTLY
  */
  // @formatter:on

  /**
   * are we nightly?
   *
   * @return true/false if nightly dev build
   */
  public static boolean isNightly() {
    return getBuild().equalsIgnoreCase("nightly") || humanVersion.toUpperCase(Locale.ROOT).contains("NIGHTLY");
  }

  /**
   * are we pre-release?
   *
   * @return true/false if nightly dev build
   */
  public static boolean isPreRelease() {
    return getBuild().equalsIgnoreCase("prerelease") || humanVersion.toUpperCase(Locale.ROOT).contains("PRERELEASE");
  }

  /**
   * are we a GIT version?
   *
   * @return true/false if GIT build
   */
  public static boolean isGitBuild() {
    return getBuild().equalsIgnoreCase("git") || humanVersion.toUpperCase(Locale.ROOT).contains("SNAPSHOT");
  }

  /**
   * are we on the release version?
   *
   * @return true/false if release build
   */
  public static boolean isReleaseBuild() {
    return !isNightly() && !isPreRelease() && !isGitBuild();
  }

  /**
   * gets the REAL version string out of the JAR file's manifest<br>
   * eg: 2.4 (r992)
   *
   * @return version string
   */
  public static String getRealVersion() {
    String v = getManifestEntry(ReleaseInfo.class, "Implementation-Version");
    if (v.isEmpty()) {
      // no manifest? only happens on git builds - get the version from special file
      v = getVersion() + " - GIT";
    }

    // replace SNAPSHOT wording
    v = v.replace("-SNAPSHOT", "");

    if (isNightly()) {
      v += " - NIGHTLY";
    }
    else if (isPreRelease()) {
      v += " - PRE-RELEASE";
    }

    return v;
  }

  /**
   * gets the REAL build date string out of the JAR file's manifest<br>
   * eg: 20130924-1832
   *
   * @return version string
   */
  public static String getRealBuildDate() {
    String b = getManifestEntry(ReleaseInfo.class, "Build-Date");
    if (b.isEmpty()) {
      b = getBuildDate(); // GIT, actual date
    }
    return b;
  }

  /**
   * gets manifest from JAR containing 'class'<br>
   * If found as 'file://' try to get from execution root<br>
   * returns <i>null</i> if not found.
   *
   * @param c
   *          the class file
   * @return value of manifest entry
   */
  public static Manifest getManifest(Class<?> c) {
    Manifest mf = null;
    try {
      String classname = "/" + c.getName().replaceAll("\\.", "/") + ".class";
      URL jarURL = c.getResource(classname);
      if (jarURL.getProtocol().equals("jar")) {
        JarURLConnection jurlConn = (JarURLConnection) jarURL.openConnection();
        mf = jurlConn.getManifest();
      }
      else if (jarURL.getProtocol().equals("file")) {
        // started from within eclipse or somehow as exploded
        // DO NOT get from classpath as we get the manifest of the first loaded JAR

        // base path is the filesystem root of the "classpath" (classes | test-classes)
        String basepath = jarURL.getPath().substring(0, jarURL.getPath().indexOf(classname));

        // assume there is already some generated manifest on filesystem
        mf = new Manifest(Files.newInputStream(Paths.get(basepath, "/META-INF/MANIFEST.MF")));
      }
    }
    catch (Exception e) {
      // do nothing
      mf = null;
    }
    return mf;
  }

  /**
   * gets specified manifest entry from JAR containing 'class'
   *
   * @param c
   *          the class file
   * @param entry
   *          the menifest entry
   * @return value of manifest entry
   */
  @SuppressWarnings("rawtypes")
  private static String getManifestEntry(Class c, String entry) {
    String s = "";
    try {
      Manifest mf = ReleaseInfo.getManifest(c);
      if (mf != null) {
        Attributes attr = mf.getMainAttributes();
        s = attr.getValue(entry);
      }
    }
    catch (Exception e) {
      // NPE if no manifest found
    }
    return s;
  }
}
