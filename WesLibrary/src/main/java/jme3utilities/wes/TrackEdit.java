/*
 Copyright (c) 2017-2023, Stephen Gold

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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyQuaternion;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods for editing JME animation tracks.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class TrackEdit {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TrackEdit.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f scaleIdentity = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TrackEdit() {
    }
    // *************************************************************************
    // new methods exposed
    // TODO deleteRange(MorphTrack), reduce(MorphTrack)
    // TODO resampleAtRate(MorphTrack), resampleToNumber(MorphTrack)
    // TODO wrap(MorphTrack)

    /**
     * Copy a MorphTrack, deleting everything before the specified time and
     * making that the start of the new track.
     *
     * @param oldTrack the input MorphTrack (not null, unaffected)
     * @param neckTime the cutoff time (in seconds, &gt;0)
     * @param neckWeights the weights at the neck time (not null, unaffected)
     * @return a new MorphTrack with the same target and t[0]=0
     */
    public static MorphTrack behead(
            MorphTrack oldTrack, float neckTime, float[] neckWeights) {
        Validate.positive(neckTime, "neck time");
        Validate.nonNull(neckWeights, "neck weights");

        float[] oldTimes = oldTrack.getTimes(); // alias
        assert neckTime >= oldTimes[0] : neckTime;
        int oldCount = oldTimes.length;
        float[] oldWeights = oldTrack.getWeights(); // alias

        int neckIndex = MyArray.findPreviousIndex(neckTime, oldTimes);
        int newCount = oldCount - neckIndex;
        assert newCount > 0 : newCount;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        int numTargets = oldTrack.getNbMorphTargets();
        float[] newWeights = new float[newCount * numTargets];

        newTimes[0] = 0f;
        for (int j = 0; j < numTargets; ++j) {
            float weight = neckWeights[j];
            newWeights[j] = weight;
        }

        for (int newIndex = 1; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex + neckIndex;
            newTimes[newIndex] = oldTimes[oldIndex] - neckTime;

            int oldStart = oldIndex * numTargets;
            int newStart = newIndex * numTargets;
            for (int j = 0; j < numTargets; ++j) {
                float weight = oldWeights[oldStart + j];
                newWeights[newStart + j] = weight;
            }
        }

        Geometry target = oldTrack.getTarget();
        MorphTrack result
                = new MorphTrack(target, newTimes, newWeights, numTargets);

        return result;
    }

    /**
     * Copy a bone/spatial track, deleting everything before the specified time
     * and making that the start of the track.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param neckTime cutoff time (in seconds, &gt;0)
     * @param neckTransform transform of bone/spatial at the neck time (not
     * null, unaffected)
     * @param oldDuration (in seconds, &ge;neckTime)
     * @return a new track of the same type as oldTrack
     */
    public static Track behead(Track oldTrack, float neckTime,
            Transform neckTransform, float oldDuration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int neckIndex
                = MyAnimation.findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        assert newCount > 0 : newCount;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = neckTransform.getTranslation().clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = neckTransform.getRotation().clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = neckTransform.getScale().clone();
        }

        for (int newIndex = 1; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex + neckIndex;

            newTimes[newIndex] = oldTimes[oldIndex] - neckTime;
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, deleting everything before the specified time and
     * making that the start of the new track.
     *
     * @param oldTrack the input TransformTrack (not null, unaffected)
     * @param neckTime the cutoff time (in seconds, &gt;0)
     * @param neckTransform the Transform of target at the neck time (not null,
     * unaffected)
     * @return a new TransformTrack with the same target and t[0]=0
     */
    public static TransformTrack behead(
            TransformTrack oldTrack, float neckTime, Transform neckTransform) {
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getTimes(); // alias
        assert neckTime >= oldTimes[0];
        int oldCount = oldTimes.length;

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int neckIndex
                = MyAnimation.findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        assert newCount > 0 : newCount;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = neckTransform.getTranslation().clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = neckTransform.getRotation().clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = neckTransform.getScale().clone();
        }

        for (int newIndex = 1; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex + neckIndex;

            newTimes[newIndex] = oldTimes[oldIndex] - neckTime;
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Chain 2 bone/spatial tracks together to create a new track.
     *
     * @param track1 the track to play first (not null, unaffected)
     * @param track2 the track to play last (not null, unaffected)
     * @param startTime2 start time for track2 in the result (in seconds, &ge;0,
     * &le;newDuration)
     * @param newDuration duration of the result (in seconds, &ge;start2)
     * @return a new track of the same type
     */
    public static Track chain(
            Track track1, Track track2, float startTime2, float newDuration) {
        assert (track1 instanceof BoneTrack && track2 instanceof BoneTrack)
                || (track1 instanceof SpatialTrack
                && track2 instanceof SpatialTrack);
        Validate.inRange(startTime2, "start time for track2", 0f, newDuration);

        float[] times1 = track1.getKeyFrameTimes(); // alias
        Vector3f[] translations1 = MyAnimation.copyTranslations(track1);
        Quaternion[] rots1 = MyAnimation.copyRotations(track1);
        Vector3f[] scales1 = MyAnimation.copyScales(track1);

        float[] times2 = track2.getKeyFrameTimes(); // alias
        Vector3f[] translations2 = MyAnimation.copyTranslations(track2);
        Quaternion[] rots2 = MyAnimation.copyRotations(track2);
        Vector3f[] scales2 = MyAnimation.copyScales(track2);

        // Calculate the index of the last keyframe to include from each track.
        int last1 = MyAnimation.findPreviousKeyframeIndex(track1, newDuration);
        assert last1 >= 0 : last1;
        float newDuration2 = newDuration - startTime2;
        int last2 = MyAnimation.findPreviousKeyframeIndex(track2, newDuration2);
        assert last2 >= 0 : last2;

        // Calculate the number of keyframes in the result.
        float lastTime1 = times1[last1];
        int numCopy1;
        int numBlend;
        int numCopy2;
        if (lastTime1 < startTime2) {
            numCopy1 = last1 + 1;
            numBlend = 0;
            numCopy2 = last2 + 1;
        } else if (lastTime1 == startTime2) {
            numCopy1 = last1;
            numBlend = 1;
            numCopy2 = last2;
        } else {
            throw new IllegalArgumentException("overlapping tracks");
        }
        int newCount = numCopy1 + numBlend + numCopy2;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (translations1 != null || translations2 != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (rots1 != null || rots2 != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (scales1 != null || scales2 != null) {
            newScales = new Vector3f[newCount];
        }

        // Fill the new arrays.
        for (int frameIndex = 0; frameIndex < newCount; ++frameIndex) {
            Quaternion rot1;
            Quaternion rot2;
            Vector3f tra1;
            Vector3f tra2;
            Vector3f scale1;
            Vector3f scale2;
            if (frameIndex < numCopy1) { // Copy from track1[frameIndex].
                newTimes[frameIndex] = times1[frameIndex];
                tra1 = (translations1 == null)
                        ? null : translations1[frameIndex];
                rot1 = (rots1 == null) ? null : rots1[frameIndex];
                scale1 = (scales1 == null) ? null : scales1[frameIndex];
                tra2 = null;
                rot2 = null;
                scale2 = null;

            } else if (frameIndex > last1) { // Copy from track2[index2].
                int index2 = frameIndex - numCopy1;
                newTimes[frameIndex] = times2[index2] + startTime2;
                tra1 = null;
                rot1 = null;
                scale1 = null;
                tra2 = (translations2 == null) ? null : translations2[index2];
                rot2 = (rots2 == null) ? null : rots2[index2];
                scale2 = (scales2 == null) ? null : scales2[index2];

            } else { // Blend track1[last1] and track2[0].
                assert numBlend == 1 : numBlend;
                assert frameIndex == last1;
                assert lastTime1 == startTime2;
                newTimes[frameIndex] = startTime2;
                tra1 = (translations1 == null)
                        ? null : translations1[frameIndex];
                rot1 = (rots1 == null) ? null : rots1[frameIndex];
                scale1 = (scales1 == null) ? null : scales1[frameIndex];
                tra2 = (translations2 == null) ? null : translations2[0];
                rot2 = (rots2 == null) ? null : rots2[0];
                scale2 = (scales2 == null) ? null : scales2[0];
            }

            if (newTranslations != null) {
                newTranslations[frameIndex]
                        = blendTranslations(0.5f, tra1, tra2);
            }
            if (newRotations != null) {
                newRotations[frameIndex] = blendRotations(0.5f, rot1, rot2);
            }
            if (newScales != null) {
                newScales[frameIndex] = blendScales(0.5f, scale1, scale2);
            }
        }

        Track result = newTrack(
                track1, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Deeply clone the specified track without cloning its target.
     *
     * @param <T> the type of track to be cloned
     * @param track an AnimTrack or Track (not null)
     * @return a new track of the same type with the same target
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneTrack(T track) {
        T result;

        if (track instanceof MorphTrack) {
            /*
             * Can't use a Cloner here because MorphTrack.cloneFields()
             * doesn't clone times or weights.
             */
            MorphTrack oldTrack = (MorphTrack) track;
            Geometry target = oldTrack.getTarget();

            float[] oldTimes = oldTrack.getTimes(); // alias
            int numKeyFrames = oldTimes.length;
            float[] newTimes = new float[numKeyFrames];
            System.arraycopy(oldTimes, 0, newTimes, 0, numKeyFrames);

            float[] oldWeights = oldTrack.getWeights(); // alias
            int numWeights = oldWeights.length;
            float[] newWeights = new float[numWeights];
            System.arraycopy(oldWeights, 0, newWeights, 0, numWeights);

            int numTargets = oldTrack.getNbMorphTargets();
            // TODO clone the interpolator if it's not null

            MorphTrack clone
                    = new MorphTrack(target, newTimes, newWeights, numTargets);
            result = (T) clone;

        } else if (track instanceof Track) {
            result = (T) ((Track) track).clone();

        } else if (track instanceof TransformTrack) {
            /*
             * Can't use a Cloner here because TransformTrack.cloneFields()
             * doesn't clone times, translations, rotations, or scales.
             */
            TransformTrack oldTrack = (TransformTrack) track;

            float[] oldTimes = oldTrack.getTimes(); // alias
            int numKeyFrames = oldTimes.length;
            float[] newTimes = new float[numKeyFrames];
            System.arraycopy(oldTimes, 0, newTimes, 0, numKeyFrames);

            Vector3f[] translations = oldTrack.getTranslations();
            Quaternion[] rotations = oldTrack.getRotations();
            Vector3f[] scales = oldTrack.getScales();
            HasLocalTransform target = oldTrack.getTarget();
            // TODO clone the interpolator if it's not null

            TransformTrack clone = new TransformTrack(
                    target, newTimes, translations, rotations, scales);
            result = (T) clone;

        } else {
            String className = track.getClass().getSimpleName();
            throw new IllegalArgumentException(className);
        }

        return result;
    }

    /**
     * Copy a TransformTrack, converting it from a travelling animation to an
     * in-place animation. This is accomplished by zeroing out any average
     * linear velocity between (the translations of) the first and last frames.
     * Rotations and scales aren't considered. Works best on cyclic animations.
     *
     * @param oldTrack the input/source track (not null, must contain 2 or more
     * translations, unaffected)
     * @return a new track with the same target
     */
    public static TransformTrack convertToInPlace(TransformTrack oldTrack) {
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        int numTranslations
                = (oldTranslations == null) ? 0 : oldTranslations.length;
        Validate.inRange(numTranslations, "number of translations",
                2, Integer.MAX_VALUE);

        float[] oldTimes = oldTrack.getTimes(); // alias
        int frameCount = oldTimes.length;
        int lastFrame = frameCount - 1;
        float elapsed = oldTimes[lastFrame] - oldTimes[0];
        if (elapsed == 0f) {
            throw new IllegalArgumentException(
                    "The first and last frames must not be simultaneous!");
        }

        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays as needed.
        float[] newTimes = new float[frameCount];
        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[frameCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[frameCount];
        }
        /*
         * Calculate the average linear velocity
         * between the first and last frames in the track.
         */
        Vector3f startOffset = oldTranslations[0];
        Vector3f endOffset = oldTranslations[lastFrame];
        Vector3f velocity
                = MyVector3f.velocity(elapsed, startOffset, endOffset, null);

        for (int frameIndex = 0; frameIndex < frameCount; ++frameIndex) {
            float time = oldTimes[frameIndex];
            newTimes[frameIndex] = time;

            Vector3f translation = oldTranslations[frameIndex].clone();
            MyVector3f.accumulateScaled(translation, velocity, -time);
            newTranslations[frameIndex] = translation;

            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a track, delaying all its keyframes by the specified amount.
     *
     * @param oldTrack base track (not null, unaffected)
     * @param delayAmount delay interval (in seconds, &ge;0, &le;newDuration)
     * @param newDuration duration of the result (in seconds, &ge;delayAmount)
     * @return a new track of the same type as oldTrack
     */
    public static Track delayAll(
            Track oldTrack, float delayAmount, float newDuration) {
        Validate.inRange(delayAmount, "delay amount", 0f, newDuration);

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        // Calculate the old index of the last keyframe to include.
        float oldDuration = newDuration - delayAmount;
        assert oldDuration < newDuration;
        int lastIndex = MyAnimation
                .findPreviousKeyframeIndex(oldTrack, oldDuration);
        int addFrames;
        if (delayAmount > 0f) {
            addFrames = 1;
        } else {
            addFrames = 0;
        }
        int newCount = addFrames + lastIndex + 1;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = new Vector3f();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = new Quaternion(); // identity
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = new Vector3f(1f, 1f, 1f);
        }

        // Fill the new arrays.
        for (int oldIndex = 0; oldIndex <= lastIndex; ++oldIndex) {
            int frameIndex = oldIndex + addFrames;
            newTimes[frameIndex] = oldTimes[oldIndex] + delayAmount;

            if (newTranslations != null) {
                newTranslations[frameIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, deleting the indexed range of keyframes (which
     * mustn't include the first keyframe).
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param startIndex the index of the first keyframe to delete (&gt;0,
     * &le;lastIndex)
     * @param deleteCount number of keyframes to delete (&gt;0, &lt;lastIndex)
     * @return a new track of the same type as oldTrack
     */
    public static Track deleteRange(
            Track oldTrack, int startIndex, int deleteCount) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int lastIndex = oldCount - 1;
        Validate.inRange(startIndex, "start index", 1, lastIndex);
        Validate.inRange(deleteCount, "delete count", 1, lastIndex);
        float endIndex = startIndex + deleteCount - 1;
        Validate.inRange(endIndex, "end index", 1, lastIndex);

        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int newCount = oldCount - deleteCount;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex;
            if (newIndex < startIndex) {
                oldIndex = newIndex;
            } else {
                oldIndex = newIndex + deleteCount;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, deleting the indexed range of keyframes (which
     * mustn't include the first keyframe).
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param startIndex the index of the first keyframe to delete (&gt;0,
     * &le;lastIndex)
     * @param deleteCount number of keyframes to delete (&gt;0, &lt;lastIndex)
     * @return a new TransformTrack
     */
    public static TransformTrack deleteRange(
            TransformTrack oldTrack, int startIndex, int deleteCount) {
        float[] oldTimes = oldTrack.getTimes(); // alias
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int lastIndex = oldCount - 1;
        Validate.inRange(startIndex, "start index", 1, lastIndex);
        Validate.inRange(deleteCount, "delete count", 1, lastIndex);
        float endIndex = startIndex + deleteCount - 1;
        Validate.inRange(endIndex, "end index", 1, lastIndex);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = oldCount - deleteCount;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex;
            if (newIndex < startIndex) {
                oldIndex = newIndex;
            } else {
                oldIndex = newIndex + deleteCount;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, inserting a keyframe at the specified time
     * (which mustn't already have a keyframe).
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform transform to insert (not null, unaffected)
     * @return a new track of the same type
     */
    public static Track insertKeyframe(
            Track oldTrack, float frameTime, Transform transform) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(frameTime, "keyframe time");
        assert MyAnimation.findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = oldCount + 1;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = new Vector3f[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    newTimes[newIndex] = frameTime;
                    newTranslations[newIndex]
                            = transform.getTranslation().clone();
                    newRotations[newIndex] = transform.getRotation().clone();
                    newScales[newIndex] = transform.getScale().clone();
                    added = true;
                }
                ++newIndex;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (oldTranslations == null) {
                newTranslations[newIndex] = new Vector3f();
            } else {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (oldRotations == null) {
                newRotations[newIndex] = new Quaternion();
            } else {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (oldScales == null) {
                newScales[newIndex] = new Vector3f(1f, 1f, 1f);
            } else {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }
        if (!added) {
            newTimes[oldCount] = frameTime;
            newTranslations[oldCount] = transform.getTranslation().clone();
            newRotations[oldCount] = transform.getRotation().clone();
            newScales[oldCount] = transform.getScale().clone();
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, inserting a keyframe at the specified time (which
     * mustn't already have a keyframe).
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform transform to insert (not null, unaffected)
     * @return a new TransformTrack
     */
    public static TransformTrack insertKeyframe(
            TransformTrack oldTrack, float frameTime, Transform transform) {
        Validate.positive(frameTime, "keyframe time");
        assert MyAnimation.findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = oldCount + 1;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = new Vector3f[newCount];

        HasLocalTransform target = oldTrack.getTarget();
        Transform fillData = target.getLocalTransform();

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    newTimes[newIndex] = frameTime;
                    newTranslations[newIndex]
                            = transform.getTranslation().clone();
                    newRotations[newIndex] = transform.getRotation().clone();
                    newScales[newIndex] = transform.getScale().clone();
                    added = true;
                }
                ++newIndex;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (oldTranslations == null) {
                newTranslations[newIndex] = fillData.getTranslation().clone();
            } else {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (oldRotations == null) {
                newRotations[newIndex] = fillData.getRotation().clone();
            } else {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (oldScales == null) {
                newScales[newIndex] = fillData.getScale().clone();
            } else {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }
        if (!added) {
            newTimes[oldCount] = frameTime;
            newTranslations[oldCount] = transform.getTranslation().clone();
            newRotations[oldCount] = transform.getRotation().clone();
            newScales[oldCount] = transform.getScale().clone();
        }

        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Create a new bone/spatial track.
     *
     * @param oldTrack to identify the track type and target bone/spatial (not
     * null, unaffected)
     * @param times (not null, alias created)
     * @param translations (either null or same length as times)
     * @param rotations (either null or same length as times)
     * @param scales (either null or same length as times)
     * @return a new track of the same type as oldTrack
     */
    public static Track newTrack(
            Track oldTrack, float[] times, Vector3f[] translations,
            Quaternion[] rotations, Vector3f[] scales) {
        Validate.nonNull(times, "times");
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        Track result;
        if (oldTrack instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) oldTrack;
            int boneIndex = boneTrack.getTargetBoneIndex();
            result = MyAnimation.newBoneTrack(
                    boneIndex, times, translations, rotations, scales);

        } else if (oldTrack instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) oldTrack;
            Spatial spatial = spatialTrack.getTrackSpatial();
            SpatialTrack newSpatialTrack
                    = new SpatialTrack(times, translations, rotations, scales);
            newSpatialTrack.setTrackSpatial(spatial);
            result = newSpatialTrack;

        } else {
            throw new IllegalArgumentException(oldTrack.getClass().getName());
        }

        return result;
    }

    /**
     * Normalize all quaternions in a morph/transform track if any of them are
     * out of tolerance.
     *
     * @param inputTrack the input transform/morph track (not null, unaffected)
     * @param tolerance the tolerance for norms (&ge;0)
     * @return a new track if changes were made, or else inputTrack
     */
    public static AnimTrack<?> normalizeQuaternions(
            AnimTrack<?> inputTrack, float tolerance) {
        Validate.nonNegative(tolerance, "tolerance");

        AnimTrack<?> result = inputTrack;
        if (inputTrack instanceof MorphTrack) { // contains no quaternions
            return result;
        }

        TransformTrack oldTrack = (TransformTrack) inputTrack;
        Quaternion[] oldRotations = oldTrack.getRotations();
        if (oldRotations == null) {
            return result;
        }

        int numFrames = oldRotations.length;
        assert numFrames > 0 : numFrames;
        boolean changes = false;
        for (Quaternion oldQuat : oldRotations) {
            double norm = MyQuaternion.lengthSquared(oldQuat);
            double delta = Math.abs(1.0 - norm);
            if (delta > tolerance) {
                changes = true;
                break;
            }
        }
        if (!changes) {
            return result;
        }

        float[] oldTimes = oldTrack.getTimes(); // alias
        assert oldTimes.length == numFrames;
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays.
        float[] times = new float[numFrames];
        Vector3f[] translations = null;
        if (oldTranslations != null) {
            assert oldTranslations.length == numFrames;
            translations = new Vector3f[numFrames];
        }
        Quaternion[] rotations = new Quaternion[numFrames];
        Vector3f[] scales = null;
        if (oldScales != null) {
            assert oldScales.length == numFrames;
            scales = new Vector3f[numFrames];
        }

        for (int frameI = 0; frameI < numFrames; ++frameI) {
            times[frameI] = oldTimes[frameI];
            if (translations != null) {
                translations[frameI] = oldTranslations[frameI].clone();
            }
            rotations[frameI] = oldRotations[frameI].clone();
            MyQuaternion.normalizeLocal(rotations[frameI]);
            if (scales != null) {
                scales[frameI] = oldScales[frameI].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        result = new TransformTrack(
                target, times, translations, rotations, scales);

        return result;
    }

    /**
     * Normalize all quaternions in a bone/spatial track if any of them are out
     * of tolerance.
     *
     * @param inputTrack the input bone/spatial track (not null, unaffected)
     * @param tolerance the tolerance for norms (&ge;0)
     * @return a new track if changes are made, or else oldTrack
     */
    public static Track normalizeQuaternions(
            Track inputTrack, float tolerance) {
        assert inputTrack instanceof BoneTrack
                || inputTrack instanceof SpatialTrack;
        Validate.nonNegative(tolerance, "tolerance");

        Track result = inputTrack;
        Quaternion[] oldRotations = MyAnimation.copyRotations(inputTrack);
        if (oldRotations == null) {
            return result;
        }

        int numFrames = oldRotations.length;
        assert numFrames > 0 : numFrames;
        boolean changes = false;
        for (Quaternion oldQuat : oldRotations) {
            double norm = MyQuaternion.lengthSquared(oldQuat);
            double delta = Math.abs(1.0 - norm);
            if (delta > tolerance) {
                changes = true;
                break;
            }
        }
        if (!changes) {
            return result;
        }

        float[] oldTimes = inputTrack.getKeyFrameTimes(); // alias
        assert oldTimes.length == numFrames;
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(inputTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(inputTrack);

        // Allocate new arrays.
        float[] times = new float[numFrames];
        Vector3f[] translations = null;
        if (oldTranslations != null) {
            assert oldTranslations.length == numFrames;
            translations = new Vector3f[numFrames];
        }
        Quaternion[] rotations = new Quaternion[numFrames];
        Vector3f[] scales = null;
        if (oldScales != null) {
            assert oldScales.length == numFrames;
            scales = new Vector3f[numFrames];
        }

        for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
            times[frameIndex] = oldTimes[frameIndex];
            if (translations != null) {
                translations[frameIndex] = oldTranslations[frameIndex].clone();
            }
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            MyQuaternion.normalizeLocal(rotations[frameIndex]);
            if (scales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        result = newTrack(inputTrack, times, translations, rotations, scales);

        return result;
    }

    /**
     * Copy a bone/spatial track, uniformly reducing the number of keyframes by
     * the specified factor.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new track of the same type as oldTrack
     */
    public static Track reduce(Track oldTrack, int factor) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = 1 + (oldCount - 1) / factor;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex * factor;

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, uniformly reducing the number of keyframes by the
     * specified factor.
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new TransformTrack
     */
    public static TransformTrack reduce(TransformTrack oldTrack, int factor) {
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        float[] oldTimes = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = 1 + (oldCount - 1) / factor;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; ++newIndex) {
            int oldIndex = newIndex * factor;

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Remove all repetitious keyframes from a MorphTrack.
     *
     * @param track the input track (not null, modified)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(MorphTrack track) {
        float[] oldTimes = track.getTimes(); // alias
        int oldCount = oldTimes.length;
        int newCount = MyArray.countNeSorted(oldTimes);
        if (newCount == oldCount) {
            return false; // nothing to remove
        }

        float[] oldWeights = track.getWeights(); // alias
        float[] newTimes = new float[newCount];
        int numTargets = track.getNbMorphTargets();
        float[] newWeights = new float[newCount * numTargets];

        // Copy all non-repeated keyframes.
        float prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = oldTimes[oldIndex];

                int oldStart = oldIndex * numTargets;
                int newStart = newIndex * numTargets;
                for (int j = 0; j < numTargets; ++j) {
                    float weight = oldWeights[oldStart + j];
                    newWeights[newStart + j] = weight;
                }
                ++newIndex;
            }
            prevTime = time;
        }

        track.setKeyframes(newTimes, newWeights);
        return true;
    }

    /**
     * Remove all repetitious keyframes from a bone/spatial track.
     *
     * @param track input bone/spatial track (not null, modified)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(Track track) {
        assert track instanceof BoneTrack || track instanceof SpatialTrack;

        float[] oldTimes = track.getKeyFrameTimes(); // alias
        int oldCount = oldTimes.length;
        int newCount = MyArray.countNeSorted(oldTimes);
        if (newCount == oldCount) {
            return false; // nothing to remove
        }

        Vector3f[] oldTranslations = MyAnimation.copyTranslations(track);
        Quaternion[] oldRotations = MyAnimation.copyRotations(track);
        Vector3f[] oldScales = MyAnimation.copyScales(track);

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        // Copy all non-repeated keyframes.
        float prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = oldTimes[oldIndex];
                if (newTranslations != null) {
                    newTranslations[newIndex] = oldTranslations[oldIndex];
                }
                if (newRotations != null) {
                    newRotations[newIndex] = oldRotations[oldIndex];
                }
                if (newScales != null) {
                    newScales[newIndex] = oldScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        setKeyframes(track, newTimes, newTranslations, newRotations, newScales);
        return true;
    }

    /**
     * Remove all repetitious keyframes from a TransformTrack.
     *
     * @param track the input track (not null, modified)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(TransformTrack track) {
        float[] oldTimes = track.getTimes(); // alias
        int oldCount = oldTimes.length;
        int newCount = MyArray.countNeSorted(oldTimes);
        if (newCount == oldCount) {
            return false; // nothing to remove
        }

        Vector3f[] oldTranslations = track.getTranslations();
        Quaternion[] oldRotations = track.getRotations();
        Vector3f[] oldScales = track.getScales();

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        // Copy all non-repeated keyframes.
        float prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < oldCount; ++oldIndex) {
            float time = oldTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = oldTimes[oldIndex];
                if (newTranslations != null) {
                    newTranslations[newIndex] = oldTranslations[oldIndex];
                }
                if (newRotations != null) {
                    newRotations[newIndex] = oldRotations[oldIndex];
                }
                if (newScales != null) {
                    newScales[newIndex] = oldScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        track.setKeyframes(newTimes, newTranslations, newRotations, newScales);
        return true;
    }

    /**
     * Copy a bone/spatial track, setting the indexed keyframe to the specified
     * transform.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param frameIndex which keyframe (&ge;0, &lt;numFrames)
     * @param transform transform to apply (not null, unaffected)
     * @return a new track of the same type as oldTrack
     */
    public static Track replaceKeyframe(
            Track oldTrack, int frameIndex, Transform transform) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        int frameCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 0, frameCount - 1);
        Validate.nonNull(transform, "transform");

        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        // Allocate new arrays.
        float[] newTimes = new float[frameCount];
        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = new Quaternion[frameCount];
        Vector3f[] newScales = new Vector3f[frameCount];

        for (int frameI = 0; frameI < frameCount; ++frameI) {
            newTimes[frameI] = oldTimes[frameI];
            if (frameI == frameIndex) {
                newTranslations[frameI] = transform.getTranslation().clone();
                newRotations[frameI] = transform.getRotation().clone();
                newScales[frameI] = transform.getScale().clone();
            } else {
                if (oldTranslations == null) {
                    newTranslations[frameI] = new Vector3f();
                } else {
                    newTranslations[frameI] = oldTranslations[frameI].clone();
                }
                if (oldRotations == null) {
                    newRotations[frameI] = new Quaternion();
                } else {
                    newRotations[frameI] = oldRotations[frameI].clone();
                }
                if (oldScales == null) {
                    newScales[frameI] = new Vector3f(1f, 1f, 1f);
                } else {
                    newScales[frameI] = oldScales[frameI].clone();
                }
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, setting the indexed keyframe to the specified
     * transform.
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param frameIndex which keyframe (&ge;0, &lt;numFrames)
     * @param transform transform to apply (not null, unaffected)
     * @return a new TransformTrack
     */
    public static TransformTrack replaceKeyframe(
            TransformTrack oldTrack, int frameIndex, Transform transform) {
        float[] oldTimes = oldTrack.getTimes(); // alias
        int frameCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 0, frameCount - 1);
        Validate.nonNull(transform, "transform");

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays.
        float[] newTimes = new float[frameCount];
        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = new Quaternion[frameCount];
        Vector3f[] newScales = new Vector3f[frameCount];

        HasLocalTransform target = oldTrack.getTarget();
        Transform fillData = target.getLocalTransform();

        for (int frameI = 0; frameI < frameCount; ++frameI) {
            newTimes[frameI] = oldTimes[frameI];
            if (frameI == frameIndex) {
                newTranslations[frameI] = transform.getTranslation().clone();
                newRotations[frameI] = transform.getRotation().clone();
                newScales[frameI] = transform.getScale().clone();
            } else {
                if (oldTranslations == null) {
                    newTranslations[frameI] = fillData.getTranslation().clone();
                } else {
                    newTranslations[frameI] = oldTranslations[frameI].clone();
                }
                if (oldRotations == null) {
                    newRotations[frameI] = fillData.getRotation().clone();
                } else {
                    newRotations[frameI] = oldRotations[frameI].clone();
                }
                if (oldScales == null) {
                    newScales[frameI] = fillData.getScale().clone();
                } else {
                    newScales[frameI] = oldScales[frameI].clone();
                }
            }
        }

        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling at the specified times.
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param newTimes sample times (not null, alias created)
     * @return a new instance
     */
    public static TransformTrack resample(
            TransformTrack oldTrack, float[] newTimes) {
        int numSamples = newTimes.length;
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays.
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[numSamples];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[numSamples];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[numSamples];
        }

        for (int frameIndex = 0; frameIndex < numSamples; ++frameIndex) {
            float time = newTimes[frameIndex];
            Transform transform = new Transform();
            oldTrack.getDataAtTime(time, transform);

            if (newTranslations != null) {
                newTranslations[frameIndex] = transform.getTranslation();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = transform.getRotation();
            }
            if (newScales != null) {
                newScales[frameIndex] = transform.getScale();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling it at the specified rate.
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param sampleRate the desired sample rate (in frames per second, &gt;0)
     * @param duration the duration of the AnimClip (in seconds, &ge;0)
     * @return a new instance
     */
    public static TransformTrack resampleAtRate(
            TransformTrack oldTrack, float sampleRate, float duration) {
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int numSamples = 1 + (int) Math.floor(duration * sampleRate);
        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; ++frameIndex) {
            float time = frameIndex / sampleRate;
            if (time > duration) {
                time = duration;
            }
            newTimes[frameIndex] = time;
        }
        TransformTrack result = resample(oldTrack, newTimes);

        return result;
    }

    /**
     * Copy a TransformTrack, resampling to the specified number of samples.
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param numSamples the desired number of samples (&ge;2)
     * @param duration the duration of the AnimClip (in seconds, &ge;0)
     * @return a new instance
     */
    public static TransformTrack resampleToNumber(
            TransformTrack oldTrack, int numSamples, float duration) {
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        Validate.positive(duration, "duration");

        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; ++frameIndex) {
            float time;
            if (frameIndex == numSamples - 1) {
                time = duration;
            } else {
                time = (frameIndex * duration) / (numSamples - 1);
            }
            newTimes[frameIndex] = time;
        }
        TransformTrack result = resample(oldTrack, newTimes);

        return result;
    }

    /**
     * Re-target the specified AnimClip from the specified source armature to
     * the specified target armature using the specified map.
     *
     * @param sourceClip the AnimClip to re-target (not null, unaffected)
     * @param sourceTrack the input joint track (not null, unaffected)
     * @param sourceArmature (not null, unaffected)
     * @param targetArmature (not null, unaffected)
     * @param targetJoint the target joint (not null, alias created)
     * @param map skeleton map to use (not null, unaffected)
     * @param cache previously calculated poses (not null, added to)
     * @return a new TransformTrack
     */
    public static TransformTrack retargetTrack(AnimClip sourceClip,
            TransformTrack sourceTrack, Armature sourceArmature,
            Armature targetArmature, Joint targetJoint, SkeletonMapping map,
            Map<Float, Pose> cache) {
        Validate.nonNull(sourceArmature, "source armature");
        Validate.nonNull(targetArmature, "target armature");
        Validate.nonNull(map, "map");
        Validate.nonNull(targetJoint, "target joint");

        float[] times = sourceTrack.getTimes(); // alias
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceArmature);
        int targetJointIndex = targetJoint.getId();

        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            float trackTime = times[frameIndex];
            Pose targetPose = cache.get(trackTime);
            if (targetPose == null) {
                targetPose = new Pose(targetArmature);
                sourcePose.setToClip(sourceClip, trackTime);
                targetPose.setToRetarget(sourcePose, map);
                cache.put(trackTime, targetPose);
            }
            Transform localTransform
                    = targetPose.localTransform(targetJointIndex, null);
            translations[frameIndex] = localTransform.getTranslation();
            rotations[frameIndex] = localTransform.getRotation();
            scales[frameIndex] = localTransform.getScale();
        }

        TransformTrack result = new TransformTrack(
                targetJoint, times, translations, rotations, scales);

        return result;
    }

    /**
     * Re-target the specified AnimClip from the specified source armature to
     * the specified target skeleton using the specified map.
     *
     * @param sourceClip the AnimClip to re-target (not null, unaffected)
     * @param sourceTrack the input joint track (not null, unaffected)
     * @param sourceArmature (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @param map skeleton map to use (not null, unaffected)
     * @param cache previously calculated poses (not null, added to)
     * @return a new BoneTrack
     */
    public static BoneTrack retargetTrack(
            AnimClip sourceClip, TransformTrack sourceTrack,
            Armature sourceArmature, Skeleton targetSkeleton,
            int targetBoneIndex, SkeletonMapping map, Map<Float, Pose> cache) {
        Validate.nonNull(sourceArmature, "source armature");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times = sourceTrack.getTimes(); // alias
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceArmature);

        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            float trackTime = times[frameIndex];
            Pose targetPose = cache.get(trackTime);
            if (targetPose == null) {
                targetPose = new Pose(targetSkeleton);
                sourcePose.setToClip(sourceClip, trackTime);
                targetPose.setToRetarget(sourcePose, map);
                cache.put(trackTime, targetPose);
            }
            Transform userTransform
                    = targetPose.userTransform(targetBoneIndex, null);
            translations[frameIndex] = userTransform.getTranslation();
            rotations[frameIndex] = userTransform.getRotation();
            scales[frameIndex] = userTransform.getScale();
        }

        BoneTrack result = new BoneTrack(
                targetBoneIndex, times, translations, rotations, scales);

        return result;
    }

    /**
     * Re-target the specified BoneTrack from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation the Animation to re-target, or null for bind pose
     * @param sourceTrack the input BoneTrack (not null, unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @param map skeleton map to use (not null, unaffected)
     * @param techniques tweening techniques to use (not null, unaffected)
     * @param cache previously calculated poses (not null, added to)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, int targetBoneIndex, SkeletonMapping map,
            TweenTransforms techniques, Map<Float, Pose> cache) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(techniques, "techniques");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times;
        int numKeyframes;
        if (sourceTrack == null) {
            numKeyframes = 1;
            times = new float[numKeyframes];
            times[0] = 0f;
        } else {
            times = sourceTrack.getKeyFrameTimes(); // alias
            numKeyframes = times.length;
            assert numKeyframes > 0 : numKeyframes;
        }
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceSkeleton);

        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            float trackTime = times[frameIndex];
            Pose targetPose = cache.get(trackTime);
            if (targetPose == null) {
                targetPose = new Pose(targetSkeleton);
                sourcePose.setToAnimation(
                        sourceAnimation, trackTime, techniques);
                targetPose.setToRetarget(sourcePose, map);
                cache.put(trackTime, targetPose);
            }
            Transform userTransform
                    = targetPose.userTransform(targetBoneIndex, null);
            translations[frameIndex] = userTransform.getTranslation();
            rotations[frameIndex] = userTransform.getRotation();
            scales[frameIndex] = userTransform.getScale();
        }

        BoneTrack result = new BoneTrack(
                targetBoneIndex, times, translations, rotations, scales);

        return result;
    }

    /**
     * Copy the specified transform/morph track, reversing the sequence of its
     * frames.
     *
     * @param inputTrack the input transform/morph track (not null, unaffected)
     * @return a new AnimTrack with the same type and target as oldTrack, with
     * t[0]=0
     */
    public static AnimTrack<?> reverse(AnimTrack<?> inputTrack) {
        Validate.nonNull(inputTrack, "input track");

        AnimTrack<?> result;
        float[] newTimes;
        float[] oldTimes = MyAnimation.getKeyFrameTimes(inputTrack); // alias
        int numFrames = oldTimes.length;
        float lastFrameTime = oldTimes[numFrames - 1];

        if (inputTrack instanceof MorphTrack) {
            MorphTrack oldMorphTrack = (MorphTrack) inputTrack;
            float[] oldWeights = oldMorphTrack.getWeights(); // alias

            // Allocate new arrays.
            newTimes = new float[numFrames];
            int numTargets = oldMorphTrack.getNbMorphTargets();
            float[] weights = new float[numFrames * numTargets];

            for (int newIndex = 0; newIndex < numFrames; ++newIndex) {
                int oldIndex = numFrames - newIndex - 1;
                newTimes[newIndex] = lastFrameTime - oldTimes[oldIndex];

                int oldStart = oldIndex * numTargets;
                int newStart = newIndex * numTargets;
                for (int j = 0; j < numTargets; ++j) {
                    float weight = oldWeights[oldStart + j];
                    weights[newStart + j] = weight;
                }
            }

            Geometry target = oldMorphTrack.getTarget();
            result = new MorphTrack(target, newTimes, weights, numTargets);

        } else {
            TransformTrack oldTransformTrack = (TransformTrack) inputTrack;

            Vector3f[] oldTranslations = oldTransformTrack.getTranslations();
            Quaternion[] oldRotations = oldTransformTrack.getRotations();
            Vector3f[] oldScales = oldTransformTrack.getScales();

            // Allocate new arrays.
            newTimes = new float[numFrames];
            Vector3f[] newTranslations = null;
            if (oldTranslations != null) {
                newTranslations = new Vector3f[numFrames];
            }
            Quaternion[] newRotations = null;
            if (oldRotations != null) {
                newRotations = new Quaternion[numFrames];
            }
            Vector3f[] newScales = null;
            if (oldScales != null) {
                newScales = new Vector3f[numFrames];
            }

            for (int newIndex = 0; newIndex < numFrames; ++newIndex) {
                int oldI = numFrames - newIndex - 1;
                newTimes[newIndex] = lastFrameTime - oldTimes[oldI];
                if (newTranslations != null) {
                    newTranslations[newIndex] = oldTranslations[oldI].clone();
                }
                if (newRotations != null) {
                    newRotations[newIndex] = oldRotations[oldI].clone();
                }
                if (newScales != null) {
                    newScales[newIndex] = oldScales[oldI].clone();
                }
            }

            HasLocalTransform target = oldTransformTrack.getTarget();
            result = new TransformTrack(
                    target, newTimes, newTranslations, newRotations, newScales);
        }

        return result;
    }

    /**
     * Copy the specified bone/spatial track, reversing the sequence of its
     * frames.
     *
     * @param inputTrack the input bone/spatial track (not null, unaffected)
     * @return a new Track of the same type as oldTrack
     */
    public static Track reverse(Track inputTrack) {
        assert inputTrack instanceof BoneTrack
                || inputTrack instanceof SpatialTrack;

        float[] oldTimes = inputTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(inputTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(inputTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(inputTrack);

        int numFrames = oldTimes.length;
        float lastFrameTime = oldTimes[numFrames - 1];

        // Allocate new arrays.
        float[] newTimes = new float[numFrames];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[numFrames];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[numFrames];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[numFrames];
        }

        for (int newIndex = 0; newIndex < numFrames; ++newIndex) {
            int oldIndex = numFrames - newIndex - 1;

            newTimes[newIndex] = lastFrameTime - oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(
                inputTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a MorphTrack, altering its duration and adjusting all its keyframe
     * times proportionately.
     *
     * @param oldTrack input track (not null, unaffected)
     * @param newDuration the desired duration (in seconds, &ge;0)
     * @return a new MorphTrack with the same target and t[0]=0
     */
    public static MorphTrack setDuration(
            MorphTrack oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        float[] oldTimes = oldTrack.getTimes(); // alias
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        float oldDuration = oldTimes[oldCount - 1] - oldTimes[0];
        assert oldDuration >= 0f : oldCount;

        float[] oldWeights = oldTrack.getWeights(); // alias

        // Allocate new arrays.
        int newCount;
        if (oldDuration == 0f && newDuration > 0f) {
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        float[] newTimes = new float[newCount];
        int numTargets = oldTrack.getNbMorphTargets();
        float[] newWeights = new float[newCount * numTargets];

        for (int frameI = 0; frameI < oldCount; ++frameI) {
            float newTime;
            if (oldDuration == 0f) {
                assert frameI == 0 : frameI;
                newTime = 0f;
            } else {
                float oldTime = oldTimes[frameI] - oldTimes[0];
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameI] = newTime;

            int startWeightI = frameI * numTargets;
            for (int j = 0; j < numTargets; ++j) {
                int weightI = startWeightI + j;
                newWeights[weightI] = oldWeights[weightI];
            }
        }
        if (oldDuration == 0f && newDuration > 0f) {
            int oldIndex = oldCount - 1;
            int newIndex = oldCount;
            newTimes[newIndex] = newDuration;

            int oldStart = oldIndex * numTargets;
            int newStart = newIndex * numTargets;
            for (int j = 0; j < numTargets; ++j) {
                float weight = oldWeights[oldStart + j];
                newWeights[newStart + j] = weight;
            }
        }

        Geometry target = oldTrack.getTarget();
        MorphTrack result
                = new MorphTrack(target, newTimes, newWeights, numTargets);

        return result;
    }

    /**
     * Copy a track, altering its duration and adjusting all its keyframe times
     * proportionately.
     *
     * @param oldTrack input track (not null, unaffected)
     * @param newDuration new duration (in seconds, &ge;0)
     * @return a new track of the same type as oldTrack
     */
    public static Track setDuration(Track oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        float oldDuration = oldTrack.getLength();
        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        assert numFrames == 1 || oldDuration > 0f : numFrames;

        Track result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes(); // alias

        for (int frameIndex = 0; frameIndex < numFrames; ++frameIndex) {
            float oldTime = oldTimes[frameIndex];
            assert oldTime <= oldDuration : oldTime;

            float newTime;
            if (oldDuration == 0f) {
                assert oldTime == 0f : oldTime;
                newTime = 0f;
            } else {
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameIndex] = newTime;
        }

        return result;
    }

    /**
     * Copy a TransformTrack, altering its duration and adjusting all its
     * keyframe times proportionately.
     *
     * @param oldTrack input track (not null, unaffected)
     * @param newDuration the desired duration (in seconds, &ge;0)
     * @return a new TransformTrack with the same target and t[0]=0
     */
    public static TransformTrack setDuration(
            TransformTrack oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "new duration");

        float[] oldTimes = oldTrack.getTimes(); // alias
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        float oldDuration = oldTimes[oldCount - 1] - oldTimes[0];
        assert oldDuration >= 0f : oldCount;

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays.
        int newCount;
        if (oldDuration == 0f && newDuration > 0f) {
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int frameI = 0; frameI < oldCount; ++frameI) {
            float newTime;
            if (oldDuration == 0f) {
                assert frameI == 0 : frameI;
                newTime = 0f;
            } else {
                float oldTime = oldTimes[frameI] - oldTimes[0];
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameI] = newTime;

            if (newTranslations != null) {
                newTranslations[frameI] = oldTranslations[frameI].clone();
            }
            if (newRotations != null) {
                newRotations[frameI] = oldRotations[frameI].clone();
            }
            if (newScales != null) {
                newScales[frameI] = oldScales[frameI].clone();
            }
        }
        if (oldDuration == 0f && newDuration > 0f) {
            int oldIndex = oldCount - 1;
            int newIndex = oldCount;
            newTimes[newIndex] = newDuration;

            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy the specified track, adjusting the animation time of the indexed
     * frame.
     *
     * @param oldTrack the track to copy (not null, unaffected)
     * @param frameIndex the index of the frame to adjust (&gt;0)
     * @param newTime new time for the frame (in seconds, &gt;0)
     * @return a new track of the same type as oldTrack, or null if unsuccessful
     */
    public static Track setFrameTime(
            Track oldTrack, int frameIndex, float newTime) {
        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        int numFrames = oldTimes.length;
        Validate.inRange(frameIndex, "frame index", 1, numFrames - 1);
        Validate.positive(newTime, "new time");

        if (newTime <= oldTimes[frameIndex - 1]) {
            return null;
        }
        if (frameIndex < numFrames - 1) {
            if (newTime >= oldTimes[frameIndex + 1]) {
                return null;
            }
        }

        Track result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes(); // alias
        newTimes[frameIndex] = newTime;

        return result;
    }

    /**
     * Alter the keyframes in a bone/spatial track.
     *
     * @param track (not null, modified)
     * @param times (not null, alias created)
     * @param translations (either null or same length as times)
     * @param rotations (either null or same length as times)
     * @param scales (either null or same length as times)
     */
    public static void setKeyframes(
            Track track, float[] times, Vector3f[] translations,
            Quaternion[] rotations, Vector3f[] scales) {
        Validate.nonNull(times, "times");
        int numKeyframes = times.length;
        assert numKeyframes > 0 : numKeyframes;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            if (scales == null) {
                boneTrack.setKeyframes(times, translations, rotations);
            } else {
                boneTrack.setKeyframes(times, translations, rotations, scales);
            }

        } else if (track instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            spatialTrack.setKeyframes(times, translations, rotations, scales);

        } else {
            throw new IllegalArgumentException(track.getClass().getName());
        }
    }

    /**
     * Copy a bone/spatial track, deleting any optional components that consist
     * entirely of identities.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @return a new track, or null if all track components in the input consist
     * entirely of identities
     */
    public static Track simplify(Track oldTrack) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;

        boolean keepTranslations = false;
        boolean keepRotations = false;
        boolean keepScales = false;

        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        for (int index = 0; index < numFrames; ++index) {
            if (oldTranslations != null) {
                Vector3f translation = oldTranslations[index];
                if (!MyVector3f.isZero(translation)) {
                    keepTranslations = true;
                }
            }
            if (oldRotations != null) {
                Quaternion rotation = oldRotations[index];
                if (!MyQuaternion.isRotationIdentity(rotation)) {
                    keepRotations = true;
                }
            }
            if (oldScales != null) {
                Vector3f scale = oldScales[index];
                if (!MyVector3f.isScaleIdentity(scale)) {
                    keepScales = true;
                }
            }
        }

        Track result = null;
        if (keepTranslations || keepRotations || keepScales) {
            if (oldTrack instanceof BoneTrack) {
                // A bone track requires both translations and rotations.
                keepTranslations = true;
                keepRotations = true;
            }
            float[] newTimes = new float[numFrames];
            Vector3f[] newTranslations = keepTranslations
                    ? new Vector3f[numFrames] : null;
            Quaternion[] newRotations = keepRotations
                    ? new Quaternion[numFrames] : null;
            Vector3f[] newScales = keepScales ? new Vector3f[numFrames] : null;

            for (int index = 0; index < numFrames; ++index) {
                newTimes[index] = oldTimes[index];
                if (keepTranslations) {
                    newTranslations[index] = oldTranslations[index].clone();
                }
                if (keepRotations) {
                    newRotations[index] = oldRotations[index].clone();
                }
                if (keepScales) {
                    newScales[index] = oldScales[index].clone();
                }
            }
            result = newTrack(oldTrack, newTimes, newTranslations,
                    newRotations, newScales);
        }

        return result;
    }

    /**
     * Copy a TransformTrack, deleting any components that consist entirely of
     * identities.
     *
     * @param oldTrack the input track (not null, unaffected)
     * @return a new TransformTrack with the same target
     */
    public static TransformTrack simplify(TransformTrack oldTrack) {
        boolean keepScales = false;

        float[] oldTimes = oldTrack.getTimes(); // alias
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;

        Vector3f[] oldScales = oldTrack.getScales();
        if (oldScales != null) {
            for (int index = 0; index < numFrames; ++index) {
                Vector3f scale = oldScales[index];
                if (!MyVector3f.isScaleIdentity(scale)) {
                    keepScales = true;
                    break;
                }
            }
        }

        boolean keepTranslations = false;
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        if (oldTranslations != null) {
            for (int index = 0; index < numFrames; ++index) {
                Vector3f translation = oldTranslations[index];
                if (!MyVector3f.isZero(translation)) {
                    keepTranslations = true;
                    break;
                }
            }
        }

        boolean keepRotations = false;
        Quaternion[] oldRotations = oldTrack.getRotations();
        if (oldRotations != null) {
            for (int index = 0; index < numFrames; ++index) {
                Quaternion rotation = oldRotations[index];
                if (!MyQuaternion.isRotationIdentity(rotation)) {
                    keepRotations = true;
                    break;
                }
            }
        }

        float[] newTimes = new float[numFrames];
        Vector3f[] newTranslations
                = keepTranslations ? new Vector3f[numFrames] : null;
        Quaternion[] newRotations
                = keepRotations ? new Quaternion[numFrames] : null;
        Vector3f[] newScales = keepScales ? new Vector3f[numFrames] : null;

        for (int index = 0; index < numFrames; ++index) {
            newTimes[index] = oldTimes[index];
            if (keepTranslations) {
                newTranslations[index] = oldTranslations[index].clone();
            }
            if (keepRotations) {
                newRotations[index] = oldRotations[index].clone();
            }
            if (keepScales) {
                newScales[index] = oldScales[index].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, deleting the scale component if it consists
     * entirely of approximate identities. TODO handle translation and rotation
     * identities
     *
     * @param oldTrack the input track (not null, unaffected)
     * @param tolerance the tolerance to use when testing for identity (&ge;0)
     * @return a new TransformTrack with the same target
     */
    public static TransformTrack simplify(
            TransformTrack oldTrack, float tolerance) {
        Validate.nonNegative(tolerance, "tolerance");
        boolean keepScales = false;

        float[] oldTimes = oldTrack.getTimes(); // alias
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        Vector3f[] oldScales = oldTrack.getScales();
        if (oldScales != null) {
            for (int index = 0; index < numFrames; ++index) {
                Vector3f scale = oldScales[index];
                if (!scale.isSimilar(scaleIdentity, tolerance)) {
                    keepScales = true;
                    break;
                }
            }
        }

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();

        float[] newTimes = new float[numFrames];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[numFrames];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[numFrames];
        }
        Vector3f[] newScales = keepScales ? new Vector3f[numFrames] : null;

        for (int frameI = 0; frameI < numFrames; ++frameI) {
            newTimes[frameI] = oldTimes[frameI];
            if (newTranslations != null) {
                newTranslations[frameI] = oldTranslations[frameI].clone();
            }
            if (newRotations != null) {
                newRotations[frameI] = oldRotations[frameI].clone();
            }
            if (keepScales) {
                newScales[frameI] = oldScales[frameI].clone();
            }
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, smoothing it using the specified techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param width width of time window (&ge;0, &le;duration)
     * @param smoothTranslations technique for translations (not null)
     * @param smoothRotations technique for translations (not null)
     * @param smoothScales technique for scales (not null)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new track of the same type as oldTrack
     */
    public static Track smooth(Track oldTrack, float width,
            SmoothVectors smoothTranslations, SmoothRotations smoothRotations,
            SmoothVectors smoothScales, float duration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.inRange(width, "width", 0f, duration);
        Validate.nonNegative(duration, "duration");

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        // Allocate new arrays.
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        float[] newTimes = new float[numFrames];
        System.arraycopy(oldTimes, 0, newTimes, 0, numFrames);

        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = smoothTranslations.smooth(
                    oldTimes, duration, oldTranslations, width, null);
        }

        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = smoothRotations.smooth(
                    oldTimes, duration, oldRotations, width, null);
        }

        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = smoothScales.smooth(
                    oldTimes, duration, oldScales, width, null);
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, smoothing it using the specified techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param width width of time window (&ge;0, &le;duration)
     * @param smoothTranslations technique for translations (not null)
     * @param smoothRotations technique for translations (not null)
     * @param smoothScales technique for scales (not null)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new track of the same type as oldTrack
     */
    public static TransformTrack smooth(TransformTrack oldTrack, float width,
            SmoothVectors smoothTranslations, SmoothRotations smoothRotations,
            SmoothVectors smoothScales, float duration) {
        Validate.inRange(width, "width", 0f, duration);
        Validate.nonNegative(duration, "duration");

        float[] oldTimes = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays.
        int numFrames = oldTimes.length;
        assert numFrames > 0 : numFrames;
        float[] newTimes = new float[numFrames];
        System.arraycopy(oldTimes, 0, newTimes, 0, numFrames);

        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = smoothTranslations.smooth(
                    oldTimes, duration, oldTranslations, width, null);
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = smoothRotations.smooth(
                    oldTimes, duration, oldRotations, width, null);
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = smoothScales.smooth(
                    oldTimes, duration, oldScales, width, null);
        }

        HasLocalTransform target = oldTrack.getTarget();
        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a MorphTrack, truncating it at the specified time.
     *
     * @param oldTrack input MorphTrack (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @param endWeights weights at the end time (not null, unaffected)
     * @return a new MorphTrack with the same target and t[0]=0
     */
    public static MorphTrack truncate(
            MorphTrack oldTrack, float endTime, float[] endWeights) {
        Validate.nonNegative(endTime, "end time");
        Validate.nonNull(endWeights, "end weights");

        // Access the old arrays.
        float[] oldTimes = oldTrack.getTimes(); // alias
        float[] oldWeights = oldTrack.getWeights(); // alias

        // Allocate new arrays. Avoid creating a repetitious keyframe.
        int lastFrame = MyArray.findPreviousIndex(endTime, oldTimes);
        int newCount = lastFrame + 1;
        if (oldTimes[lastFrame] != endTime) {
            ++newCount;
        }
        lastFrame = newCount - 1;
        float[] newTimes = new float[newCount];
        int numTargets = oldTrack.getNbMorphTargets();
        float[] newWeights = new float[newCount * numTargets];

        for (int frameI = 0; frameI < lastFrame; ++frameI) {
            newTimes[frameI] = oldTimes[frameI] - oldTimes[0];
            int startWeightI = frameI * numTargets;
            for (int j = 0; j < numTargets; ++j) {
                int weightI = startWeightI + j;
                newWeights[weightI] = oldWeights[weightI];
            }
        }

        newTimes[lastFrame] = endTime;
        int startWeightI = lastFrame * numTargets;
        for (int j = 0; j < numTargets; ++j) {
            newWeights[startWeightI + j] = endWeights[j];
        }

        Geometry target = oldTrack.getTarget();
        MorphTrack result
                = new MorphTrack(target, newTimes, newWeights, numTargets);

        assert (float) result.getLength() == endTime;
        return result;
    }

    /**
     * Copy a bone/spatial track, truncating it at the specified time.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @return a new track of the same type as oldTrack
     */
    public static Track truncate(Track oldTrack, float endTime) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.nonNegative(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int newCount
                = 1 + MyAnimation.findPreviousKeyframeIndex(oldTrack, endTime);

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int frameIndex = 0; frameIndex < newCount; ++frameIndex) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex]
                        = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, truncating it at the specified time.
     *
     * @param oldTrack input TransformTrack (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @param endTransform transform of target at the end time (unaffected)
     * @return a new TransformTrack with the same target and t[0]=0
     */
    public static TransformTrack truncate(
            TransformTrack oldTrack, float endTime, Transform endTransform) {
        Validate.nonNegative(endTime, "end time");

        // Access the old arrays.
        float[] oldTimes = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        // Allocate new arrays. Avoid creating a repetitious keyframe.
        int lastFrame
                = MyAnimation.findPreviousKeyframeIndex(oldTrack, endTime);
        int newCount = lastFrame + 1;
        if (oldTimes[lastFrame] != endTime) {
            ++newCount;
        }
        lastFrame = newCount - 1;
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = new Vector3f[newCount];

        HasLocalTransform target = oldTrack.getTarget();
        Transform fillData = target.getLocalTransform();

        for (int frameI = 0; frameI < lastFrame; ++frameI) {
            newTimes[frameI] = oldTimes[frameI] - oldTimes[0];
            if (oldTranslations == null) {
                newTranslations[frameI] = fillData.getTranslation().clone();
            } else {
                newTranslations[frameI] = oldTranslations[frameI].clone();
            }
            if (oldRotations == null) {
                newRotations[frameI] = fillData.getRotation().clone();
            } else {
                newRotations[frameI] = oldRotations[frameI].clone();
            }
            if (oldScales == null) {
                newScales[frameI] = fillData.getScale().clone();
            } else {
                newScales[frameI] = oldScales[frameI].clone();
            }
        }

        newTimes[lastFrame] = endTime;
        newTranslations[lastFrame] = endTransform.getTranslation().clone();
        newRotations[lastFrame] = endTransform.getRotation().clone();
        newScales[lastFrame] = endTransform.getScale().clone();

        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        assert (float) result.getLength() == endTime;
        return result;
    }

    /**
     * Copy a bone/spatial track, altering the track's first keyframe and
     * end-time keyframe so that they precisely match. If the track doesn't end
     * with a keyframe, append one.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param duration duration of the animation (in seconds, &gt;0)
     * @param endWeight how much weight to give to the pre-existing end-time
     * keyframe, if one exists (&ge;0, &le;1)
     * @return a new Track of the same type as oldTrack
     */
    public static Track wrap(Track oldTrack, float duration, float endWeight) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(duration, "duration");
        Validate.fraction(endWeight, "end weight");

        float[] oldTimes = oldTrack.getKeyFrameTimes(); // alias
        Vector3f[] oldTranslations = MyAnimation.copyTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.copyRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.copyScales(oldTrack);

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount;
        Vector3f wrapTranslation = new Vector3f();
        Quaternion wrapRotation = new Quaternion();
        Vector3f wrapScale = new Vector3f();
        int endIndex = MyAnimation.findKeyframeIndex(oldTrack, duration);
        if (endIndex == -1) { // doesn't end with a keyframe, ignore endWeight
            endIndex = oldCount;
            newCount = oldCount + 1;
            if (oldTranslations != null) {
                wrapTranslation.set(oldTranslations[0]);
            }
            if (oldRotations != null) {
                wrapRotation.set(oldRotations[0]);
            }
            if (oldScales != null) {
                wrapScale.set(oldScales[0]);
            }
        } else {
            newCount = oldCount;
            if (oldTranslations != null) {
                MyVector3f.lerp(endWeight, oldTranslations[0],
                        oldTranslations[endIndex], wrapTranslation);
            }
            if (oldRotations != null) {
                MyQuaternion.slerp(endWeight, oldRotations[0],
                        oldRotations[endIndex], wrapRotation);
            }
            if (oldScales != null) {
                MyVector3f.lerp(endWeight, oldScales[0], oldScales[endIndex],
                        wrapScale);
            }
        }
        assert endIndex == newCount - 1;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        newTimes[endIndex] = duration;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = wrapTranslation.clone();
            newTranslations[endIndex] = wrapTranslation.clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = wrapRotation.clone();
            newRotations[endIndex] = wrapRotation.clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = wrapScale.clone();
            newScales[endIndex] = wrapScale.clone();
        }

        for (int frameIndex = 1; frameIndex < endIndex; ++frameIndex) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex]
                        = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        Track result = newTrack(
                oldTrack, newTimes, newTranslations, newRotations, newScales);

        return result;
    }

    /**
     * Copy a TransformTrack, altering the track's first keyframe and end-time
     * keyframe so that they precisely match. If the track doesn't end with a
     * keyframe, append one.
     *
     * @param oldTrack the input TransformTrack (not null, unaffected)
     * @param duration duration of the animation (in seconds, &gt;0)
     * @param endWeight how much weight to give to the pre-existing end-time
     * keyframe, if one exists (&ge;0, &le;1)
     * @return a new TransformTrack
     */
    public static TransformTrack wrap(
            TransformTrack oldTrack, float duration, float endWeight) {
        Validate.positive(duration, "duration");
        Validate.fraction(endWeight, "end weight");

        float[] oldTimes = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount;

        HasLocalTransform target = oldTrack.getTarget();
        Transform fillData = target.getLocalTransform();
        Vector3f wrapTranslation = fillData.getTranslation().clone();
        Quaternion wrapRotation = fillData.getRotation().clone();
        Vector3f wrapScale = fillData.getScale().clone();

        int endIndex = MyArray.findPreviousIndex(duration, oldTimes);
        if (endIndex >= 0 && oldTimes[endIndex] != duration) {
            endIndex = -1;
        }

        if (endIndex == -1) { // doesn't end with a keyframe, ignore endWeight
            endIndex = oldCount;
            newCount = oldCount + 1;
            if (oldTranslations != null) {
                wrapTranslation.set(oldTranslations[0]);
            }
            if (oldRotations != null) {
                wrapRotation.set(oldRotations[0]);
            }
            if (oldScales != null) {
                wrapScale.set(oldScales[0]);
            }
        } else {
            newCount = oldCount;
            if (oldTranslations != null) {
                MyVector3f.lerp(endWeight, oldTranslations[0],
                        oldTranslations[endIndex], wrapTranslation);
            }
            if (oldRotations != null) {
                MyQuaternion.slerp(endWeight, oldRotations[0],
                        oldRotations[endIndex], wrapRotation);
            }
            if (oldScales != null) {
                MyVector3f.lerp(endWeight, oldScales[0], oldScales[endIndex],
                        wrapScale);
            }
        }
        assert endIndex == newCount - 1;

        // Allocate new arrays.
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        newTimes[endIndex] = duration;

        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = wrapTranslation.clone();
            newTranslations[endIndex] = wrapTranslation.clone();
        }

        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = wrapRotation.clone();
            newRotations[endIndex] = wrapRotation.clone();
        }

        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = wrapScale.clone();
            newScales[endIndex] = wrapScale.clone();
        }

        for (int frameIndex = 1; frameIndex < endIndex; ++frameIndex) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex]
                        = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        TransformTrack result = new TransformTrack(
                target, newTimes, newTranslations, newRotations, newScales);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Blend 2 rotations, skipping any nulls.
     *
     * @param weight2 how much weight to give to rot2, if neither rot1 nor rot2
     * is null (&ge;0, &le;1)
     * @param rot1 the first input rotation (may be null, unaffected)
     * @param rot2 the 2nd input rotation (may be null, unaffected)
     * @return a new quaternion
     */
    private static Quaternion blendRotations(
            float weight2, Quaternion rot1, Quaternion rot2) {
        Quaternion result;
        if (rot1 == null) {
            if (rot2 == null) {
                result = new Quaternion(); // identity
            } else {
                result = rot2.clone();
            }
        } else if (rot2 == null) {
            result = rot1.clone();
        } else {
            result = MyQuaternion.slerp(weight2, rot1, rot2, null);
        }

        return result;
    }

    /**
     * Blend 2 scale vectors, skipping any nulls.
     *
     * @param weight2 how much weight to give to scale2, if neither scale1 nor
     * scale2 is null (&ge;0, &le;1)
     * @param scale1 the first input vector (may be null, unaffected)
     * @param scale2 the 2nd input vector (may be null, unaffected)
     * @return a new vector
     */
    private static Vector3f blendScales(
            float weight2, Vector3f scale1, Vector3f scale2) {
        Vector3f result;
        if (scale1 == null) {
            if (scale2 == null) {
                result = new Vector3f(1f, 1f, 1f);
            } else {
                result = scale2.clone();
            }
        } else if (scale2 == null) {
            result = scale1.clone();
        } else {
            result = MyVector3f.lerp(weight2, scale1, scale2, null);
        }

        return result;
    }

    /**
     * Blend 2 translation vectors, skipping any nulls.
     *
     * @param weight2 how much weight to give to tra2, if neither tra1 nor tra2
     * is null (&ge;0, &le;1)
     * @param tra1 the first input vector (may be null, unaffected)
     * @param tra2 the 2nd input vector (may be null, unaffected)
     * @return a new vector
     */
    private static Vector3f blendTranslations(
            float weight2, Vector3f tra1, Vector3f tra2) {
        Vector3f result;
        if (tra1 == null) {
            if (tra2 == null) {
                result = new Vector3f();
            } else {
                result = tra2.clone();
            }
        } else if (tra2 == null) {
            result = tra1.clone();
        } else {
            result = MyVector3f.lerp(weight2, tra1, tra2, null);
        }

        return result;
    }
}
