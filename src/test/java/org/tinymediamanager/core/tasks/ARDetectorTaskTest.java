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

    // when
    float result_240_1 = task.roundAR(2.39f);
    float result_240_2 = task.roundAR(2.42f);
    float result_240_3 = task.roundAR(2.5f);
    float result_185_1 = task.roundAR(1.83f);
    float result_185_2 = task.roundAR(1.85f);
    float result_185_3 = task.roundAR(1.86f);

    // then
    assertThat(result_240_1).isEqualTo(2.4f);
    assertThat(result_240_2).isEqualTo(2.4f);
    assertThat(result_240_3).isEqualTo(2.4f);
    assertThat(result_185_1).isEqualTo(1.85f);
    assertThat(result_185_2).isEqualTo(1.85f);
    assertThat(result_185_3).isEqualTo(1.85f);
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

    // when
    float result_240_1 = task.roundAR(2.38f);
    float result_240_2 = task.roundAR(2.42f);
    float result_240_3 = task.roundAR(2.5f);
    float result_185_1 = task.roundAR(1.83f);
    float result_185_2 = task.roundAR(1.85f);
    float result_185_3 = task.roundAR(1.86f);

    // then
    assertThat(result_240_1).isEqualTo(2.35f);
    assertThat(result_240_2).isEqualTo(2.4f);
    assertThat(result_240_3).isEqualTo(2.4f);
    assertThat(result_185_1).isEqualTo(1.85f);
    assertThat(result_185_2).isEqualTo(1.85f);
    assertThat(result_185_3).isEqualTo(1.85f);
  }

}