package org.tinymediamanager.core.tvshow.tasks;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;

import java.util.List;

public class TvShowARDetectorTask extends ARDetectorTask {

  private final List<TvShowEpisode> episodes;

  public TvShowARDetectorTask(List<TvShowEpisode> episodes) {
    this.episodes = episodes;
  }

  @Override
  protected void doInBackground() {
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
        analyze(mediaFile, idx++);
      }
      episode.saveToDb();
      episode.writeNFO();
    }
  }
}
