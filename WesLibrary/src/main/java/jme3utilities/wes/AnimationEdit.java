/*
 Copyright (c) 2017-2022, Stephen Gold
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;

/**
 * Utility methods for editing JME animations.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class AnimationEdit {
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
     * Add the specified AnimTrack to the specified AnimClip.
     *
     * @param clip (not null, alias created)
     * @param track (not null, modified)
     */
    public static void addTrack(AnimClip clip, AnimTrack<?> track) {
        Validate.nonNull(track, "track");

        AnimTrack<?>[] oldTracks = clip.getTracks();
        AnimTrack<?>[] newTracks;
        if (oldTracks == null) {
            newTracks = new AnimTrack[1];
            newTracks[0] = track;
        } else {
            int oldNumTracks = oldTracks.length;
            newTracks = new AnimTrack[oldNumTracks + 1];
            System.arraycopy(oldTracks, 0, newTracks, 0, oldNumTracks);
            newTracks[oldNumTracks] = track;
        }

        clip.setTracks(newTracks);
    }

    /**
     * Copy the specified AnimClip, converting it from a travelling animation to
     * an in-place animation. Rotations and scales aren't considered. Works best
     * on cyclic animations.
     *
     * @param sourceClip the AnimClip to convert (not null, unaffected)
     * @param resultName name for the resulting AnimClip (not null)
     * @return a new AnimClip
     */
    public static AnimClip convertToInPlace(AnimClip sourceClip,
            String resultName) {
        Validate.nonNull(resultName, "result name");

        // Start with an empty AnimClip.
        AnimClip result = new AnimClip(resultName);

        AnimTrack<?>[] tracks = sourceClip.getTracks();
        for (AnimTrack<?> oldTrack : tracks) {
            AnimTrack<?> newTrack = null;
            /*
             * If the track is a TransformTrack with 2 or more unique
             * translations, zero out any average linear velocity
             */
            if (oldTrack instanceof TransformTrack) {
                TransformTrack transformTrack = (TransformTrack) oldTrack;

                Vector3f[] translations = transformTrack.getTranslations();
                if (translations != null) {
                    int numUnique = MyArray.countNe(translations);
                    if (numUnique >= 2) {
                        newTrack = TrackEdit.convertToInPlace(transformTrack);
                    }
                }
            }

            // If the track couldn't be converted, clone it.
            if (newTrack == null) {
                newTrack = TrackEdit.cloneTrack(oldTrack);
            }
            addTrack(result, newTrack);
        }

        return result;
    }

    /**
     * Extract a range from the specified Animation.
     *
     * @param sourceAnimation the Animation to extract from (not null,
     * unaffected)
     * @param startTime the start of the range (in seconds, &gt;0, &le;endTime)
     * @param endTime the end of the range (in seconds, &ge;startTime)
     * @param techniques tweening techniques to use (unaffected)
     * @param newAnimationName a name for the resulting Animation (not null)
     * @return a new Animation
     */
    public static Animation extractAnimation(Animation sourceAnimation,
            float startTime, float endTime, TweenTransforms techniques,
            String newAnimationName) {
        Validate.nonNull(sourceAnimation, "source animation");
        Validate.inRange(startTime, "start time", 0f, endTime);
        Validate.inRange(endTime, "end time", startTime, Float.MAX_VALUE);
        Validate.nonNull(newAnimationName, "new animation name");
        /*
         * Start with an empty Animation.
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
     * Extract a range from the specified AnimClip.
     *
     * @param sourceClip the AnimClip to extract from (not null, unaffected)
     * @param startTime the start of the range (in seconds, &gt;0, &le;endTime)
     * @param endTime the end of the range (in seconds, &ge;startTime)
     * @param techniques tweening techniques to use (unaffected)
     * @param newClipName a name for the resulting AnimClip (not null)
     * @return a new AnimClip
     */
    public static AnimClip extractAnimation(AnimClip sourceClip,
            float startTime, float endTime, TweenTransforms techniques,
            String newClipName) {
        Validate.nonNull(sourceClip, "source clip");
        Validate.inRange(startTime, "start time", 0f, endTime);
        Validate.inRange(endTime, "end time", startTime, Float.MAX_VALUE);
        Validate.nonNull(newClipName, "new clip name");
        /*
         * Start with an empty AnimClip.
         */
        AnimClip result = new AnimClip(newClipName);

        AnimTrack<?>[] sourceTracks = sourceClip.getTracks();
        for (AnimTrack<?> sourceTrack : sourceTracks) {
            if (sourceTrack instanceof MorphTrack) {
                throw new UnsupportedOperationException(); // TODO
            } else {
                TransformTrack oldTrack = (TransformTrack) sourceTrack;
                Transform endTransform
                        = techniques.interpolate(endTime, oldTrack, null);
                TransformTrack newTrack
                        = TrackEdit.truncate(oldTrack, endTime, endTransform);
                if (startTime > 0f) {
                    Transform startTransform
                            = techniques.interpolate(startTime, oldTrack, null);
                    newTrack = TrackEdit.behead(newTrack, startTime,
                            startTransform);
                }
                addTrack(result, newTrack);
            }
        }

        return result;
    }

    /**
     * Normalize all quaternions in an Animation.
     *
     * @param animation (not null, modified)
     * @param tolerance for norms (&ge;0)
     * @return the number of tracks edited (&ge;0)
     */
    public static int normalizeQuaternions(Animation animation,
            float tolerance) {
        Validate.nonNegative(tolerance, "tolerance");

        Track[] tracks = animation.getTracks();
        int numTracks = tracks.length;

        int numTracksEdited = 0;
        for (int trackIndex = 0; trackIndex < numTracks; ++trackIndex) {
            Track oldTrack = tracks[trackIndex];
            if (oldTrack instanceof BoneTrack
                    || oldTrack instanceof SpatialTrack) {
                Track newTrack
                        = TrackEdit.normalizeQuaternions(oldTrack, tolerance);
                if (oldTrack != newTrack) {
                    ++numTracksEdited;
                    tracks[trackIndex] = newTrack;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Normalize all quaternions in an AnimClip.
     *
     * @param clip (not null, modified)
     * @param tolerance for norms (&ge;0)
     * @return the number of tracks edited (&ge;0)
     */
    public static int normalizeQuaternions(AnimClip clip, float tolerance) {
        Validate.nonNegative(tolerance, "tolerance");

        AnimTrack<?>[] tracks = clip.getTracks();
        int numTracks = tracks.length;

        int numTracksEdited = 0;
        for (int trackIndex = 0; trackIndex < numTracks; ++trackIndex) {
            AnimTrack<?> oldTrack = tracks[trackIndex];
            AnimTrack<?> newTrack
                    = TrackEdit.normalizeQuaternions(oldTrack, tolerance);
            if (oldTrack != newTrack) {
                ++numTracksEdited;
                tracks[trackIndex] = newTrack;
            }
        }

        return numTracksEdited;
    }

    /**
     * Remove repetitious keyframes from an Animation.
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
     * Remove repetitious keyframes from an AnimClip.
     *
     * @param clip (not null, modified)
     * @return the number of tracks edited (&ge;0)
     */
    public static int removeRepeats(AnimClip clip) {
        int numTracksEdited = 0;
        AnimTrack<?>[] tracks = clip.getTracks();
        for (AnimTrack<?> track : tracks) {
            boolean removed;
            if (track instanceof MorphTrack) {
                removed = TrackEdit.removeRepeats((MorphTrack) track);
            } else {
                removed = TrackEdit.removeRepeats((TransformTrack) track);
            }
            if (removed) {
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }

    /**
     * Re-target the specified Animation from the specified source Skeleton to
     * the specified target Skeleton using the specified map.
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
        for (int iTarget = 0; iTarget < numTargetBones; ++iTarget) {
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
     * Re-target the specified Animation from the specified source armature to
     * the specified target skeleton using the specified map.
     *
     * @param sourceClip which AnimClip to re-target (not null, unaffected)
     * @param sourceArmature (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     * @param animationName name for the resulting Animation (not null)
     * @return a new Animation
     */
    public static Animation retargetAnimation(AnimClip sourceClip,
            Armature sourceArmature, Skeleton targetSkeleton,
            SkeletonMapping map, String animationName) {
        Validate.nonNull(sourceArmature, "source armature");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty Animation.
         */
        float duration = (float) sourceClip.getLength();
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each target bone that's mapped.
         */
        Map<Float, Pose> cache = new TreeMap<>();
        int numTargetBones = targetSkeleton.getBoneCount();
        for (int iTarget = 0; iTarget < numTargetBones; ++iTarget) {
            Bone targetBone = targetSkeleton.getBone(iTarget);
            String targetName = targetBone.getName();
            BoneMapping boneMapping = map.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceArmature.getJointIndex(sourceName);
                TransformTrack sourceTrack
                        = MyAnimation.findJointTrack(sourceClip, iSource);
                BoneTrack track = TrackEdit.retargetTrack(sourceClip,
                        sourceTrack, sourceArmature, targetSkeleton, iTarget,
                        map, cache);
                result.addTrack(track);
            }
        }
        /*
         * Convert any non-joint tracks.
         */
        AnimTrack<?>[] tracks = sourceClip.getTracks();
        for (AnimTrack<?> track : tracks) {
            if (!MyAnimation.isJointTrack(track)) {
                // TODO
            }
        }

        return result;
    }

    /**
     * Re-target the specified AnimClip from the specified source armature to
     * the specified target armature using the specified map.
     *
     * @param sourceClip which AnimClip to re-target (not null, unaffected)
     * @param sourceArmature (not null, unaffected)
     * @param targetArmature (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     * @param clipName name for the resulting AnimClip (not null)
     * @return a new AnimClip
     */
    public static AnimClip retargetAnimation(AnimClip sourceClip,
            Armature sourceArmature, Armature targetArmature,
            SkeletonMapping map, String clipName) {
        Validate.nonNull(sourceArmature, "source armature");
        Validate.nonNull(targetArmature, "target armature");
        Validate.nonNull(map, "map");
        Validate.nonNull(clipName, "clip name");
        /*
         * Start with an empty AnimClip.
         */
        AnimClip result = new AnimClip(clipName);
        /*
         * Add a TransformTrack for each target joint that's mapped.
         */
        Map<Float, Pose> cache = new TreeMap<>();
        int numTargetJoints = targetArmature.getJointCount();
        for (int iTarget = 0; iTarget < numTargetJoints; ++iTarget) {
            Joint targetJoint = targetArmature.getJoint(iTarget);
            String targetName = targetJoint.getName();
            BoneMapping boneMapping = map.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceArmature.getJointIndex(sourceName);
                TransformTrack sourceTrack
                        = MyAnimation.findJointTrack(sourceClip, iSource);
                TransformTrack newTrack = TrackEdit.retargetTrack(sourceClip,
                        sourceTrack, sourceArmature, targetArmature,
                        targetJoint, map, cache);

                int numSamples = newTrack.getTimes().length;
                assert numSamples > 0 : numSamples;
                assert newTrack.getTranslations().length == numSamples;
                assert newTrack.getRotations().length == numSamples;
                assert newTrack.getScales().length == numSamples;
                assert newTrack.getLength() == sourceTrack.getLength();

                addTrack(result, newTrack);
            }
        }
        /*
         * Copy any non-joint tracks.
         */
        AnimTrack<?>[] tracks = sourceClip.getTracks();
        for (AnimTrack<?> track : tracks) {
            if (!MyAnimation.isJointTrack(track)) {
                AnimTrack<?> clone = Heart.deepCopy(track);
                addTrack(result, clone);
            }
        }
        assert result.getLength() == sourceClip.getLength();

        return result;
    }

    /**
     * Reverse the specified AnimClip.
     *
     * @param sourceClip the AnimClip to reverse (not null, unaffected)
     * @param animationName name for the resulting AnimClip (not null)
     * @return a new AnimClip
     */
    public static AnimClip reverseAnimation(AnimClip sourceClip,
            String animationName) {
        Validate.nonNull(animationName, "animation name");

        AnimClip result = new AnimClip(animationName);

        AnimTrack<?>[] forwardTracks = sourceClip.getTracks();
        for (AnimTrack<?> forwardTrack : forwardTracks) {
            AnimTrack<?> newTrack = TrackEdit.reverse(forwardTrack);
            assert newTrack.getLength() == forwardTrack.getLength();
            addTrack(result, newTrack);
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
     * Alter the duration of the specified AnimClip.
     *
     * @param sourceClip the AnimClip to alter (not null, unaffected)
     * @param newDuration the desired duration (in seconds, &ge;0)
     * @param animationName name for the resulting AnimClip (not null)
     * @return a new AnimClip
     */
    public static AnimClip setDuration(AnimClip sourceClip, float newDuration,
            String animationName) {
        Validate.nonNull(animationName, "animation name");

        AnimClip result = new AnimClip(animationName);

        AnimTrack<?>[] oldTracks = sourceClip.getTracks();
        for (AnimTrack<?> sourceTrack : oldTracks) {
            AnimTrack<?> newTrack;
            if (sourceTrack instanceof MorphTrack) {
                MorphTrack oldTrack = (MorphTrack) sourceTrack;
                newTrack = TrackEdit.setDuration(oldTrack, newDuration);
            } else {
                TransformTrack oldTrack = (TransformTrack) sourceTrack;
                newTrack = TrackEdit.setDuration(oldTrack, newDuration);
            }
            assert newTrack.getLength() == newDuration;
            addTrack(result, newTrack);
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

    /**
     * Repair all tracks in which the first keyframe isn't at time=0.
     *
     * @param clip the AnimClip to repair (not null)
     * @return the number of tracks edited (&ge;0)
     */
    public static int zeroFirst(AnimClip clip) {
        int numTracksEdited = 0;
        AnimTrack<?>[] tracks = clip.getTracks();
        for (AnimTrack<?> track : tracks) {
            float[] times;
            if (track instanceof MorphTrack) {
                times = ((MorphTrack) track).getTimes();
            } else {
                times = ((TransformTrack) track).getTimes();
            }
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
