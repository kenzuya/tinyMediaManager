package org.tinymediamanager.core.tasks;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ARDetectorTaskTest {

  private final List<Float> customARs = Arrays.asList(1.78f, 1.85f, 2.35f, 2.4f);

  @Test
  public void calculateARPrimaryAndSecondaryRaw_both() {
    // given
    ARDetectorTask.VideoInfo videoInfo = new ARDetectorTask.VideoInfo();
    videoInfo.sampleCount = 100;
    videoInfo.arMap = new HashMap<>();
    videoInfo.arMap.put(2.4f, 4);
    videoInfo.arMap.put(2.41f, 1);
    videoInfo.arMap.put(2.3f, 3);
    videoInfo.arMap.put(2.6f, 2);
    videoInfo.arMap.put(2.59f, 1);
    videoInfo.arMap.put(2.2f, 1);

    ARDetectorTask task = new ARDetectorTask(null) {
      @Override
      protected void init() {
        this.arCustomList.addAll(customARs);
      }
    };

    // when
    task.calculateARPrimaryAndSecondaryRaw(videoInfo);

    // then
    assertThat(videoInfo.arPrimaryRaw).isEqualTo(2.4f);
    assertThat(videoInfo.arSecondary).isEqualTo(2.6f);
  }

  @Test
  public void calculateARPrimaryAndSecondaryRaw_onlyPrimary() {
    // given
    ARDetectorTask.VideoInfo videoInfo = new ARDetectorTask.VideoInfo();
    videoInfo.sampleCount = 100;
    videoInfo.arMap = new HashMap<>();
    videoInfo.arMap.put(2.4f, 4);
    videoInfo.arMap.put(2.45f, 1);
    videoInfo.arMap.put(2.35f, 3);

    ARDetectorTask task = new ARDetectorTask(null) {
      @Override
      protected void init() {
        this.arCustomList.addAll(customARs);
      }
    };

    // when
    task.calculateARPrimaryAndSecondaryRaw(videoInfo);

    // then
    assertThat(videoInfo.arPrimaryRaw).isEqualTo(2.4f);
    assertThat(videoInfo.arSecondary).isEqualTo(0);
  }

  @Test
  public void roundAR_round() {
    // given
    ARDetectorTask task = new ARDetectorTask(null) {
      @Override
      protected void init() {
        this.arCustomList.addAll(customARs);
        this.roundUp = false;
      }
    };

    // expect
    assertThat(task.roundAR(2.0f)).isEqualTo(1.85f);
    assertThat(task.roundAR(2.36f)).isEqualTo(2.35f);
    assertThat(task.roundAR(2.38f)).isEqualTo(2.4f);
    assertThat(task.roundAR(2.42f)).isEqualTo(2.4f);
    assertThat(task.roundAR(2.5f)).isEqualTo(2.4f);
    assertThat(task.roundAR(1.83f)).isEqualTo(1.85f);
    assertThat(task.roundAR(1.85f)).isEqualTo(1.85f);
    assertThat(task.roundAR(1.86f)).isEqualTo(1.85f);
  }

  @Test
  public void roundAR_roundUp() {
    // given
    ARDetectorTask task = new ARDetectorTask(null) {
      @Override
      protected void init() {
        this.arCustomList.addAll(customARs);
        this.roundUp = true;
      }
    };
    task.roundUpThreshold = 0.01f;

    // expect
    assertThat(task.roundAR(2.0f)).isEqualTo(2.35f);
    assertThat(task.roundAR(2.36f)).isEqualTo(2.35f);
    assertThat(task.roundAR(2.38f)).isEqualTo(2.4f);
    assertThat(task.roundAR(2.42f)).isEqualTo(2.4f);
    assertThat(task.roundAR(2.5f)).isEqualTo(2.4f);
    assertThat(task.roundAR(1.83f)).isEqualTo(1.85f);
    assertThat(task.roundAR(1.85f)).isEqualTo(1.85f);
    assertThat(task.roundAR(1.86f)).isEqualTo(1.85f);
  }

}