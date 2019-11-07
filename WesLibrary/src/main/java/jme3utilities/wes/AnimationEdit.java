/*
 Copyright (c) 2017-2019, Stephen Gold
 All rights reserved.

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

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

/**
 * Utility methods for editing JME animations.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class AnimationEdit {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AnimationEdit.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private AnimationEdit() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Extract a range from the specified animation.
     *
     * @param sourceAnimation the animation to extract from (not null,
     * unaffected)
     * @param startTime the start of the range (in seconds, &gt;0, &le;endTime)
     * @param endTime the end of the range (in seconds, &ge;startTime)
     * @param techniques tweening techniques to use (unaffected)
     * @param newAnimationName a name for the resulting animation (not null)
     * @return a new animation
     */
    public static Animation extractAnimation(Animation sourceAnimation,
            float startTime, float endTime, TweenTransforms techniques,
            String newAnimationName) {
        Validate.nonNull(sourceAnimation, "source animation");
        Validate.inRange(startTime, "start time", 0f, endTime);
        Validate.inRange(endTime, "end time", startTime, Float.MAX_VALUE);
        Validate.nonNull(newAnimationName, "new animation name");
        /*
         * Start with an empty animation.
         */
        float newDuration = endTime - startTime;
        Animation result = new Animation(newAnimationName, newDuration);

        float sourceDuration = sourceAnimation.getLength();
        Track[] sourceTracks = sourceAnimation.getTracks();
        for (Track sourceTrack : sourceTracks) {
            Track newTrack;
            if (sourceTrack instanceof BoneTrack
                    || sourceTrack instanceof SpatialTrack) {
                newTrack = TrackEdit.truncate(sourceTrack, endTime);
                if (startTime > 0f) {
                    Transform startTransform = techniques.interpolate(startTime,
                            sourceTrack, sourceDuration, null, null);
                    newTrack = TrackEdit.behead(newTrack, startTime,
                            startTransform, endTime);
                }
            } else {
                newTrack = sourceTrack.clone(); // TODO other track types
            }
            result.addTrack(newTrack);
        }

        return result;
    }

    /**
     * Normalize all quaternions in an animation.
     *
     * @param animation (not null, modified)
     * @return the number of tracks edited (&ge;0)
     */
    public static int normalizeQuaternions(Animation animation) {
        Track[] tracks = animation.getTracks();
        int numTracks = tracks.length;

        int numTracksEdited = 0;
        for (int trackIndex = 0; trackIndex < numTracks; ++trackIndex) {
            Track oldTrack = tracks[trackIndex];
            if (oldTrack instanceof BoneTrack || oldTrack instanceof SpatialTrack) {
                Track newTrack = TrackEdit.normalizeQuaternions(oldTrack);
                if (oldTrack != newTrack) {
                    ++numTracksEdited;
                    tracks[trackIndex] = newTrack;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Remove repetitious keyframes from an animation.
     *
     * @param animation (not null, modified)
     * @return the number of tracks edited (&ge;0)
     */
    public static int removeRepeats(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                boolean removed = TrackEdit.removeRepeats(track);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Re-target the specified Animation from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation which Animation to re-target (not null,
     * unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     * @param techniques the tweening techniques to use (not null, unaffected)
     * @param animationName name for the resulting Animation (not null)
     * @return a new Animation
     */
    public static Animation retargetAnimation(Animation sourceAnimation,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping map, TweenTransforms techniques,
            String animationName) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(techniques, "techniques");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty Animation.
         */
        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each target bone that's mapped.
         */
        Map<Float, Pose> cache = new TreeMap<>();
        int numTargetBones = targetSkeleton.getBoneCount();
        for (int iTarget = 0; iTarget < numTargetBones; iTarget++) {
            Bone targetBone = targetSkeleton.getBone(iTarget);
            String targetName = targetBone.getName();
            BoneMapping boneMapping = map.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceSkeleton.getBoneIndex(sourceName);
                BoneTrack sourceTrack
                        = MyAnimation.findBoneTrack(sourceAnimation, iSource);
                BoneTrack track = TrackEdit.retargetTrack(sourceAnimation,
                        sourceTrack, sourceSkeleton, targetSkeleton, iTarget,
                        map, techniques, cache);
                result.addTrack(track);
            }
        }
        /*
         * Copy any non-bone tracks.
         */
        Track[] tracks = sourceAnimation.getTracks();
        for (Track track : tracks) {
            if (!(track instanceof BoneTrack)) {
                Track clone = track.clone();
                result.addTrack(clone);
            }
        }

        return result;
    }

    /**
     * Reverse the specified Animation. All tracks in the Animation must be
     * bone/spatial tracks.
     *
     * @param sourceAnimation which Animation to reverse (not null, unaffected)
     * @param animationName name for the resulting Animation (not null)
     * @return a new Animation
     */
    public static Animation reverseAnimation(Animation sourceAnimation,
            String animationName) {
        Validate.nonNull(animationName, "animation name");

        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);

        Track[] forwardTracks = sourceAnimation.getTracks();
        for (Track forwardTrack : forwardTracks) {
            Track newTrack = TrackEdit.reverse(forwardTrack);
            result.addTrack(newTrack);
        }

        return result;
    }

    /**
     * Repair all tracks in which the first keyframe isn't at time=0.
     *
     * @param animation (not null)
     * @return the number of tracks edited (&ge;0)
     */
    public static int zeroFirst(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            float[] times = track.getKeyFrameTimes(); // an alias
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
