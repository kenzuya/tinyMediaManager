package org.tinymediamanager.core.movie.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tasks.ARDetectorTask;

import java.util.List;

public class MovieARDetectorTask extends ARDetectorTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(MovieARDetectorTask.class);

  private final List<Movie> movies;

  public MovieARDetectorTask(List<Movie> movies) {
    this.movies = movies;
  }

  @Override
  protected void doInBackground() {
    int filesTotal = this.movies.stream()
      .map(movie -> movie.getMediaFiles(MediaFileType.VIDEO).size())
      .reduce(Integer::sum)
      .orElse(0);

    if (filesTotal > 0) {
      setWorkUnits(filesTotal * 100);
    }

    int idx = 0;
    for (Movie movie : this.movies) {
      for (MediaFile mediaFile : movie.getMediaFiles(MediaFileType.VIDEO)) {
        if (cancel) {
          break;
        }
        analyze(mediaFile, idx++);
      }
      if (cancel) {
        LOGGER.info("Abort queue");
        break;
      }
      movie.saveToDb();
      movie.writeNFO();
    }
  }
}
