package org.tinymediamanager.scraper.util.youtube.exception;

public class YoutubeException extends Exception {

  private YoutubeException(String message) {
    super(message);
  }

  public static class VideoUnavailableException extends YoutubeException {

    public VideoUnavailableException(String message) {
      super(message);
    }
  }

  public static class BadPageException extends YoutubeException {

    public BadPageException(String message) {
      super(message);
    }
  }

  public static class FormatNotFoundException extends YoutubeException {

    public FormatNotFoundException(String message) {
      super(message);
    }

  }

  public static class LiveVideoException extends YoutubeException {

    public LiveVideoException(String message) {
      super(message);
    }

  }

  public static class CipherException extends YoutubeException {

    public CipherException(String message) {
      super(message);
    }
  }

  public static class NetworkException extends YoutubeException {

    public NetworkException(String message) {
      super(message);
    }
  }

  public static class SubtitlesException extends YoutubeException {

    public SubtitlesException(String message) {
      super(message);
    }
  }

}
