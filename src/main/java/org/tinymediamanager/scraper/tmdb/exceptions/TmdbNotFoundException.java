package org.tinymediamanager.scraper.tmdb.exceptions;

public class TmdbNotFoundException extends TmdbException {
  public TmdbNotFoundException(int code, String message) {
    super(code, message);
  }
}
