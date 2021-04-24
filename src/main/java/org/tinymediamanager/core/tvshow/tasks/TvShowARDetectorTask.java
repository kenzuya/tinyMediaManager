package org.tinymediamanager.core.tvshow.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

import java.util.List;

public class TvShowARDetectorTask extends ARDetectorTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowARDetectorTask.class);

  private final List<TvShowEpisode> episodes;

  public TvShowARDetectorTask(List<TvShowEpisode> episodes) {
    super(TaskType.MAIN_TASK);
    this.episodes = episodes;
  }

  @Override
  protected void doInBackground() {
    if (!canRun()) return;

    int filesTotal = this.episodes.stream()
                                  .map(episode -> episode.getMediaFiles(MediaFileType.VIDEO).size())
                                  .reduce(Integer::sum)
                                  .orElse(0);

    if (filesTotal > 0) {
      setWorkUnits(filesTotal * 100);
    }

    int idx = 0;
    for (TvShowEpisode episode : this.episodes) {
      for (MediaFile mediaFile : episode.getMediaFiles(MediaFileType.VIDEO)) {
        if (cancel) {
          break;
        }
        analyze(mediaFile, idx++);
      }
      if (cancel) {
        LOGGER.info("Abort queue");
        break;
      }
      episode.saveToDb();
      episode.writeNFO();
    }
  }
}
