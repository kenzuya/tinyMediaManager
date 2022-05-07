package org.tinymediamanager.core.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.movie.entities.Movie;

public class MediaFileTest extends BasicTest {

  @Test
  public void testLanguageDetection() {
    MediaFile mf = new MediaFile(getWorkFolder().resolve("Django Unchained Special Edition.pt-br.sub"));
    mf.gatherMediaInformation();
    assertThat(mf.getSubtitles()).isNotEmpty();
    assertThat(mf.getSubtitles().get(0).getLanguage()).isEqualTo("pob");
  }

  @Test
  public void mediaFileTypeNoTrailer() {
    Path filename = getWorkFolder().resolve("South Park - S00E00 - The New Terrance and Phillip Movie Trailer.avi");
    MediaFileType mft = MediaFileHelper.parseMediaFileType(filename);
    assertEqual(MediaFileType.VIDEO, mft);
  }

  @Test
  public void decade() {
    Movie m = new Movie();
    m.setYear(1985);
    assertThat(m.getDecadeLong().equals("1980-1989"));
    assertThat(m.getDecadeShort().equals("1980s"));

    m.setYear(201);
    assertThat(m.getDecadeLong().equals("200-209"));
    assertThat(m.getDecadeShort().equals("200s"));
  }

  @Test
  public void testMediaFileTypeDetection() throws Exception {
    // trailer
    checkMediaFileType("So.Dark.the.Night.1946.720p.BluRay.x264-x0r[Trailer-Theatrical-Trailer].mkv", MediaFileType.TRAILER);
    checkMediaFileType("cool movie-trailer.mkv", MediaFileType.TRAILER);
    checkMediaFileType("film-trailer.1.mov", MediaFileType.TRAILER);
    checkMediaFileType("film-trailer1.mov", MediaFileType.TRAILER);
    checkMediaFileType("film-trailer-1.mov", MediaFileType.TRAILER);

    // VIDEO
    checkMediaFileType("This.is.Trailer.park.Boys.mkv", MediaFileType.VIDEO);

    // THEME
    checkMediaFileType("This.is.Trailer.park.Boys.theme.mp3", MediaFileType.THEME);
    checkMediaFileType("This.is.Trailer.park.Boys.SoundTrack.wav", MediaFileType.THEME);
    checkMediaFileType("SoundTrack.ogg", MediaFileType.THEME);
    checkMediaFileType("theme.aac", MediaFileType.THEME);

  }

  private void checkMediaFileType(String filename, MediaFileType trailer) {
    assertThat(new MediaFile(Paths.get(filename)).getType()).isEqualTo(trailer);
  }
}
