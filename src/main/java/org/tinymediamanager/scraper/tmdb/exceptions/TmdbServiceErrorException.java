package org.tinymediamanager.scraper.tmdb.exceptions;

public class TmdbServiceErrorException extends TmdbException {
  public TmdbServiceErrorException(int code, String message) {
    super(code, message);
  }
}
