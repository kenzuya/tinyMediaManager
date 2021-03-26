package org.tinymediamanager.core.tasks;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.*;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.thirdparty.FFmpeg;
import org.tinymediamanager.thirdparty.MediaInfo;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ARDetectorTask extends TmmTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(ARDetectorTask.class);

  private final Pattern patternSample = Pattern.compile("x1:([0-9]*)\\sx2:([0-9]*)\\sy1:([0-9]*)\\sy2:([0-9]*)\\sw:([0-9]*)\\sh:([0-9]*)\\sx:");

  private final Settings settings = Settings.getInstance();

  protected Settings.ArdMode mode = Settings.ArdMode.DEFAULT;
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

  protected int multiFormatMode = 0;
  private float multiFormatThreshold = 0.06f;

  protected final List<Float> arCustomList = new LinkedList<>();

  public ARDetectorTask() {
    super(TmmResourceBundle.getString("update.aspectRatio"),
          100, TaskType.BACKGROUND_TASK);
    init();
  }

  protected void init() {
    this.mode = settings.getArdMode();
    Settings.ARDSampleSetting modeSettings = settings.getArdSetting(this.mode);
    if (modeSettings == null) {
      modeSettings = settings.getArdSetting(Settings.ArdMode.DEFAULT);
    }
    this.sampleDuration = modeSettings.getDuration();
    this.sampleMinNumber = modeSettings.getMinNumber();
    this.sampleMaxGap = modeSettings.getMaxGap();

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
    if (!Settings.ArdMode.ACCURATE.equals(this.mode)) {
      this.multiFormatMode = 0;
    }
    this.multiFormatThreshold = settings.getArdMFThreshold();

    this.arSecondaryDelta = settings.getArdSecondaryDelta();
    this.plausiWidthPct = settings.getArdPlausiWidthPct();
    this.plausiHeightPct = settings.getArdPlausiHeightPct();
    this.plausiWidthDeltaPct = settings.getArdPlausiWidthDeltaPct();
    this.plausiHeightDeltaPct = settings.getArdPlausiHeightDeltaPct();
  }

  protected void analyze(MediaFile mediaFile) {
    analyze(mediaFile, 0);
  }

  protected void analyze(MediaFile mediaFile, int idx) {
    setTaskName(TmmResourceBundle.getString("update.aspectRatio") + ": " + mediaFile.getFilename());

    try {
      VideoInfo videoInfo = new VideoInfo();

      try {
        String width = MediaFileHelper.getMediaInfoDirect(mediaFile, MediaInfo.StreamKind.Video, 0, "Sampled_Width");
        videoInfo.width = Integer.valueOf(width);
      } catch (Exception ex) {
        videoInfo.width = mediaFile.getVideoWidth();
      }

      try {
        String height = MediaFileHelper.getMediaInfoDirect(mediaFile, MediaInfo.StreamKind.Video, 0, "Sampled_Height");
        videoInfo.height = Integer.valueOf(height);
      } catch (Exception ex) {
        videoInfo.height = mediaFile.getVideoHeight();
      }

      videoInfo.duration = mediaFile.getDuration();
      videoInfo.arSample = getSampleAR(mediaFile);
      if (videoInfo.arSample <= 0.5f) videoInfo.arSample = 1f;

      LOGGER.info("Metadata: Encoded size: {}x{}px, Encoded AR: {}, SAR: {}, Duration: {}",
        videoInfo.width, videoInfo.height,
        mediaFile.getAspectRatio(),
        videoInfo.arSample,
        mediaFile.getDurationHHMMSS());

      int start = (int)(videoInfo.duration * this.ignoreBeginning / 100f);
      int end = (int)(videoInfo.duration * (1f - (this.ignoreEnd / 100f)));

      float increment = (end - start) / (this.sampleMinNumber + 1f);

      float seconds = start + increment;

      if (increment > this.sampleMaxGap) {
        increment = this.sampleMaxGap;
        seconds = start;
      }

      while (seconds < (end - 2)) {
        try {
          int iSec = Math.round(seconds);
          int iInc = Math.round(increment);
          String result = FFmpeg.scanSample(iSec, sampleDuration, mediaFile.getFile());
          parseSample(result, iSec, iInc, videoInfo);
        } catch (Exception ex) {
          LOGGER.warn("Error scanning sample", ex);
        }

        seconds += increment - videoInfo.sampleSkipAdjustement;

        if (this.cancel) {
          return;
        }

        int progress = ((int)seconds - start) * 100 / (end - start);
        publishState(idx * 100 + progress);
      }

      if (videoInfo.sampleCount == 0) {
        LOGGER.warn("No results from scanning");
        MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR,
                                            "task.ard",
                                            "message.ard.failed",
                                            new String[] { ":", mediaFile.getFilename()}));
        return;
      }

      calculateARPrimaryAndSecondaryRaw(videoInfo);

      if (this.multiFormatMode > 0) {
        detectMultiFormat(videoInfo);
      } else {
        getNewHeight(videoInfo);
        videoInfo.height = videoInfo.heightPrimary;
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.debug("Multi format:      disabled");
      }
      videoInfo.arPrimary = roundAR(videoInfo.arPrimaryRaw);
      LOGGER.debug("AR_Primary:        {}", String.format("%.2f", videoInfo.arPrimary));

      mediaFile.setAspectRatio(videoInfo.arPrimary);
      mediaFile.setVideoHeight(videoInfo.height);
      mediaFile.setVideoWidth(videoInfo.width);

      LOGGER.info("Detected: {}x{} AR: {}", videoInfo.width, videoInfo.height, videoInfo.arPrimary);
    } catch (Exception ex) {
      LOGGER.error("Error detecting aspect ratio", ex);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR,
                                                      "task.ard",
                                                      "message.ard.failed",
                                                      new String[] { ":", mediaFile.getFilename()}));
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

          String barstxt = String.format("{%4d|%4d} {%3d|%3d}",
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
      LOGGER.debug("Analyzing {}s @ {} => bars: {} => Sample skipped: More than {}% difference between left and right black bar",
                  this.sampleDuration, LocalTime.MIN.plusSeconds(seconds).toString(),
                  barstxt, this.plausiWidthDeltaPct);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if (Math.abs(blackTop - blackBottom) > (videoInfo.height * this.plausiHeightDeltaPct / 100d)) {
      LOGGER.debug("Analyzing {}s @ {} => bars: {} => Sample skipped: More than {}% difference between top and bottom black bar",
                  this.sampleDuration, LocalTime.MIN.plusSeconds(seconds).toString(),
                  barstxt, this.plausiHeightDeltaPct);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = (float) increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if ((videoInfo.width * this.plausiWidthPct / 100d) >= width) {
      LOGGER.debug("Analyzing {}s @ {} => bars: {} => Sample skipped: Cropped width ({}px) is less than {}% of video width ({}px)",
                  this.sampleDuration, LocalTime.MIN.plusSeconds(seconds).toString(),
                  barstxt, width, this.plausiWidthPct, videoInfo.width);
      if (videoInfo.sampleSkipAdjustement == 0) {
        videoInfo.sampleSkipAdjustement = increment * 1.4f;
      } else {
        videoInfo.sampleSkipAdjustement = 0;
      }
    } else if ((videoInfo.height * this.plausiHeightPct / 100d) >= height) {
      LOGGER.debug("Analyzing {}s @ {} => bars: {} => Sample skipped: Cropped height ({}px) is less than {}% of video height ({}px)",
                  this.sampleDuration, LocalTime.MIN.plusSeconds(seconds).toString(),
                  barstxt, height, this.plausiHeightPct, videoInfo.height);
      if (videoInfo.sampleSkipAdjustement == 0) {
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
      if (!videoInfo.heightMap.containsKey(height)) {
        videoInfo.heightMap.put(height, 1);
      } else {
        videoInfo.heightMap.put(height, videoInfo.heightMap.get(height) + 1);
      }
      if (!videoInfo.widthMap.containsKey(width)) {
        videoInfo.widthMap.put(width, 1);
      } else {
        videoInfo.widthMap.put(width, videoInfo.widthMap.get(width) + 1);
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

    // If sums are equal arPrimaryRaw and arSecondary need to be swapped because sort order of heightPrimary is different
    if (arPrimarySum == arSecondarySum) {
      float tmp = videoInfo.arPrimaryRaw;
      videoInfo.arPrimaryRaw = videoInfo.arSecondary;
      videoInfo.arSecondary = tmp;
    }

    float arPrimaryPct = arPrimarySum * 100f / videoInfo.sampleCount;
    videoInfo.arSecondaryPct = arSecondarySum * 100f / videoInfo.sampleCount;
    float arOtherPct = (videoInfo.sampleCount - arPrimarySum - arSecondarySum) * 100f / videoInfo.sampleCount;

    LOGGER.debug("AR_PrimaryRaw:     {}, {}% of samples are within ±{}    Aspect ratio (AR) detected in most of the analyzed samples",
                String.format("%7.5f", videoInfo.arPrimaryRaw), String.format("%6.2f", arPrimaryPct), this.arSecondaryDelta);
    LOGGER.debug("AR_SecondaryRaw:   {}, {}% of samples are within ±{}    Second most frequent AR (multi format video likely at higher values)",
                String.format("%7.5f", videoInfo.arSecondary), String.format("%6.2f", videoInfo.arSecondaryPct), this.arSecondaryDelta);
    LOGGER.debug("Other ARs:                  {}% of samples                     Other ARs found, high value means bad detection",
                String.format("%6.2f", arOtherPct));
  }

  protected void detectMultiFormat(VideoInfo videoInfo) {
    if (videoInfo.arSecondaryPct >= this.multiFormatThreshold * 100) {
      LOGGER.debug("Multi format:      yes                                             AR_Secondary ({}% of samples) >= MFV Detection Threshold ({}% of samples)",
      String.format("%.2f", videoInfo.arSecondaryPct), String.format("%.2f", this.multiFormatThreshold * 100));

      if (multiFormatMode == 1) {
        float tmp = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;
        getNewHeight(videoInfo);
        videoInfo.height = Math.max(videoInfo.heightPrimary, videoInfo.heightSecondary);
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.debug("MFV detected, arPrimaryRaw is higher AR: {} height: {}", String.format("%.5f", videoInfo.arPrimaryRaw), videoInfo.height);
        LOGGER.debug("MFV detected, arSecondary is wider AR: {}", String.format("%.5f", videoInfo.arSecondary));
      } else if (multiFormatMode == 2) {
        float tmp = Math.max(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arSecondary = Math.min(videoInfo.arPrimaryRaw, videoInfo.arSecondary);
        videoInfo.arPrimaryRaw = tmp;
        getNewHeight(videoInfo);
        videoInfo.height = Math.min(videoInfo.heightPrimary, videoInfo.heightSecondary);
        videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

        LOGGER.debug("MFV detected, arPrimaryRaw is wider AR: {} height: {}", String.format("%.5f", videoInfo.arPrimaryRaw), videoInfo.height);
        LOGGER.debug("MFV detected, arSecondary is higher AR: {}", String.format("%.5f", videoInfo.arSecondary));
      }
    } else {
      getNewHeight(videoInfo);
      videoInfo.height = videoInfo.heightPrimary;
      videoInfo.width = Math.round(videoInfo.height * videoInfo.arPrimaryRaw / videoInfo.arSample);

      LOGGER.debug("Multi format:      no                                              AR_Secondary ({}% of samples) < MFV Detection Threshold ({}% of samples)",
                  String.format("%.2f", videoInfo.arSecondaryPct), String.format("%.2f", this.multiFormatThreshold * 100));
    }
  }

  protected float roundAR_nearest(float ar) {
    float rounded = 999f;

    // Round to nearest Aspect Ratio from list
    if (this.arCustomList.size() == 1) {
      return this.arCustomList.get(0);
    } else {
      for (int idx = 0; idx < this.arCustomList.size() - 1; idx++) {
        float maxAr = (float) Math.sqrt(this.arCustomList.get(idx).doubleValue() *
                                        this.arCustomList.get(idx + 1).doubleValue());
        if (ar < maxAr) {
          rounded = this.arCustomList.get(idx).floatValue();
          break;
        }
      }
    }

    if (rounded == 999f) rounded = this.arCustomList.get(this.arCustomList.size() - 1).floatValue();
    LOGGER.debug("Rounded to nearest Aspect Ratio from list");

    return rounded;
  }

  protected float roundAR(float ar) {
    float rounded = 999f;
	  boolean roundNearest = false;

    if (this.roundUp) {
      // Round up to next wider Aspect Ratio from list if delta is greater than threshold
      float arDelta = 999f;

      for (Float arProvided : this.arCustomList) {
        if (Math.abs(arProvided - ar) <= this.roundUpThreshold) {
          rounded = roundAR_nearest(ar);
          roundNearest = true;
          break;
        }
        if ((arDelta > (arProvided - ar)) && ((arProvided - ar) >= 0)) {
          arDelta = arProvided - ar;
          rounded = arProvided;
        }
      }
      if (rounded == 999f) rounded = this.arCustomList.get(this.arCustomList.size() - 1).floatValue();
    } else {
      rounded = roundAR_nearest(ar);
      roundNearest = true;
    }
    if (!roundNearest) LOGGER.debug("Rounded to next wider Aspect Ratio from list");

    return rounded;
  }

  protected void getNewHeight(VideoInfo videoInfo) {
    int widthPrimary = videoInfo.widthMap.entrySet()
                                         .stream()
                                         .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                                         .findFirst()
                                         .map(entry -> entry.getKey())
                                         .orElse(videoInfo.width);
    videoInfo.heightPrimary = videoInfo.heightMap.entrySet()
                                                 .stream()
                                                 .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                                                 .findFirst()
                                                 .map(entry -> entry.getKey())
                                                 .orElse(videoInfo.height);

    videoInfo.heightSecondary = videoInfo.heightMap.entrySet()
                                                   .stream()
                                                   .filter(entry -> (entry.getKey() >= widthPrimary / (videoInfo.arPrimaryRaw + this.arSecondaryDelta) &&
                                                                     entry.getKey() <= widthPrimary / (videoInfo.arPrimaryRaw - this.arSecondaryDelta)))
                                                   .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                                                   .findFirst()
                                                   .map(entry -> entry.getKey())
                                                   .orElse(videoInfo.heightPrimary);
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

    int heightPrimary;
    int heightSecondary;

    Map<Float, Integer> arMap = new HashMap<>();
    Map<Integer, Integer> widthMap = new HashMap<>();
    Map<Integer, Integer> heightMap = new HashMap<>();
  }
}
