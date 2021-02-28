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
package org.tinymediamanager.core;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.scraper.http.Url;
import org.tinymediamanager.scraper.util.StrgUtils;
import org.tinymediamanager.scraper.util.UrlUtil;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * The Class ImageCache - used to build a local image cache (scaled down versions & thumbnails - also for offline access).
 * 
 * @author Manuel Laggner
 */
public class ImageCache {
  private static final Logger LOGGER     = LoggerFactory.getLogger(ImageCache.class);
  private static final Path   CACHE_DIR  = Paths.get(Globals.CACHE_FOLDER + "/image");
  private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

  public enum CacheSize {
    SMALL,
    @JsonEnumDefaultValue
    BIG,
    ORIGINAL
  }

  public enum CacheType {
    BALANCED,
    @JsonEnumDefaultValue
    QUALITY,
    ULTRA_QUALITY
  }

  static {
    createSubdirs();
  }

  private ImageCache() {
    throw new IllegalAccessError();
  }

  public static void createSubdirs() {
    if (!Files.exists(CACHE_DIR)) {
      try {
        Files.createDirectories(CACHE_DIR);
      }
      catch (IOException e) {
        LOGGER.warn("Could not create cache dir {} - {}", CACHE_DIR, e.getMessage());
      }
    }

    for (char sub : HEX_DIGITS) {
      try {
        Path p = CACHE_DIR.resolve(Character.toString(sub));
        Files.createDirectories(p);
      }
      catch (FileAlreadyExistsException ignore) {
        // do not care
      }
      catch (IOException e) {
        LOGGER.warn("Could not create cache sub dir '{}' - {}", sub, e.getMessage());
      }
    }
  }

  /**
   * Gets the cache dir. If it is not on the disk - it will also create it
   * 
   * @return the cache dir
   */
  public static Path getCacheDir() {
    return CACHE_DIR;
  }

  /**
   * Gets the file name (MD5 hash) of the cached file.
   * 
   * @param path
   *          the url
   * @return the cached file name
   */
  public static String getMD5(String path) {
    try {
      if (path == null) {
        return null;
      }
      // now uses a simple md5 hash, which should have a fairly low collision
      // rate, especially for our limited use
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] key = md.digest(path.getBytes());
      return StrgUtils.bytesToHex(key);
    }
    catch (Exception e) {
      LOGGER.debug("Failed to create cached filename for image: {} - {}", path, e);
    }
    return "";
  }

  public static String getMD5WithSubfolder(String path) {
    String md5 = getMD5(path);
    if (StringUtils.isBlank(md5)) {
      return null;
    }

    return Paths.get(md5.substring(0, 1), md5).toString();
  }

  /**
   * Cache image without overwriting an existing one
   * 
   * @param mediaFile
   *          the media file
   * @return the file the cached file
   * @throws Exception
   *           any exception occurred while caching
   */
  public static Path cacheImage(MediaFile mediaFile) throws Exception {
    return cacheImage(mediaFile, false);
  }

  /**
   * Cache image.
   *
   * @param mediaFile
   *          the media file
   * @param overwrite
   *          indicator if we should overwrite any existing files
   * @return the file the cached file
   * @throws Exception
   *           any exception occurred while caching
   */
  private static Path cacheImage(MediaFile mediaFile, boolean overwrite) throws Exception {
    if (!mediaFile.isGraphic()) {
      throw new InvalidFileTypeException(mediaFile.getFileAsPath());
    }

    Path originalFile = mediaFile.getFileAsPath();
    Path cachedFile = ImageCache.getCacheDir().resolve(getMD5WithSubfolder(originalFile.toString()) + "." + Utils.getExtension(originalFile));
    if (overwrite || !Files.exists(cachedFile)) {
      // check if the original file exists && size > 0
      if (!Files.exists(originalFile)) {
        throw new FileNotFoundException("unable to cache file: " + originalFile + "; file does not exist");
      }
      if (Files.size(originalFile) == 0) {
        throw new EmptyFileException(originalFile);
      }

      // recreate cache dir if needed
      // rescale & cache
      BufferedImage originalImage = null;

      // try to cache the image file
      // we have up to 5 retries here if we hit the memory cap since we are hitting the machine hard due to multi CPU image caching
      int retries = 5;
      do {
        try {
          originalImage = ImageUtils.createImage(originalFile);
          break;
        }
        catch (OutOfMemoryError e) {
          // memory limit hit; give it another 500ms time to recover
          LOGGER.debug("hit memory cap: {}", e.getMessage());
          Thread.sleep(500);
        }
        retries--;
      } while (retries > 0);

      if (originalImage == null) {
        throw new IOException("could not open original image to scale; probably due to memory limits");
      }

      // calculate width based on MF type
      int desiredWidth = calculateCacheImageWidth(originalImage);

      Point size = ImageUtils.calculateSize(desiredWidth, originalImage.getHeight(), originalImage.getWidth(), originalImage.getHeight(), true);
      BufferedImage scaledImage = null;

      // we have up to 5 retries here if we hit the memory cap since we are hitting the machine hard due to multi CPU image caching
      retries = 5;
      do {
        try {
          switch (Globals.settings.getImageCacheType()) {
            case BALANCED:
              // scale fast
              scaledImage = Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.FIT_EXACT, size.x, size.y);
              break;

            case QUALITY:
              // scale with good quality
              scaledImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, size.x, size.y);
              break;

            case ULTRA_QUALITY:
              // scale with good quality
              scaledImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, size.x, size.y);
              break;
          }
          break;
        }
        catch (OutOfMemoryError e) {
          // memory limit hit; give it another 500ms time to recover
          LOGGER.debug("hit memory cap: {}", e.getMessage());
          Thread.sleep(500);
        }
        retries--;
      } while (retries > 0);

      originalImage.flush();

      if (scaledImage == null) {
        throw new IOException("could not scale image; probably due to memory limits");
      }

      ImageWriter imgWrtr = null;
      ImageWriteParam imgWrtrPrm = null;

      // here we have two different ways to create our thumb
      // a) a scaled down jpg/png (without transparency) which we have to modify since OpenJDK cannot call native jpg encoders
      // b) a scaled down png (with transparency) which we can store without any more modifying as png
      if (ImageUtils.hasTransparentPixels(scaledImage)) {
        // transparent image -> png
        imgWrtr = ImageIO.getImageWritersByFormatName("png").next();
        imgWrtrPrm = imgWrtr.getDefaultWriteParam();

      }
      else {
        // non transparent image -> jpg
        // convert to rgb
        BufferedImage rgb = new BufferedImage(scaledImage.getWidth(), scaledImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        ColorConvertOp xformOp = new ColorConvertOp(null);
        xformOp.filter(scaledImage, rgb);
        imgWrtr = ImageIO.getImageWritersByFormatName("jpg").next();
        imgWrtrPrm = imgWrtr.getDefaultWriteParam();
        imgWrtrPrm.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imgWrtrPrm.setCompressionQuality(0.80f);

        scaledImage = rgb;
      }

      FileImageOutputStream output = new FileImageOutputStream(cachedFile.toFile());
      imgWrtr.setOutput(output);
      IIOImage image = new IIOImage(scaledImage, null, null);
      imgWrtr.write(null, image, imgWrtrPrm);
      imgWrtr.dispose();
      output.flush();
      output.close();

      scaledImage.flush();

      if (!Files.exists(cachedFile)) {
        throw new IOException("unable to cache file: " + originalFile);
      }
    }

    return cachedFile;
  }

  private static int calculateCacheImageWidth(BufferedImage originalImage) {
    // initialize with the original width
    int desiredWidth = originalImage.getWidth();

    switch (Settings.getInstance().getImageCacheSize()) {
      case ORIGINAL:
        // nothing to do - desiredWidth already initialized
        break;

      case BIG:
        // decide the scale-side depending on the aspect ratio
        if (((float) originalImage.getWidth()) / ((float) originalImage.getHeight()) > 1) {
          // landscape
          if (originalImage.getWidth() > 1000) {
            desiredWidth = 1000;
          }
        }
        else {
          // portrait
          if (originalImage.getHeight() > 1000) {
            desiredWidth = 1000 * originalImage.getWidth() / originalImage.getHeight();
          }
        }
        break;

      case SMALL:
        // decide the scale-side depending on the aspect ratio
        if (((float) originalImage.getWidth()) / ((float) originalImage.getHeight()) > 1) {
          // landscape
          if (originalImage.getWidth() > 400) {
            desiredWidth = 400;
          }
        }
        else {
          // portrait
          if (originalImage.getHeight() > 400) {
            desiredWidth = 400 * originalImage.getWidth() / originalImage.getHeight();
          }
        }
        break;

    }

    return desiredWidth;
  }

  /**
   * Cache image silently without throwing an exception. Use the method {@link #cacheImageSilently(MediaFile)} if possible!
   *
   * @param path
   *          the path to this image
   */
  public static void cacheImageSilently(Path path) {
    cacheImageSilently(new MediaFile(path));
  }

  /**
   * Cache image silently without throwing an exception.
   *
   * @param mediaFile
   *          the media file
   */
  public static void cacheImageSilently(MediaFile mediaFile) {
    if (!Settings.getInstance().isImageCache()) {
      return;
    }

    if (!mediaFile.isGraphic()) {
      return;
    }

    try {
      cacheImage(mediaFile, true);
    }
    catch (Exception e) {
      LOGGER.debug("could not cache image: {}", e.getMessage());
    }
  }

  /**
   * Invalidate cached image. Use the method {@link #invalidateCachedImage(MediaFile)} is possible!
   *
   * @param path
   *          the path to this image
   */
  public static void invalidateCachedImage(Path path) {
    invalidateCachedImage(new MediaFile(path));
  }

  /**
   * Invalidate cached image.
   * 
   * @param mediaFile
   *          the media file
   */
  public static void invalidateCachedImage(MediaFile mediaFile) {
    if (!mediaFile.isGraphic()) {
      return;
    }

    Path path = mediaFile.getFileAsPath();
    Path cachedFile = getCacheDir().resolve(ImageCache.getMD5WithSubfolder(path.toAbsolutePath().toString()) + "." + Utils.getExtension(path));
    if (Files.exists(cachedFile)) {
      Utils.deleteFileSafely(cachedFile);
    }
  }

  /**
   * Gets the cached image for "string" location (mostly an url).<br>
   * If not found AND it is a valid url, download and cache first.<br>
   * 
   * @param url
   *          the url of image, or basically the unhashed string of cache file
   * @return the cached file or NULL
   */
  public static Path getCachedFile(String url) {
    if (url == null || url.isEmpty()) {
      return null;
    }

    String ext = UrlUtil.getExtension(url);
    if (ext.isEmpty()) {
      ext = "jpg"; // just assume
    }
    Path cachedFile = ImageCache.getCacheDir().resolve(getMD5WithSubfolder(url) + "." + ext);
    if (Files.exists(cachedFile)) {
      return cachedFile;
    }

    // is the image cache activated?
    if (!Globals.settings.isImageCache()) {
      return null;
    }

    try {
      LOGGER.trace("downloading image to the image cache: {}", url);
      Url u = new Url(url);
      boolean ok = u.download(cachedFile);
      if (ok) {
        return cachedFile;
      }
    }
    catch (Exception e) {
      LOGGER.trace("Problem getting cached file for url {}", e.getMessage());
    }

    return null;
  }

  /**
   * Gets the cached file for the given {@link Path} - if ImageCache is activated<br>
   * Use the method {@link #getCachedFile(MediaFile)} is possible!<br>
   * 
   * If not found, cache original first
   *
   * @param path
   *          the path to the image
   * @return the cached file
   */
  public static Path getCachedFile(Path path) {
    return getCachedFile(new MediaFile(path));
  }

  /**
   * Gets the cached file for the given {@link MediaFile} - if ImageCache is activated<br>
   * If not found, cache original first
   * 
   * @param mediaFile
   *          the mediaFile
   * @return the cached file
   */
  public static Path getCachedFile(MediaFile mediaFile) {
    if (mediaFile == null || !mediaFile.isGraphic()) {
      return null;
    }

    Path path = mediaFile.getFileAsPath().toAbsolutePath();

    Path cachedFile = ImageCache.getCacheDir().resolve(getMD5WithSubfolder(path.toString()) + "." + Utils.getExtension(path));
    if (Files.exists(cachedFile)) {
      return cachedFile;
    }

    // is the path already inside the cache dir? serve direct
    if (path.startsWith(CACHE_DIR.toAbsolutePath())) {
      return path;
    }

    // is the image cache activated?
    if (!Globals.settings.isImageCache()) {
      // need to return null, else the caller couldn't distinguish between cached/original file
      return null;
    }

    try {
      Path p = cacheImage(mediaFile);
      return p;
    }
    catch (EmptyFileException e) {
      LOGGER.debug("failed to cache file (file is empty): {}", path);
    }
    catch (FileNotFoundException ignored) {
      // no need to log anything here
    }
    catch (Exception e) {
      LOGGER.debug("problem caching file: {}", e.getMessage());
    }

    // need to return null, else the caller couldn't distinguish between cached/original file
    return null;
  }

  /**
   * Check whether the original image is in the image cache or not
   * 
   * @param path
   *          the path to the original image
   * @return true/false
   */
  public static boolean isImageCached(Path path) {
    if (!Globals.settings.isImageCache()) {
      return false;
    }

    Path cachedFile = CACHE_DIR.resolve(ImageCache.getMD5WithSubfolder(path.toString()) + "." + Utils.getExtension(path));

    return Files.exists(cachedFile);
  }

  /**
   * clear the image cache for all graphics within the given media entity
   * 
   * @param entity
   *          the media entity
   */
  public static void clearImageCacheForMediaEntity(MediaEntity entity) {
    List<MediaFile> mediaFiles = new ArrayList<>(entity.getMediaFiles());
    for (MediaFile mediaFile : mediaFiles) {
      if (mediaFile.isGraphic()) {
        Path file = ImageCache.getCachedFile(mediaFile);
        if (file != null) {
          Utils.deleteFileSafely(file);
        }
      }
    }
  }

}
