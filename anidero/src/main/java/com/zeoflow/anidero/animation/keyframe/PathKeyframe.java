package com.zeoflow.anidero.animation.keyframe;

import android.graphics.Path;
import android.graphics.PointF;
import androidx.annotation.Nullable;

import com.zeoflow.anidero.AnideroComposition;
import com.zeoflow.anidero.value.Keyframe;
import com.zeoflow.anidero.utils.Utils;

public class PathKeyframe extends Keyframe<PointF> {
  @Nullable private Path path;

  private final Keyframe<PointF> pointKeyFrame;

  public PathKeyframe(AnideroComposition composition, Keyframe<PointF> keyframe) {
    super(composition, keyframe.startValue, keyframe.endValue, keyframe.interpolator,
        keyframe.startFrame, keyframe.endFrame);
    this.pointKeyFrame = keyframe;
    createPath();
  }

  public void createPath() {
    // This must use equals(float, float) because PointF didn't have an equals(PathF) method
    // until KitKat...
    boolean equals = endValue != null && startValue != null &&
        startValue.equals(endValue.x, endValue.y);
    //noinspection ConstantConditions
    if (endValue != null && !equals) {
      path = Utils.createPath(startValue, endValue, pointKeyFrame.pathCp1, pointKeyFrame.pathCp2);
    }
  }

  /** This will be null if the startValue and endValue are the same. */
  @Nullable Path getPath() {
    return path;
  }
}
