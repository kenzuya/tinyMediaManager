package org.tinymediamanager.core.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.*;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.thirdparty.FFmpeg;
import org.tinymediamanager.thirdparty.MediaInfo;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ARDetectorTask extends TmmThreadPool {

  private static final Logger LOGGER = LoggerFactory.getLogger(ARDetectorTask.class);

  private final MediaFile mediaFile;

  private final Pattern patternSample = Pattern.compile("x1:([0-9]*)\\sx2:([0-9]*)\\sy1:([0-9]*)\\sy2:([0-9]*)\\sw:([0-9]*)\\sh:([0-9]*)\\sx:");

  private final Settings settings = Settings.getInstance();

  protected int sampleDuration = 2;
  protected int sampleMinNumber = 6;
  protected int sampleMaxGap = 900;
  protected float ignoreBeginning = 2f;
  protected float ignoreEnd = 8f;

  protected boolean roundUp = false;
  protected float roundUpThreshold = 0.04f;

  private float arSecondaryDelta = 0.15f;
  private float plausiWidthPct = 50f;
  private float plausiHeightPct = 60f;
  private float plausiWidthDeltaPct = 1.5f;
  private float plausiHeightDeltaPct = 2f;

  protected int multiFormatMode = 1;
  private float multiFormatThreshold = 0.07f;

  protected final List<Float> arCustomList = new LinkedList<>();

  public ARDetectorTask(MediaFile mediaFile) {
    super(TmmResourceBundle.getString("update.aspectRatio"));

    this.mediaFile = mediaFile;
    init();
  }

  @Override
  protected void doInBackground() {
    analyze();
  }

  protected void init() {
    this.sampleDuration = settings.getArdSampleDuration();
    this.sampleMinNumber = settings.getArdSampleMinNumber();
    this.sampleMaxGap = settings.getArdSampleMaxGap();
    this.ignoreBeginning = settings.getArdIgnoreBeginning();
    this.ignoreEnd = settings.getArdIgnoreEnd();
    this.arCustomList.addAll(settings.getCustomAspectRatios()
                                     .stream()
                                     .map(ar -> Float.valueOf(ar))
                                     .sorted()
                                     .collect(Collectors.toList()));
    this.roundUp = settings.isArdRoundUp();
    this.roundUpThreshold = settings.getArdRoundThreshold();
    this.multiFormatMode = settings.getArdMFMode();
    this.multiFormatThreshold = settings.getArdMFThreshold() / 100f;
  }

  protected void analyze() {
    try {
      VideoInfo videoInfo = new VideoInfo();
      videoInfo.width = this.mediaFile.getVideoWidth();
      videoInfo.height = this.mediaFile.getVideoHeight();
      videoInfo.duration = this.mediaFile.getDuration();
      videoInfo.arSample = getSampleAR(this.mediaFile);
      if (videoInfo.arSample <= 0.5f) videoInfo.arSample = 1f;

      LOGGER.info("Metadata: {}x{}px, duration:{}, AR:{}, SAR:{}",
        videoInfo.width, videoInfo.height,
        this.mediaFile.getDurationHHMMSS(),
        this.mediaFile.getAspectRatio(),
        videoInfo.arSample);

      int start = Float.valueOf((float) videoInfo.duration * this.ignoreBeginning / 100f).intValue();
      int end = Float.valueOf((float) videoInfo.duration * (1f - (this.ignoreEnd / 100f))).intValue();

      float increment = (float) (end - start) / (this.sampleMinNumber + 1f);

      float seconds = start + increment;

      if (increment > this.sampleMaxGap) {
        increment = this.sampleMaxGap;
        seconds = start;
      }

      while (seconds < (end - 2)) {
        LOGGER.debug("Scanning @{}s", seconds);
        try {
          int iSec = Math.round(seconds);
          int iInc = Math.round(increment);
          String result = FFmpeg.scanSample(iSec, sampleDuration, this.mediaFile.getFile());
          parseSample(result, iSec, iInc, videoInfo);
        } catch (Exception ex) {
          LOGGER.warn("Error scanning sample", ex);
        }

        seconds += increment + videoInfo.sampleSkipAdjustement;
      }

      if (videoInfo.sampleCount == 0) {
        LOGGER.warn("No results from scanning");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "task.ard", "message.ard.failed"));
        return;
      }

      calculateARPrimaryAndSecondaryRaw(videoInfo);

      if (this.multiFormatMode > 0) {
        detectMultiFormat(videoInfo);
      } else {
        LOGGER.debug("Multi format: disabled");
      }

      videoInfo.arPrimary = roundAR(videoInfo.arPrimaryRaw);
      LOGGER.debug("AR_Primary: {}", String.format("%.2f", videoInfo.arPrimary));

      this.mediaFile.setAspectRatio(videoInfo.arPrimary);

      int newHeight = getNewHeight(videoInfo);
      this.mediaFile.setVideoHeight(newHeight);

      LOGGER.info("Detected: {}x{} AR:{}", videoInfo.width, newHeight, videoInfo.arPrimary);
    } catch (Exception ex) {
      LOGGER.error("Error detecting aspect ratio", ex);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "task.ard", "message.ard.failed"));
    }
  }

  protected void parseSample(String result, int seconds, int increment, VideoInfo videoInfo) {
    if (StringUtils.isNotEmpty(result)) {
      Matcher matcher = patternSample.matcher(result);
      if (matcher.find()) {
        String sample = matcher.results()
          .map(match -> match.group(5) + " " + match.group(6) + " " + match.group(1) + " " + match.group(2) + " " + match.group(3) + " " + match.group(4))
          .sorted(Comparator.reverseOrder())
          .findFirst()
          .orElse("");
        String[] sampleData = sample.split(" ");
        if (sampleData.length == 6) {
          int width = Integer.valueOf(sampleData[0]);
          int height = Integer.valueOf(sampleData[1]);
          int blackLeft = Integer.valueOf(sampleData[2]);
          int blackRight = videoInfo.width - Integer.valueOf(sampleData[3]) - 1;
          int blackTop = Integer.valueOf(sampleData[4]);
          int blackBottom = videoInfo.height - Integer.valueOf(sampleData[5]) - 1;

          if (height > 0) {
            videoInfo.arMeasured = (float) width / (float) height;
          } else {
            videoInfo.arMeasured = 9.99f;
          }

          videoInfo.arCalculated = (float) (Math.round(videoInfo.arMeasured * videoInfo.arSample * 10E5) / 10E5);

          String barstxt = String.format("{%d|%d} {%d|%d}",
                                         blackLeft, blackRight, blackTop, blackBottom);

          checkPlausibility(width, height, blackLeft, blackRight, blackTop, blackBottom,
                            barstxt, seconds, increment, videoInfo);
        }
      }
    } else {
      throw new RuntimeException("Sample result is empty");
    }
  }

  protected void checkPlausibility(int width, int height,
                                 int blackLeft, int blackRight,
                                 int blackTop, int blackBottom,
                                 String barstxt,
                                 int seconds, int increment,
                                 VideoInfo videoInfo) {
    if ((Math.abs(blackLeft - blackRight)) > (videoInfo.width * this.plausiWidthDeltaPct / 100d)) {
      LOGGER.debug(" => bars: {} => Sample ignored: More than {}% difference between left and right black bar",
                  barstxt, this.plausiWidthDeltaPct);
      if (videoInfo.sampleSkipAdjustement > 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if (Math.abs(blackTop - blackBottom) > (videoInfo.height * this.plausiHeightDeltaPct / 100d)) {
      LOGGER.debug(" => bars: {} => Sample skipped: More than {}% difference between top and bottom black bar",
                  barstxt, this.plausiHeightDeltaPct);
      if (videoInfo.sampleSkipAdjustement > 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if ((videoInfo.width * this.plausiWidthPct / 100d) >= width) {
      LOGGER.debug(" => bars: {} => Sample skipped: Cropped width ({}px) is less than {}% of video width ({}px)",
                  barstxt, width, this.plausiWidthPct, videoInfo.width);
      if (videoInfo.sampleSkipAdjustement > 0) {
        videoInfo.sampleSkipAdjustement = increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if ((videoInfo.height * this.plausiHeightPct / 100d) >= height) {
      LOGGER.debug(" => bars: {} => Sample skipped: Cropped height ({}px) is less than {}% of video height ({}px)",
                  barstxt, height, this.plausiHeightPct, videoInfo.height);
      if (videoInfo.sampleSkipAdjustement > 0) {
        videoInfo.sampleSkipAdjustement = increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else {
      videoInfo.sampleSkipAdjustement = 0;
      if (!videoInfo.arMap.containsKey(videoInfo.arCalculated)) {
        videoInfo.arMap.put(videoInfo.arCalculated, 1);
      } else {
        videoInfo.arMap.put(videoInfo.arCalculated, videoInfo.arMap.get(videoInfo.arCalculated) + 1);
      }
      videoInfo.sampleCount++;
      LOGGER.debug("Analyzing {}s @ {} => bars: {} crop: {}x{} ({}) * SAR => AR_Calculated = {}",
                  this.sampleDuration, LocalTime.MIN.plusSeconds(seconds).toString(),
                  barstxt, width, height,
                  String.format("%.5f", videoInfo.arMeasured), String.format("%.5f", videoInfo.arCalculated));
    }
  }

  protected void calculateARPrimaryAndSecondaryRaw(VideoInfo videoInfo) {
    videoInfo.arPrimaryRaw = videoInfo.arMap.entrySet()
                                            .stream()
                                            .sorted(Map.Entry.<Float, Integer>comparingByValue().reversed())
                                            .findFirst()
                                            .map(entry -> entry.getKey())
                                            .orElse(0f);

    videoInfo.arSecondary = videoInfo.arMap.entrySet()
                                              .stream()
                                              .filter(entry -> (entry.getKey() <= (videoInfo.arPrimaryRaw - this.arSecondaryDelta) ||
                                                                entry.getKey() >= (videoInfo.arPrimaryRaw + this.arSecondaryDelta)))
                                              .sorted(Map.Entry.<Float, Integer>comparingByValue().reversed())
                                              .findFirst()
                                              .map(entry -> entry.getKey())
                                              .orElse(0f);

    if (videoInfo.arMap.size() == 0) videoInfo.arSecondary = 0f;

    int arPrimarySum = videoInfo.arMap.entrySet()
                                      .stream()
                                      .filter(entry -> (entry.getKey() >= (videoInfo.arPrimaryRaw - this.arSecondaryDelta / 2) &&
                                                        entry.getKey() <= (videoInfo.arPrimaryRaw + this.arSecondaryDelta / 2)))
                                      .map(entry -> entry.getValue())
                                      .reduce(0, Integer::sum);
    int arSecondarySum = videoInfo.arMap.entrySet()
                                        .stream()
                                        .filter(entry -> (entry.getKey() >= (videoInfo.arSecondary - this.arSecondaryDelta / 2) &&
                                                          entry.getKey() <= (videoInfo.arSecondary + this.arSecondaryDelta / 2)))
                                        .map(entry -> entry.getValue())
                                        .reduce(0, Integer::sum);

    float arPrimaryPct = arPrimarySum * 100 / videoInfo.sampleCount;
    videoInfo.arSecondaryPct = arSecondarySum * 100 / videoInfo.sampleCount;
    float arOtherPct = ((videoInfo.sampleCount - arPrimarySum - arSecondarySum) * 100 / videoInfo.sampleCount);

    LOGGER.debug("AR_PrimaryRaw:     {}, {}% of samples are within ±{}  Aspect ratio (AR) detected in most of the analyzed samples",
      String.format("%.5f", videoInfo.arPrimaryRaw), String.format("%.1f", arPrimaryPct), this.arSecondaryDelta);
    LOGGER.debug("AR_SecondaryRaw:   {}, {}% of samples are within ±{}  Aspect ratio (AR) detected in most of the analyzed samples",
      String.format("%.5f", videoInfo.arSecondary), String.format("%.1f", videoInfo.arSecondaryPct), this.arSecondaryDelta);
    LOGGER.debug("Other ARs:         {}% of samplesOther                ARs found, high value means bad detection",
      String.format("%.1f", arOtherPct));
  }

  protected void detectMultiFormat(VideoInfo videoInfo) {
    if (videoInfo.arSecondaryPct >= this.multiFormatThreshold) {
      LOGGER.debug("Multi format: yes   AR_Secondary ({}% of samples) >= MFV Detection Threshold ({}% of samples)",
      String.format("%.1f", videoInfo.arSecondaryPct), String.format("%f", videoInfo.arSecondaryPct * 100));

      if (multiFormatMode == 1) {
        float tmp = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;

        LOGGER.debug("MFV detected, AR_Primary = wider AR, rounded to list of ARs");
        LOGGER.debug("MFV detected, AR_Secondary = higher AR, rounded to list of ARs");
      } else if (multiFormatMode == 2) {
        float tmp = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;

        LOGGER.debug("AR_Primary = higher AR, rounded to list of ARs");
        LOGGER.debug("AR_Secondary = wider AR, rounded to list of ARs");
      }
    } else {
      LOGGER.debug("Multi format: no    AR_Secondary ({}% of samples) < MFV Detection Threshold ({}%)",
                  String.format("%.1f", videoInfo.arSecondaryPct), String.format("%f", this.multiFormatThreshold * 100));
    }
  }

  protected float roundAR(float ar) {
    float rounded = 999f;

    if (this.roundUp) {
      float arDelta = 999f;

      for (Float arProvided : this.arCustomList) {
        // Round up to next wider Aspect Ratio from list if delta is greater than threshold
        if (Math.abs(arProvided - ar) <= this.roundUpThreshold) {
          rounded = arProvided;
          break;
        }
        if ((arDelta > (arProvided - ar)) && ((arProvided - ar) >= 0)) {
          arDelta = arProvided - ar;
          rounded = arProvided;
        }
      }
    } else {
      // Round to nearest Aspect Ratio from list
      if (this.arCustomList.size() == 1) {
        return this.arCustomList.get(0);
      } else {
        for (int idx = 0; idx < this.arCustomList.size() - 2; idx++) {
          float maxAr = (float) Math.sqrt(this.arCustomList.get(idx).doubleValue() *
                                          this.arCustomList.get(idx + 1).doubleValue());
          if (ar < maxAr) {
            rounded = this.arCustomList.get(idx).floatValue();
            break;
          }
        }
      }
    }

    if (rounded == 999f) rounded = this.arCustomList.get(this.arCustomList.size() - 1).floatValue();

    return rounded;
  }

  protected int getNewHeight(VideoInfo videoInfo) {
    int height = videoInfo.height;
    if ((videoInfo.arPrimaryRaw * 1.25f) > videoInfo.arPrimary) {
      int croppedHeight = Math.round(videoInfo.width / (videoInfo.arPrimary / videoInfo.arSample));
      if (Math.abs(videoInfo.height - croppedHeight) >= 10) {
        height = croppedHeight;
      } else {
        LOGGER.debug("Cropped height is close to real height");
      }
    } else {
      LOGGER.debug("Real aspect ration is much lower then rounded aspect ratio");
    }
    return height;
  }

  protected float getSampleAR(MediaFile mediaFile) {
    String pixelAspectRatio = MediaFileHelper.getMediaInfoDirect(mediaFile, MediaInfo.StreamKind.Video, 0, "PixelAspectRatio");
    if (StringUtils.isNotEmpty(pixelAspectRatio)) {
      return Float.valueOf(pixelAspectRatio);
    }
    return 0f;
  }

  protected static class VideoInfo {
    int width;
    int height;
    int duration;

    int sampleCount = 0;

    float arSample = 0f;
    float arMeasured = 0f;
    float arCalculated = 0f;
    float arPrimary = 0f;
    float arPrimaryRaw = 0f;
    float arSecondary = 0f;
    float arSecondaryPct = 0f;

    float sampleSkipAdjustement = 0f;

    Map<Float, Integer> arMap = new HashMap<>();
  }


  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the
    // text is never right
    publishState(progressDone);
  }

}
