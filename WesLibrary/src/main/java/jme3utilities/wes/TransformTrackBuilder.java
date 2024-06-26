/*
 Copyright (c) 2023, Stephen Gold

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.wes;

import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;

/**
 * Gather the data needed to construct a TransformTrack.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TransformTrackBuilder {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TransformTrackBuilder.class.getName());
    // *************************************************************************
    // fields

    /**
     * expected duration of the track (in seconds)
     */
    final private float duration;
    /**
     * target of the resulting animation track
     */
    final private HasLocalTransform target;
    /**
     * maps animation times to (normalized) rotation vectors
     */
    final private Map<Float, Quaternion> rotationMap = new TreeMap<>();
    /**
     * maps animation times to scale vectors
     */
    final private Map<Float, Vector3f> scaleMap = new TreeMap<>();
    /**
     * maps animation times to translation vectors
     */
    final private Map<Float, Vector3f> translationMap = new TreeMap<>();
    /**
     * collects the animation times of all keyframes (in seconds)
     */
    final private Set<Float> timeSet = new TreeSet<>();
    /**
     * interpolation algorithm for rotations
     */
    private TweenRotations tweenRotations = TweenRotations.LoopSpline;
    /**
     * interpolation algorithm for scale vectors
     */
    private TweenVectors tweenScales = TweenVectors.LoopFdcSpline;
    /**
     * interpolation algorithm for translation vectors
     */
    private TweenVectors tweenTranslations = TweenVectors.LoopFdcSpline;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a builder for the specified target.
     *
     * @param target the animation target, typically an animation joint or a
     * Spatial (not null)
     * @param duration the expected duration of the animation clip (in seconds,
     * &ge;0)
     */
    public TransformTrackBuilder(HasLocalTransform target, float duration) {
        Validate.nonNull(target, "target");
        Validate.nonNegative(duration, "duration");

        this.target = target;
        this.duration = duration;
        /*
         * Give each track component a keyframe at t=0
         * to ensure interpolation will work:
         */
        Transform t = target.getLocalTransform();
        addRotation(0f, t.getRotation());
        addScale(0f, t.getScale());
        addTranslation(0f, t.getTranslation());
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a rotation keyframe.
     *
     * @param time the animation time (in seconds, &ge;0, &le;duration)
     * @param rotation the desired rotation at that time (not null, unaffected)
     */
    public void addRotation(float time, Quaternion rotation) {
        Validate.inRange(time, "time", 0f, duration);

        // Normalize the quaternion to ensure Wes interpolation will work:
        Quaternion cloneRotation = rotation.clone().normalizeLocal();

        rotationMap.put(time, cloneRotation);
        timeSet.add(time);
    }

    /**
     * Add a scaling keyframe.
     *
     * @param time the animation time (in seconds, &ge;0, &le;duration)
     * @param scale the desired scale factors at that time (not null,
     * unaffected)
     */
    public void addScale(float time, Vector3f scale) {
        Validate.inRange(time, "time", 0f, duration);

        Vector3f cloneScale = scale.clone();
        scaleMap.put(time, cloneScale);
        timeSet.add(time);
    }

    /**
     * Add a translation keyframe.
     *
     * @param time the animation time (in seconds, &ge;0, &le;duration)
     * @param translation the desired translation at that time (not null,
     * unaffected)
     */
    public void addTranslation(float time, Vector3f translation) {
        Validate.inRange(time, "time", 0f, duration);

        Vector3f cloneTranslation = translation.clone();
        translationMap.put(time, cloneTranslation);
        timeSet.add(time);
    }

    /**
     * Create a TransformTrack.
     *
     * @return a new instance (not null)
     */
    public TransformTrack build() {
        // Convert keyframe data to curves for efficient interpolation:
        VectorCurve scaleCurve = toVectorCurve(scaleMap, tweenScales);
        RotationCurve rotationCurve
                = toRotationCurve(rotationMap, tweenRotations);
        VectorCurve translationCurve
                = toVectorCurve(translationMap, tweenTranslations);

        // Generate the merged array of keyframe times:
        float[] times = toFloatArray(timeSet);
        int numFrames = times.length;

        // Allocate arrays for (possibly interpolated) track data:
        Vector3f[] translations = new Vector3f[numFrames];
        Quaternion[] rotations = new Quaternion[numFrames];
        Vector3f[] scales = new Vector3f[numFrames];

        for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
            float time = times[frameIndex];

            Vector3f translation = translationMap.get(time);
            if (translation == null) {
                translation = tweenTranslations.interpolate(
                        time, translationCurve, null);
            }
            translations[frameIndex] = translation;

            Quaternion rotation = rotationMap.get(time);
            if (rotation == null) {
                rotation
                        = tweenRotations.interpolate(time, rotationCurve, null);
            }
            rotations[frameIndex] = rotation;

            Vector3f scale = scaleMap.get(time);
            if (scale == null) {
                scale = tweenScales.interpolate(time, scaleCurve, null);
            }
            scales[frameIndex] = scale;
        }

        TransformTrack result = new TransformTrack(
                target, times, translations, rotations, scales);

        return result;
    }

    /**
     * Alter the interpolation technique for rotations.
     *
     * @param newTechnique (not null, default=LoopSpline)
     */
    public void setTweenRotations(TweenRotations newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        this.tweenRotations = newTechnique;
    }

    /**
     * Alter the interpolation technique for scales.
     *
     * @param newTechnique (not null, default=LoopFdcSpline)
     */
    public void setTweenScales(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        this.tweenScales = newTechnique;
    }

    /**
     * Alter the interpolation technique for translations.
     *
     * @param newTechnique (not null, default=LoopFdcSpline)
     */
    public void setTweenTranslations(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        this.tweenTranslations = newTechnique;
    }
    // *************************************************************************
    // private methods

    /**
     * Convert the specified set of times to an array and verify that it is in
     * ascending order.
     *
     * @param timeSet the input set (not null, unaffected)
     * @return a new array, sorted in ascending order
     */
    private static float[] toFloatArray(Set<Float> timeSet) {
        int numFrames = timeSet.size();
        float[] result = new float[numFrames];

        int frameIndex = 0;
        for (float time : timeSet) {
            result[frameIndex] = time;
            ++frameIndex;
        }

        assert MyArray.isSorted(result);
        return result;
    }

    /**
     * Convert the specified Map into a RotationCurve for interpolation.
     *
     * @param map the input map (not null, unaffected)
     * @param tween the interpolation technique that will be used (not null)
     * @return a new instance (not null)
     */
    private RotationCurve toRotationCurve(
            Map<Float, Quaternion> map, TweenRotations tween) {
        float[] times = toFloatArray(map.keySet());
        int numFrames = times.length;
        Quaternion[] array = new Quaternion[numFrames];

        for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
            float time = times[frameIndex];
            Quaternion rotation = map.get(time);
            array[frameIndex] = rotation.clone();
        }
        RotationCurve result = tween.precompute(times, duration, array);

        return result;
    }

    /**
     * Convert the specified Map into a VectorCurve for interpolation.
     *
     * @param map the input map (not null, unaffected)
     * @param tween the interpolation technique that will be used (not null)
     * @return a new instance (not null)
     */
    private VectorCurve toVectorCurve(
            Map<Float, Vector3f> map, TweenVectors tween) {
        float[] times = toFloatArray(map.keySet());
        int numFrames = times.length;
        Vector3f[] array = new Vector3f[numFrames];

        for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
            float time = times[frameIndex];
            Vector3f vector = map.get(time);
            array[frameIndex] = vector.clone();
        }
        VectorCurve result = tween.precompute(times, duration, array);

        return result;
    }
}
