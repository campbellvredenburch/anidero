package com.zeoflow.anidero;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.collection.LongSparseArray;
import androidx.collection.SparseArrayCompat;

import com.zeoflow.anidero.model.Font;
import com.zeoflow.anidero.model.FontCharacter;
import com.zeoflow.anidero.model.Marker;
import com.zeoflow.anidero.model.layer.Layer;
import com.zeoflow.anidero.parser.moshi.JsonReader;
import com.zeoflow.anidero.utils.Logger;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * After Effects/Bodymovin composition model. This is the serialized model from which the
 * animation will be created.
 *
 * To create one, use {@link AnideroCompositionFactory}.
 *
 * It can be used with a {@link com.zeoflow.anidero.AnideroAnimationView} or
 * {@link AnideroDrawable}.
 */
public class AnideroComposition
{

  private final PerformanceTracker performanceTracker = new PerformanceTracker();
  private final HashSet<String> warnings = new HashSet<>();
  private Map<String, List<Layer>> precomps;
  private Map<String, AnideroImageAsset> images;
  /** Map of font names to fonts */
  private Map<String, Font> fonts;
  private List<Marker> markers;
  private SparseArrayCompat<FontCharacter> characters;
  private LongSparseArray<Layer> layerMap;
  private List<Layer> layers;
  // This is stored as a set to avoid duplicates.
  private Rect bounds;
  private float startFrame;
  private float endFrame;
  private float frameRate;
  /**
   * Used to determine if an animation can be drawn with hardware acceleration.
   */
  private boolean hasDashPattern;
  /**
   * Counts the number of mattes and masks. Before Android switched to SKIA
   * for drawing in Oreo (API 28), using hardware acceleration with mattes and masks
   * was only faster until you had ~4 masks after which it would actually become slower.
   */
  private int maskAndMatteCount = 0;

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public void init(Rect bounds, float startFrame, float endFrame, float frameRate,
      List<Layer> layers, LongSparseArray<Layer> layerMap, Map<String,
      List<Layer>> precomps, Map<String, AnideroImageAsset> images,
      SparseArrayCompat<FontCharacter> characters, Map<String, Font> fonts,
      List<Marker> markers) {
    this.bounds = bounds;
    this.startFrame = startFrame;
    this.endFrame = endFrame;
    this.frameRate = frameRate;
    this.layers = layers;
    this.layerMap = layerMap;
    this.precomps = precomps;
    this.images = images;
    this.characters = characters;
    this.fonts = fonts;
    this.markers = markers;
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public void addWarning(String warning) {
    Logger.warning(warning);
    warnings.add(warning);
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public void setHasDashPattern(boolean hasDashPattern) {
    this.hasDashPattern = hasDashPattern;
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public void incrementMatteOrMaskCount(int amount) {
    maskAndMatteCount += amount;
  }

  /**
   * Used to determine if an animation can be drawn with hardware acceleration.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public boolean hasDashPattern() {
    return hasDashPattern;
  }

  /**
   * Used to determine if an animation can be drawn with hardware acceleration.
   */
  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public int getMaskAndMatteCount() {
    return maskAndMatteCount;
  }

  public ArrayList<String> getWarnings() {
    return new ArrayList<>(Arrays.asList(warnings.toArray(new String[warnings.size()])));
  }

  @SuppressWarnings("WeakerAccess") public void setPerformanceTrackingEnabled(boolean enabled) {
    performanceTracker.setEnabled(enabled);
  }

  public PerformanceTracker getPerformanceTracker() {
    return performanceTracker;
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  public Layer layerModelForId(long id) {
    return layerMap.get(id);
  }

  @SuppressWarnings("WeakerAccess") public Rect getBounds() {
    return bounds;
  }

  @SuppressWarnings("WeakerAccess") public float getDuration() {
    return (long) (getDurationFrames() / frameRate * 1000);
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public float getStartFrame() {
    return startFrame;
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public float getEndFrame() {
    return endFrame;
  }

  public float getFrameRate() {
    return frameRate;
  }

  public List<Layer> getLayers() {
    return layers;
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY)
  @Nullable
  public List<Layer> getPrecomps(String id) {
    return precomps.get(id);
  }

  public SparseArrayCompat<FontCharacter> getCharacters() {
    return characters;
  }

  public Map<String, Font> getFonts() {
    return fonts;
  }

  public List<Marker> getMarkers() {
    return markers;
  }

  @Nullable
  public Marker getMarker(String markerName) {
    int size = markers.size();
    for (int i = 0; i < markers.size(); i++) {
      Marker marker = markers.get(i);
      if (marker.matchesName(markerName)) {
        return marker;
      }
    }
    return null;
  }

  public boolean hasImages() {
    return !images.isEmpty();
  }

  @SuppressWarnings("WeakerAccess") public Map<String, AnideroImageAsset> getImages() {
    return images;
  }

  public float getDurationFrames() {
    return endFrame - startFrame;
  }


  @Override public String toString() {
    final StringBuilder sb = new StringBuilder("AnideroComposition:\n");
    for (Layer layer : layers) {
      sb.append(layer.toString("\t"));
    }
    return sb.toString();
  }

  /**
   * This will be removed in the next version of Anidero. {@link AnideroCompositionFactory} has improved
   * API names, failure handlers, and will return in-progress tasks so you will never parse the same
   * animation twice in parallel.
   *
   * @see AnideroCompositionFactory
   */
  @Deprecated
  public static class Factory {
    private Factory() {
    }

    /**
     * @see AnideroCompositionFactory#fromAsset(Context, String)
     */
    @Deprecated
    public static Cancellable fromAssetFileName(Context context, String fileName, OnCompositionLoadedListener l) {
      ListenerAdapter listener = new ListenerAdapter(l);
      AnideroCompositionFactory.fromAsset(context, fileName).addListener(listener);
      return listener;
    }

    /**
     * @see AnideroCompositionFactory#fromRawRes(Context, int)
     */
    @Deprecated
     public static Cancellable fromRawFile(Context context, @RawRes int resId, OnCompositionLoadedListener l) {
       ListenerAdapter listener = new ListenerAdapter(l);
       AnideroCompositionFactory.fromRawRes(context, resId).addListener(listener);
       return listener;
    }

    /**
     * @see AnideroCompositionFactory#fromJsonInputStream(InputStream)
     */
    @Deprecated
    public static Cancellable fromInputStream(InputStream stream, OnCompositionLoadedListener l) {
      ListenerAdapter listener = new ListenerAdapter(l);
      AnideroCompositionFactory.fromJsonInputStream(stream, null).addListener(listener);
      return listener;
    }

    /**
     * @see AnideroCompositionFactory#fromJsonString(String)
     */
    @Deprecated
    public static Cancellable fromJsonString(String jsonString, OnCompositionLoadedListener l) {
      ListenerAdapter listener = new ListenerAdapter(l);
      AnideroCompositionFactory.fromJsonString(jsonString, null).addListener(listener);
      return listener;
    }

    /**
     * @see AnideroCompositionFactory#fromJsonReader(JsonReader)
     */
    @Deprecated
    public static Cancellable fromJsonReader(JsonReader reader, OnCompositionLoadedListener l) {
      ListenerAdapter listener = new ListenerAdapter(l);
      AnideroCompositionFactory.fromJsonReader(reader, null).addListener(listener);
      return listener;
    }

    /**
     * @see AnideroCompositionFactory#fromAssetSync(Context, String)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromFileSync(Context context, String fileName) {
      return AnideroCompositionFactory.fromAssetSync(context, fileName).getValue();
    }

    /**
     * @see AnideroCompositionFactory#fromJsonInputStreamSync(InputStream)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromInputStreamSync(InputStream stream) {
      return AnideroCompositionFactory.fromJsonInputStreamSync(stream, null).getValue();
    }

    /**
     * This will now auto-close the input stream!
     *
     * @see AnideroCompositionFactory#fromJsonInputStreamSync(InputStream, boolean)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromInputStreamSync(InputStream stream, boolean close) {
      if (close) {
        Logger.warning("Anidero now auto-closes input stream!");
      }
      return AnideroCompositionFactory.fromJsonInputStreamSync(stream, null).getValue();
    }

    /**
     * @see AnideroCompositionFactory#fromJsonSync(JSONObject)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromJsonSync(@SuppressWarnings("unused") Resources res, JSONObject json) {
      return AnideroCompositionFactory.fromJsonSync(json, null).getValue();
    }

    /**
     * @see AnideroCompositionFactory#fromJsonStringSync(String)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromJsonSync(String json) {
      return AnideroCompositionFactory.fromJsonStringSync(json, null).getValue();
    }

    /**
     * @see AnideroCompositionFactory#fromJsonReaderSync(JsonReader)
     */
    @Nullable
    @WorkerThread
    @Deprecated
    public static AnideroComposition fromJsonSync(JsonReader reader) throws IOException {
      return AnideroCompositionFactory.fromJsonReaderSync(reader, null).getValue();
    }

    private static final class ListenerAdapter implements AnideroListener<AnideroComposition>, Cancellable {
      private final OnCompositionLoadedListener listener;
      private boolean cancelled = false;

      private ListenerAdapter(OnCompositionLoadedListener listener) {
        this.listener = listener;
      }

      @Override public void onResult(AnideroComposition composition) {
        if (cancelled) {
          return;
        }
        listener.onCompositionLoaded(composition);
      }

      @Override public void cancel() {
        cancelled = true;
      }
    }
  }
}
