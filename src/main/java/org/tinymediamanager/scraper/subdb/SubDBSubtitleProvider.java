package org.tinymediamanager.scraper.subdb;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.SubtitleSearchOptions;
import org.tinymediamanager.scraper.SubtitleSearchResult;
import org.tinymediamanager.scraper.mediaprovider.IMediaSubtitleProvider;
import org.tinymediamanager.scraper.subdb.service.Controller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SubDBSubtitleProvider implements IMediaSubtitleProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubDBSubtitleProvider.class);
  private static final int HASH_CHUNK_SIZE = 64 * 1024;
  private static MediaProviderInfo providerinfo = createMediaProviderInfo();

  private String hash;
  public Controller subtitleService = new Controller();
  private SubtitleSearchResult subtitleResult = new SubtitleSearchResult(getProviderInfo().getId());

  public static MediaProviderInfo createMediaProviderInfo() {
    MediaProviderInfo providerInfo = new MediaProviderInfo("thesubdb", "thesubdb.com",
            "Is a free, centralized subtitle database" + "intended to be used only by opensource and non-commercial softwares. ",
            SubDBSubtitleProvider.class.getResource("/subdb.png"));

    providerInfo.setVersion(SubDBSubtitleProvider.class);
    return providerInfo;
  }

  @Override
  public List<SubtitleSearchResult> search(SubtitleSearchOptions arg0) {

    List<SubtitleSearchResult> subtitles = new ArrayList<>();

    LOGGER.debug("searching subtitle for {} ", arg0.getQuery());

    // getting the Hash for the given File
    hash = subDbGetHash(arg0.getFile());
    if (StringUtils.isNoneBlank(hash) && hash != null) {
      LOGGER.debug("Hash is {}" , hash);
    } else {
      LOGGER.debug("No hash found for {} " , arg0.getQuery());
      return subtitles;
    }

    // getting available subtitles for the given file
    String[] languages = new String[0];
    try {
      languages = subtitleService.getSubtitles(hash).split(",");
    } catch (IOException e) {
      LOGGER.debug("error splitting languages");
    }

    // Listing Subtitle for the given language
    for (String lang : languages) {

      if (arg0.getLanguage().getLanguage().equals(new Locale(lang).getLanguage())) {

        subtitleResult.setId(hash);
        subtitleResult.setProviderId(getProviderInfo().getId());
        subtitleResult.setReleaseName(arg0.getQuery() + "_subtitle_" + lang);
        subtitleResult.setTitle(arg0.getQuery());
        subtitleResult.setUrl(subtitleService.getUrl() + "?action=download&hash=" + hash + "&language=" + lang);
        subtitles.add(subtitleResult);

      }

    }

    return subtitles;
  }

  public String subDbGetHash(File file) {
    try {
      RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
      randomAccessFile.seek(file.length() - HASH_CHUNK_SIZE);
      byte[] first = new byte[HASH_CHUNK_SIZE];
      byte[] last = new byte[HASH_CHUNK_SIZE];
      randomAccessFile.seek(0);
      randomAccessFile.read(first);
      randomAccessFile.seek(file.length() - HASH_CHUNK_SIZE);
      randomAccessFile.read(last);
      randomAccessFile.close();
      byte[] combined = new byte[first.length + last.length];
      System.arraycopy(first, 0, combined, 0, first.length);
      System.arraycopy(last, 0, combined, first.length, last.length);
      return MD5(combined);
    } catch (Exception e) {
      LOGGER.debug("error getting hash for file {} " , file.getName());
      return null;
    }
  }

  private String MD5(byte[] md5) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] thedigest = md.digest(md5);

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < thedigest.length; i++) {
      sb.append(Integer.toString((thedigest[i] & 0xff) + 0x100, 16).substring(1));
    }
    return sb.toString();
  }

  @Override
  public MediaProviderInfo getProviderInfo() {
    return providerinfo;
  }
}
