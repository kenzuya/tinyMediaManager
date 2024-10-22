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

package org.tinymediamanager.core.movie.connector;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaGenres;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.movie.filenaming.MovieNfoNaming;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.scraper.util.ParserUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this class is a general XML connector which suits as a base class for most xml based connectors
 *
 * @author Manuel Laggner
 */
public abstract class MovieGenericXmlConnector implements IMovieConnector {
  private static final Logger                 LOGGER                 = LoggerFactory.getLogger(MovieGenericXmlConnector.class);
  protected static final String               ORACLE_IS_STANDALONE   = "http://www.oracle.com/xml/is-standalone";
  protected static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.US);
  protected final Movie                       movie;
  protected MovieNfoParser                    parser                 = null;

  protected Document                          document;
  protected Element                           root;

  protected MovieGenericXmlConnector(Movie movie) {
    this.movie = movie;
  }

  /**
   * write own tag which are not covered by this generic connector
   */
  protected abstract void addOwnTags();

  @Override
  public void write(List<MovieNfoNaming> nfoNames) {

    // first of all, get the data from a previous written NFO file,
    // if we do not want clean NFOs
    if (!MovieModuleManager.getInstance().getSettings().isWriteCleanNfo()) {
      for (MediaFile mf : movie.getMediaFiles(MediaFileType.NFO)) {
        try {
          parser = MovieNfoParser.parseNfo(mf.getFileAsPath());
          break;
        }
        catch (Exception ignored) {
          // just ignore
        }
      }
    }

    List<MediaFile> newNfos = new ArrayList<>(1);

    for (MovieNfoNaming nfoNaming : nfoNames) {
      String nfoFilename = movie.getNfoFilename(nfoNaming);
      if (StringUtils.isBlank(nfoFilename)) {
        continue;
      }

      try {
        // create the new NFO file according to the specifications
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR
        document = factory.newDocumentBuilder().newDocument();
        document.setXmlStandalone(true);

        // tmm comment
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dat = formatter.format(new Date());
        document.appendChild(document.createComment("created on " + dat + " - tinyMediaManager " + Settings.getInstance().getVersion()));

        root = document.createElement("movie");
        document.appendChild(root);

        // add well known tags
        addTitle();
        addOriginaltitle();
        addSorttitle();
        addYear();
        addRating();
        addUserrating();
        addVotes();
        addSet();
        addPlot();
        addOutline();
        addTagline();
        addRuntime();
        addThumb();
        addFanart();
        addMpaa();
        addCertification();
        addId();
        addTmdbid();
        addIds();
        addCountry();
        addPremiered();
        addWatched();
        addPlaycount();
        addGenres();
        addStudios();
        addCredits();
        addDirectors();
        addTags();
        addActors();
        addProducers();
        addTrailer();
        addLanguages();
        addShowlink();
        addDateAdded();
        addLockdata();

        // add connector specific tags
        addOwnTags();

        // add unsupported tags
        addUnsupportedTags();

        // add tinyMediaManagers own data
        addTinyMediaManagerTags();

        // serialize to string
        Writer out = new StringWriter();
        getTransformer().transform(new DOMSource(document), new StreamResult(out));
        String xml = out.toString().replaceAll("(?<!\r)\n", "\r\n"); // windows conform line endings

        Path f = movie.getPathNIO().resolve(nfoFilename);

        // compare old vs new
        boolean changed = true;
        try {
          String xmlOld = Utils.readFileToString(f).replaceAll("\\<\\!\\-\\-.*\\-\\-\\>", ""); // replace xml comments
          String xmlNew = xml.replaceAll("\\<\\!\\-\\-.*\\-\\-\\>", "");
          if (xmlOld.equals(xmlNew)) {
            changed = false;
          }
        }
        catch (Exception e) {
          // ignore
        }

        // write to file
        if (changed) {
          Utils.writeStringToFile(f, xml);
        }
        else {
          LOGGER.trace("NFO {} did not change - do not write it!", f);
        }
        MediaFile mf = new MediaFile(f);
        mf.gatherMediaInformation(true); // force to update filedate
        newNfos.add(mf);
      }
      catch (Exception e) {
        LOGGER.error("write '" + movie.getPathNIO().resolve(nfoFilename) + "'", e);
        MessageManager.instance
            .pushMessage(new Message(Message.MessageLevel.ERROR, movie, "message.nfo.writeerror", new String[] { ":", e.getLocalizedMessage() }));
      }
    }

    if (!newNfos.isEmpty()) {
      // remove orphaned files
      List<MediaFile> existingNfos = movie.getMediaFiles(MediaFileType.NFO);
      for (MediaFile nfo : existingNfos) {
        if (!newNfos.contains(nfo)) {
          try {
            Utils.deleteFileWithBackup(nfo.getFileAsPath(), movie.getDataSource());
          }
          catch (Exception e) {
            LOGGER.debug("Could not remove orphaned NFO - '{}'", e.getMessage());
          }
        }
      }

      movie.removeAllMediaFiles(MediaFileType.NFO);
      movie.addToMediaFiles(newNfos);
    }
  }

  /**
   * add the title in the form <title>xxx</title>
   */
  protected void addTitle() {
    Element title = document.createElement("title");
    title.setTextContent(movie.getTitle());
    root.appendChild(title);
  }

  /**
   * add the originaltitle in the form <originaltitle>xxx</originaltitle>
   */
  protected void addOriginaltitle() {
    Element originaltitle = document.createElement("originaltitle");
    originaltitle.setTextContent(movie.getOriginalTitle());
    root.appendChild(originaltitle);
  }

  /**
   * add the sorttitle in the form <sorttitle>xxx</sorttitle>
   */
  protected void addSorttitle() {
    Element sorttitle = document.createElement("sorttitle");
    sorttitle.setTextContent(movie.getSortTitle());
    root.appendChild(sorttitle);
  }

  /**
   * add the year in the form <year>xxx</year>
   */
  protected void addYear() {
    Element year = document.createElement("year");
    year.setTextContent(movie.getYear() == 0 ? "" : Integer.toString(movie.getYear()));
    root.appendChild(year);
  }

  /**
   * add the rating in the form <rating>xxx</rating> (floating point with one decimal)
   */
  protected void addRating() {
    // get main rating and calculate the rating value to a base of 10
    Float rating10;

    // the default rating
    Map<String, MediaRating> ratings = movie.getRatings();

    MediaRating mainMediaRating = null;
    for (String ratingSource : MovieModuleManager.getInstance().getSettings().getRatingSources()) {
      mainMediaRating = ratings.get(ratingSource);
      if (mainMediaRating != null) {
        break;
      }
    }

    // is there any rating which is not the user rating?
    if (mainMediaRating == null) {
      for (MediaRating r : ratings.values()) {
        // skip user ratings here
        if (MediaRating.USER.equals(r.getId())) {
          continue;
        }
        mainMediaRating = r;
      }
    }

    // just create one to not pass null
    if (mainMediaRating == null) {
      mainMediaRating = MediaMetadata.EMPTY_RATING;
    }

    if (mainMediaRating.getMaxValue() > 0) {
      rating10 = mainMediaRating.getRating() * 10 / mainMediaRating.getMaxValue();
    }
    else {
      rating10 = mainMediaRating.getRating();
    }

    Element rating = document.createElement("rating");
    rating.setTextContent(String.format(Locale.US, "%.1f", rating10));
    root.appendChild(rating);
  }

  /**
   * add the userrating in the form <userrating>xxx</userrating> (floating point with at max one decimal)
   */
  protected void addUserrating() {
    // get main rating and calculate the rating value to a base of 10
    float rating10;

    MediaRating mediaRating = movie.getRating(MediaRating.USER);

    if (mediaRating.getMaxValue() > 0) {
      rating10 = mediaRating.getRating() * 10 / mediaRating.getMaxValue();
    }
    else {
      rating10 = mediaRating.getRating();
    }

    Element UserRating = document.createElement("userrating");
    DecimalFormat df = new DecimalFormat("#.#", DECIMAL_FORMAT_SYMBOLS);
    UserRating.setTextContent(df.format(rating10));
    root.appendChild(UserRating);
  }

  /**
   * add the votes in the form <votes>xxx</votes> (integer)
   */
  protected void addVotes() {
    Element votes = document.createElement("votes");
    votes.setTextContent(Integer.toString(movie.getRating().getVotes()));
    root.appendChild(votes);
  }

  /**
   * add the set information in the form <set>xxx</set>
   */
  protected void addSet() {
    Element set = document.createElement("set");
    set.setTextContent(movie.getMovieSetTitle());
    root.appendChild(set);
  }

  /**
   * add the plot in the form <plot>xxx</plot>
   */
  protected void addPlot() {
    Element plot = document.createElement("plot");
    plot.setTextContent(movie.getPlot());
    root.appendChild(plot);
  }

  /**
   * add the outline in the form <outline>xxx</outline>
   */
  protected void addOutline() {
    String outlineText = "";
    if (MovieModuleManager.getInstance().getSettings().isCreateOutline()) {
      // lets create the outline since we do not have any outline field
      if (MovieModuleManager.getInstance().getSettings().isOutlineFirstSentence()) {
        // use the first sentence of the plot (at least 20 chars)
        StringBuilder text = new StringBuilder();
        String[] sentences = movie.getPlot().split("\\.");

        for (String sentence : sentences) {
          if (text.length() > 0) {
            // there's already a text in it, append a dot
            text.append(".");
          }

          text.append(sentence);
          if (text.length() >= 20) {
            break;
          }
        }
        outlineText = text.toString();
      }
      else {
        // use the whole plot
        outlineText = movie.getPlot();
      }
    }
    else if (parser != null && StringUtils.isNotBlank(parser.outline)) {
      // only pass pre-existing outlines since we do not have the outline
      outlineText = parser.outline;
    }

    if (StringUtils.isNotBlank(outlineText)) {
      Element outline = document.createElement("outline");
      outline.setTextContent(outlineText);
      root.appendChild(outline);
    }
  }

  /**
   * add the tagline in the form <tagline>xxx</tagline>
   */
  protected void addTagline() {
    Element tagline = document.createElement("tagline");
    tagline.setTextContent(movie.getTagline());
    root.appendChild(tagline);
  }

  /**
   * add the runtime in the form <runtime>xxx</runtime> (integer)
   */
  protected void addRuntime() {
    Element runtime = document.createElement("runtime");
    runtime.setTextContent(Integer.toString(movie.getRuntime()));
    root.appendChild(runtime);
  }

  /**
   * add the thumb (poster) url in the form <thumb>xxx</thumb>
   */
  protected void addThumb() {
    Element thumb = document.createElement("thumb");
    thumb.setTextContent(movie.getArtworkUrl(MediaFileType.POSTER));
    root.appendChild(thumb);
  }

  /**
   * add the fanart url in the form <fanart>xxx</fanart>
   */
  protected void addFanart() {
    Element fanart = document.createElement("fanart");
    fanart.setTextContent(movie.getArtworkUrl(MediaFileType.FANART));
    root.appendChild(fanart);
  }

  /**
   * add the certification in <mpaa>xxx</mpaa>
   */
  protected void addMpaa() {
    Element mpaa = document.createElement("mpaa");

    if (movie.getCertification() != null) {
      mpaa.setTextContent(
          CertificationStyle.formatCertification(movie.getCertification(), MovieModuleManager.getInstance().getSettings().getCertificationStyle()));
    }
    root.appendChild(mpaa);
  }

  /**
   * add the certification in <certification></certification>
   */
  protected void addCertification() {
    Element certification = document.createElement("certification");
    if (movie.getCertification() != null) {
      certification.setTextContent(
          CertificationStyle.formatCertification(movie.getCertification(), MovieModuleManager.getInstance().getSettings().getCertificationStyle()));
    }
    root.appendChild(certification);
  }

  /**
   * add the imdb id in <id>xxx</id>
   */
  protected void addId() {
    Element id = document.createElement("id");
    id.setTextContent(movie.getImdbId());
    root.appendChild(id);
  }

  /**
   * add the tmdb id in <tmdbid>xxx</tmdbid>
   */
  protected void addTmdbid() {
    Element tmdbid = document.createElement("tmdbid");
    if (movie.getTmdbId() > 0) {
      tmdbid.setTextContent(Integer.toString(movie.getTmdbId()));
    }
    root.appendChild(tmdbid);
  }

  /**
   * add our own id store in the new kodi form<br />
   * <uniqueid type="{scraper}" default="true/false">{id}</uniqueid>
   *
   * imdb should have default="true", but if no imdb ID is available, we must ensure that at least one entry has default="true"
   */
  protected void addIds() {
    String defaultScraper = detectDefaultScraper();

    for (Map.Entry<String, Object> entry : movie.getIds().entrySet()) {
      Element uniqueid = document.createElement("uniqueid");
      uniqueid.setAttribute("type", entry.getKey());
      if (defaultScraper.equals(entry.getKey())) {
        uniqueid.setAttribute("default", "true");
      }
      else {
        uniqueid.setAttribute("default", "false");
      }
      uniqueid.setTextContent(entry.getValue().toString());
      root.appendChild(uniqueid);
    }
  }

  /**
   * add the country in <country>xxx</country> (multiple)
   */
  protected void addCountry() {
    List<String> countries = ParserUtils.split(movie.getCountry());
    for (String c : countries) {
      Element country = document.createElement("country");
      country.setTextContent(c);
      root.appendChild(country);
    }
  }

  /**
   * add the premiered date in <premiered>xxx</premiered>
   */
  protected void addPremiered() {
    Element premiered = document.createElement("premiered");
    if (movie.getReleaseDate() != null) {
      premiered.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(movie.getReleaseDate()));
    }
    root.appendChild(premiered);
  }

  /**
   * add the dateAdded date in <dateadded>xxx</dateadded>
   */
  protected void addDateAdded() {
    Element dateadded = document.createElement("dateadded");
    switch (MovieModuleManager.getInstance().getSettings().getNfoDateAddedField()) {
      case DATE_ADDED:
        if (movie.getDateAdded() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(movie.getDateAdded()));
        }
        break;

      case FILE_CREATION_DATE:
        MediaFile mainMediaFile = movie.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateCreated() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateCreated()));
        }
        break;

      case FILE_LAST_MODIFIED_DATE:
        mainMediaFile = movie.getMainFile();
        if (mainMediaFile != null && mainMediaFile.getDateLastModified() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(mainMediaFile.getDateLastModified()));
        }
        break;

      case RELEASE_DATE:
        if (movie.getReleaseDate() != null) {
          dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(movie.getReleaseDate()));
        }
        else {
          // fall back to date added
          if (movie.getDateAdded() != null) {
            dateadded.setTextContent(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(movie.getDateAdded()));
          }
        }
    }
    root.appendChild(dateadded);
  }

  /**
   * write the <lockdata> tag (mainly for Emby)<br />
   * This will protect the NFO from being modified by Emby
   */
  protected void addLockdata() {
    if (MovieModuleManager.getInstance().getSettings().isNfoWriteLockdata()) {
      Element lockdata = document.createElement("lockdata");
      lockdata.setTextContent("true");

      root.appendChild(lockdata);
    }
  }

  /**
   * add the watched flag in <watched>xxx</watched>
   */
  protected void addWatched() {
    Element watched = document.createElement("watched");
    watched.setTextContent(Boolean.toString(movie.isWatched()));
    root.appendChild(watched);
  }

  /**
   * add the playcount in <playcount>xxx</playcount> (integer) we do not have this in tmm, but we might get it from an existing nfo
   */
  protected void addPlaycount() {
    Element playcount = document.createElement("playcount");
    int playCountFromNFO = parser != null ? parser.playcount : 0;

    // take the higher value (tmm DB vs NFO)
    playCountFromNFO = Math.max(movie.getPlaycount(), playCountFromNFO);

    // if watched, make sure at least having a playcount of 1
    if (movie.isWatched()) {
      playCountFromNFO = Math.max(playCountFromNFO, 1);
    }

    playcount.setTextContent(Integer.toString(playCountFromNFO));
    root.appendChild(playcount);
  }

  /**
   * add genres in <genre>xxx</genre> tags (multiple)
   */
  protected void addGenres() {
    for (MediaGenres mediaGenre : movie.getGenres()) {
      Element genre = document.createElement("genre");
      genre.setTextContent(mediaGenre.getLocalizedName(MovieModuleManager.getInstance().getSettings().getNfoLanguage()));
      root.appendChild(genre);
    }
  }

  /**
   * add studios in <studio>xxx</studio> tags (multiple)
   */
  protected void addStudios() {
    List<String> studios = ParserUtils.split(movie.getProductionCompany());
    for (String s : studios) {
      Element studio = document.createElement("studio");
      studio.setTextContent(s);
      root.appendChild(studio);

      // break here if we just want to write one studio
      if (MovieModuleManager.getInstance().getSettings().isNfoWriteSingleStudio()) {
        break;
      }
    }
  }

  /**
   * add credits in <credits>xxx</credits> tags (mulitple)
   */
  protected void addCredits() {
    for (Person writer : movie.getWriters()) {
      Element element = document.createElement("credits");
      element.setTextContent(writer.getName());

      // add ids
      addPersonIdsAsAttributes(element, writer);

      root.appendChild(element);
    }
  }

  /**
   * add directors in <director>xxx</director> tags (mulitple)
   */
  protected void addDirectors() {
    for (Person director : movie.getDirectors()) {
      Element element = document.createElement("director");
      element.setTextContent(director.getName());

      // add ids
      addPersonIdsAsAttributes(element, director);

      root.appendChild(element);
    }
  }

  /**
   * add tags in <tag>xxx</tag> tags (multiple)
   */
  protected void addTags() {
    for (String t : movie.getTags()) {
      Element tag = document.createElement("tag");
      tag.setTextContent(t);
      root.appendChild(tag);
    }
  }

  /**
   * add actors in <actor><name>xxx</name><role>xxx</role><thumb>xxx</thumb></actor>
   */
  protected void addActors() {
    for (Person movieActor : movie.getActors()) {
      Element actor = document.createElement("actor");

      Element name = document.createElement("name");
      name.setTextContent(movieActor.getName());
      actor.appendChild(name);

      if (StringUtils.isNotBlank(movieActor.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(movieActor.getRole());
        actor.appendChild(role);
      }

      if (StringUtils.isNotBlank(movieActor.getThumbUrl())) {
        Element thumb = document.createElement("thumb");
        thumb.setTextContent(movieActor.getThumbUrl());
        actor.appendChild(thumb);
      }

      if (StringUtils.isNotBlank(movieActor.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(movieActor.getProfileUrl());
        actor.appendChild(profile);
      }

      addPersonIdsAsChildren(actor, movieActor);

      root.appendChild(actor);
    }
  }

  /**
   * add producers in <producer><name>xxx</name><role>xxx</role><thumb>xxx</thumb></producer>
   */
  protected void addProducers() {
    for (Person movieProducer : movie.getProducers()) {
      Element producer = document.createElement("producer");

      Element name = document.createElement("name");
      name.setTextContent(movieProducer.getName());
      producer.appendChild(name);

      if (StringUtils.isNotBlank(movieProducer.getRole())) {
        Element role = document.createElement("role");
        role.setTextContent(movieProducer.getRole());
        producer.appendChild(role);
      }

      if (StringUtils.isNotBlank(movieProducer.getThumbUrl())) {
        Element thumb = document.createElement("thumb");
        thumb.setTextContent(movieProducer.getThumbUrl());
        producer.appendChild(thumb);
      }

      if (StringUtils.isNotBlank(movieProducer.getProfileUrl())) {
        Element profile = document.createElement("profile");
        profile.setTextContent(movieProducer.getProfileUrl());
        producer.appendChild(profile);
      }

      // add ids
      addPersonIdsAsAttributes(producer, movieProducer);

      root.appendChild(producer);
    }
  }

  /**
   * add spoken languages in <languages>xxx</languages>
   */
  protected void addLanguages() {
    Element languages = document.createElement("languages");

    // prepare the languages to be printed in localized form
    List<String> translatedLanguages = new ArrayList<>();
    for (String langu : ParserUtils.split(movie.getSpokenLanguages())) {
      String translated = LanguageUtils.getLocalizedLanguageNameFromLocalizedString(MovieModuleManager.getInstance().getSettings().getNfoLanguage(),
          langu.strip());
      translatedLanguages.add(translated);
    }

    languages.setTextContent(String.join(", ", translatedLanguages));
    root.appendChild(languages);
  }

  /**
   * add all linked TV shows to the <showlink>xxx</showlink> tags (mulitple)
   */
  protected void addShowlink() {
    for (String showName : movie.getShowlinks()) {
      Element showlink = document.createElement("showlink");
      showlink.setTextContent(showName);
      root.appendChild(showlink);
    }
  }

  /**
   * add the trailer url in <trailer>xxx</trailer>
   */
  protected void addTrailer() {
    Element trailer = document.createElement("trailer");
    for (MediaTrailer mediaTrailer : new ArrayList<>(movie.getTrailer())) {
      if (mediaTrailer.getInNfo() && mediaTrailer.getUrl().startsWith("http")) {
        trailer.setTextContent(mediaTrailer.getUrl());
        break;
      }
    }
    root.appendChild(trailer);
  }

  /**
   * add the media source <source>xxx</source>
   */
  protected void addSource() {
    Element source = document.createElement("source");
    source.setTextContent(movie.getMediaSource().name());
    root.appendChild(source);
  }

  /**
   * add all unsupported tags from the source file to the destination file
   */
  protected void addUnsupportedTags() {
    if (parser != null) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // NOSONAR

      for (String unsupportedString : parser.unsupportedElements) {
        try {
          Document unsupported = factory.newDocumentBuilder().parse(new ByteArrayInputStream(unsupportedString.getBytes(StandardCharsets.UTF_8)));
          root.appendChild(document.importNode(unsupported.getFirstChild(), true));
        }
        catch (Exception e) {
          LOGGER.error("import unsupported tags: {}", e.getMessage());
        }
      }
    }
  }

  /**
   * add the missing meta data for tinyMediaManager to this NFO
   */
  protected void addTinyMediaManagerTags() {
    root.appendChild(document.createComment("tinyMediaManager meta data"));
    addSource();
    addEdition();
    addOriginalFilename();
    addUserNote();
  }

  /**
   * add the user note in <user_note>xxx</user_note>
   */
  protected void addUserNote() {
    Element userNote = document.createElement("user_note");
    userNote.setTextContent(movie.getNote());
    root.appendChild(userNote);
  }

  /**
   * add the original filename (which we picked up in tmm before renaming) in <original_filename>xxx</original_filename>
   */
  protected void addOriginalFilename() {
    Element originalFileName = document.createElement("original_filename");
    if (StringUtils.isBlank(movie.getOriginalFilename())) {
      originalFileName.setTextContent(movie.getMainFile().getFilename());
    }
    else {
      originalFileName.setTextContent(movie.getOriginalFilename());
    }
    root.appendChild(originalFileName);
  }

  /**
   * add the edition in <edition>xxx</edition>
   */
  protected void addEdition() {
    Element edition = document.createElement("edition");
    edition.setTextContent(movie.getEdition().name());
    root.appendChild(edition);
  }

  /**
   * get any single element by the tag name
   * 
   * @param tag
   *          the tag name
   * @return an element or null
   */
  protected Element getSingleElementByTag(String tag) {
    NodeList nodeList = document.getElementsByTagName(tag);
    for (int i = 0; i < nodeList.getLength(); ++i) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        return (Element) node;
      }
    }
    return null;
  }

  /**
   * get the transformer for XML output
   * 
   * @return the transformer
   * @throws Exception
   *           any Exception that has been thrown
   */
  protected Transformer getTransformer() throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer(); // NOSONAR

    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
    // not supported in all JVMs
    try {
      transformer.setOutputProperty(ORACLE_IS_STANDALONE, "yes");
    }
    catch (Exception ignored) {
      // okay, seems we're not on OracleJDK, OPenJDK or AdopOpenJDK
    }
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    return transformer;
  }

  /**
   * try to detect the default scraper by the given ids
   *
   * @return the scraper where the default should be set
   */
  private String detectDefaultScraper() {
    // IMDB first
    if (movie.getIds().containsKey(MediaMetadata.IMDB)) {
      return MediaMetadata.IMDB;
    }

    // TMDB second
    if (movie.getIds().containsKey(MediaMetadata.TMDB)) {
      return MediaMetadata.TMDB;
    }

    // the first found as fallback
    return movie.getIds().keySet().stream().findFirst().orElse("");
  }

  /**
   * add all well known ids for the given {@link Person} as XML attributes
   * 
   * @param element
   *          the NFO {@link Element} to add the ids to
   * @param person
   *          the {@link Person} to get the ids from
   */
  protected void addPersonIdsAsAttributes(Element element, Person person) {
    // TMDB id
    int tmdbId = person.getIdAsInt(MediaMetadata.TMDB);
    if (tmdbId > 0) {
      element.setAttribute("tmdbid", String.valueOf(tmdbId));
    }

    // IMDB id
    String imdbId = person.getIdAsString(MediaMetadata.IMDB);
    if (StringUtils.isNotBlank(imdbId)) {
      element.setAttribute("imdbid", imdbId);
    }

    // TVDB id
    int tvdbId = person.getIdAsInt(MediaMetadata.TVDB);
    if (tvdbId > 0) {
      element.setAttribute("tvdbid", String.valueOf(tvdbId));
    }
  }

  /**
   * add all well known ids for the given {@link Person} as XML children
   *
   * @param element
   *          the NFO {@link Element} to add the ids to
   * @param person
   *          the {@link Person} to get the ids from
   */
  protected void addPersonIdsAsChildren(Element element, Person person) {
    // TMDB id
    int tmdbId = person.getIdAsInt(MediaMetadata.TMDB);
    if (tmdbId > 0) {
      Element id = document.createElement("tmdbid");
      id.setTextContent(String.valueOf(tmdbId));
      element.appendChild(id);
    }

    // IMDB id
    String imdbId = person.getIdAsString(MediaMetadata.IMDB);
    if (StringUtils.isNotBlank(imdbId)) {
      Element id = document.createElement("imdbid");
      id.setTextContent(imdbId);
      element.appendChild(id);
    }

    // TVDB id
    int tvdbId = person.getIdAsInt(MediaMetadata.TVDB);
    if (tvdbId > 0) {
      Element id = document.createElement("tvdbid");
      id.setTextContent(String.valueOf(tvdbId));
      element.appendChild(id);
    }
  }
}
