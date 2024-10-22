package org.tinymediamanager.core.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tinymediamanager.core.movie.MovieEdition.COLLECTORS_EDITION;
import static org.tinymediamanager.core.movie.MovieEdition.CRITERION_COLLECTION;
import static org.tinymediamanager.core.movie.MovieEdition.DIRECTORS_CUT;
import static org.tinymediamanager.core.movie.MovieEdition.EXTENDED_EDITION;
import static org.tinymediamanager.core.movie.MovieEdition.FINAL_CUT;
import static org.tinymediamanager.core.movie.MovieEdition.IMAX;
import static org.tinymediamanager.core.movie.MovieEdition.NONE;
import static org.tinymediamanager.core.movie.MovieEdition.REMASTERED;
import static org.tinymediamanager.core.movie.MovieEdition.SPECIAL_EDITION;
import static org.tinymediamanager.core.movie.MovieEdition.THEATRICAL_EDITION;
import static org.tinymediamanager.core.movie.MovieEdition.ULTIMATE_EDITION;
import static org.tinymediamanager.core.movie.MovieEdition.UNCUT;
import static org.tinymediamanager.core.movie.MovieEdition.UNRATED;

import org.junit.Test;

/**
 * @author Manuel Laggner
 */
public class MovieEditionTest extends BasicMovieTest {

  @Test
  public void testMovieEditionRegexp() {
    // DIRECTORS_CUT
    assertThat(parse("Halloween.Directors.Cut.German.AC3D.HDRip.x264-xx")).isEqualTo(DIRECTORS_CUT);
    assertThat(parse("Saw.Directors.Cut.2004.French.HDRiP.H264-xx")).isEqualTo(DIRECTORS_CUT);
    assertThat(parse("The.Grudge.Unrated.Directors.Cut.2004.AC3D.HDRip.x264")).isEqualTo(DIRECTORS_CUT);
    assertThat(parse("Straight Outta Compton Directors Cut 2015 German AC3 BDRiP XViD-abc")).isEqualTo(DIRECTORS_CUT);

    // EXTENDED
    assertThat(parse("Lord.Of.The.Rings.Extended.Edition")).isEqualTo(EXTENDED_EDITION);
    assertThat(parse("Vikings.S03E10.EXTENDED.720p.BluRay.x264-xyz")).isEqualTo(EXTENDED_EDITION);
    assertThat(parse("Taken.3.EXTENDED.2014.BRRip.XviD.AC3-xyz")).isEqualTo(EXTENDED_EDITION);
    assertThat(parse("Coyote.Ugly.UNRATED.EXTENDED.CUT.2000.German.AC3D.HDRip.x264")).isEqualTo(EXTENDED_EDITION);
    assertThat(parse("Project X EXTENDED CUT Italian AC3 BDRip XviD-xxx")).isEqualTo(EXTENDED_EDITION);

    // THEATRICAL
    assertThat(parse("The.Lord.of.the.Rings.The.Return.of.the.King.THEATRICAL.EDITION.2003.720p.BrRip.x264.mp4")).isEqualTo(THEATRICAL_EDITION);
    assertThat(parse("Movie.43.2013.American.Theatrical.Version.DVDRip.XViD.avi")).isEqualTo(THEATRICAL_EDITION);

    // UNRATED
    assertThat(parse("Get.Hard.2015.UNRATED.720p.BluRay.DTS.x264-xyz")).isEqualTo(UNRATED);
    assertThat(parse("Curse.Of.Chucky.2013.UNRATED.1080p.WEB-DL.H264-xyz")).isEqualTo(UNRATED);
    assertThat(parse("Men.Of.War.UNRATED.1994.AC3.HDRip.x264")).isEqualTo(UNRATED);
    assertThat(parse("Men Of War UNRATED 1994 AC3 HDRip x264")).isEqualTo(UNRATED);

    // UNCUT
    assertThat(parse("12 Monkeys [Uncut] [3D]")).isEqualTo(UNCUT);
    assertThat(parse("Creep.UNCUT.2004.AC3.HDTVRip.x264")).isEqualTo(UNCUT);
    assertThat(parse("Dragonball.Z.COMPLETE.Dutch.Dubbed.UNCUT.1989.ANiME.WS.DVDRiP.XviD")).isEqualTo(UNCUT);
    assertThat(parse("Rest Stop Dead Ahead 2006 UNCUT 720p BluRay H264 AAC-xxx")).isEqualTo(UNCUT);

    // IMAX
    assertThat(parse("IMAX.Sharks.2004.BDRip.XviD-xyz")).isEqualTo(IMAX);
    assertThat(parse("IMAX.Sea.Rex.GERMAN.DOKU.BDRip.XviD-xyz")).isEqualTo(IMAX);
    assertThat(parse("IMAX.Alaska.Spirit.of.the.Wild.1997.FRENCH.AC3.DOKU.DL.720p.BluRay.x264-xxx")).isEqualTo(IMAX);
    assertThat(parse("The.Hunger.Games.Catching.Fire.2013.IMAX.720p.BluRay.H264.AAC-xxx")).isEqualTo(IMAX);
    assertThat(parse("Transformers Revenge Of The Fallen 2009 IMAX 1080p BluRay x264-abc")).isEqualTo(IMAX);

    // REMASTERED
    assertThat(parse("Halloween.2.Das.Grauen.Kehrt.Zurueck.1981.REMASTERED.GERMAN.DL.BDRIP.X264-WATCHABLE")).isEqualTo(REMASTERED);
    assertThat(parse("Airplane.1980.MULTi.REMASTERED.COMPLETE.BLURAY-WDC")).isEqualTo(REMASTERED);
    assertThat(parse("Audrey.Rose.1977.REMASTERED.COMPLETE.BLURAY-INCUBO")).isEqualTo(REMASTERED);

    // COLLECTORS
    assertThat(parse("Hammer Hart Too Fat Too Furious Collectors Edition iNTERNAL")).isEqualTo(COLLECTORS_EDITION);
    assertThat(parse("Thunderbirds.Collectors.Edition.1966.MULTi.COMPLETE.BLURAY-SharpHD")).isEqualTo(COLLECTORS_EDITION);

    // ULTIMATE
    assertThat(parse("Watchmen.2009.Ultimate.Cut.TrueHD.AC3.MULTISUBS.1080p.BluRay.x264.HQ-TUSAHD")).isEqualTo(ULTIMATE_EDITION);

    // FINAL
    assertThat(parse("The.Wicker.Man.1973.Final.Cut.German.DL.1080p.BluRay.x264-SPiCY")).isEqualTo(FINAL_CUT);
    assertThat(parse("Blade.Runner.The.Final.Cut.1982.BluRay.1080p")).isEqualTo(FINAL_CUT);

    // SPECIAL_EDITION
    assertThat(parse("Close.Encounters.Of.The.Third.Kind.SPECIAL.EDITION.1977.iNTERNAL.1080p.BluRay.x264-EwDp")).isEqualTo(SPECIAL_EDITION);

    // PLEX filename style
    assertThat(parse("Boomerang.1992.Incl.{edition-My cool Edition}.DVDRip.x264-xyz").getName().equals("My cool Edition"));
    assertThat(parse("Boomerang.1992.Incl.{edition-Theatrical Cut}.DVDRip.x264-xyz")).isEqualTo(THEATRICAL_EDITION);

    // CRITERION Collection
    assertThat(parse("some.movie name.1977.criterion.iNTERNAL.1080p.BluRay.x264-EwDp")).isEqualTo(CRITERION_COLLECTION);
    assertThat(parse("some.movie name.1977.criterion.edition-iNTERNAL.1080p.BluRay.x264-EwDp")).isEqualTo(CRITERION_COLLECTION);
    assertThat(parse("some.movie name.1977.criterion-collection-iNTERNAL.1080p.BluRay.x264-EwDp")).isEqualTo(CRITERION_COLLECTION);

    // NORMAL
    assertThat(parse("Boomerang.1992.Incl.Directors.Commentary.DVDRip.x264-xyz")).isEqualTo(NONE);
    assertThat(parse("Unrated.The.Movie.2009.720p.BluRay.x264-xyz")).isEqualTo(NONE);
    assertThat(parse("The Lion Guard Return Of The Roar 2015 DVDRip x264-aaa")).isEqualTo(NONE);
    assertThat(parse("Spies 1928 720p BluRay x264-hhh")).isEqualTo(NONE);
    assertThat(parse("Rodeo Girl 2016 DVDRip x264-yxc")).isEqualTo(NONE);
    assertThat(parse("Climax")).isEqualTo(NONE);
    assertThat(parse("The.Ultimate.Weapon.1998.720p.BluRay.x264-GUACAMOLE")).isEqualTo(NONE);
    assertThat(parse("Final.Cut.1988.VHSRIP.X264-WATCHABLE")).isEqualTo(NONE);
  }

  private MovieEdition parse(String name) {
    return MovieEdition.getMovieEditionFromString(name);
  }
}
