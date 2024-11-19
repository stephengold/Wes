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
import com.jme3.anim.SkinningControl;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyQuaternion;

/**
 * Encapsulate a pose for a specific Armature or Skeleton.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Pose implements JmeCloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(Pose.class.getName());
    // *************************************************************************
    // fields

    /**
     * Armature on which this Pose is based, or null for none
     * <p>
     * This Armature provides the name, index, parent, children, and inverse
     * bind transform of each Joint. The bind transforms are calculated during
     * instantiation. All other Joint data are disregarded.
     */
    private Armature armature;
    /**
     * local bind transform for each Joint (not set for a Skeleton)
     */
    private List<Transform> bindTransforms;
    /**
     * user/animation transforms that describe this Pose, one for each armature
     * joint or skeleton bone
     */
    private List<Transform> transforms;
    /**
     * Skeleton on which this Pose is based, or null for none
     * <p>
     * This Skeleton provides the name, index, parent, children, bind transform,
     * and inverse bind transform of each Bone. All other Bone data are
     * disregarded. In particular, the bones' {local/model}{Pos/Rot/Scale} and
     * userControl fields are ignored.
     */
    private Skeleton skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate bind pose for the specified Armature.
     *
     * @param armature (may be null, otherwise an alias is created)
     */
    public Pose(Armature armature) {
        this.armature = armature;
        this.skeleton = null;

        int jointCount;
        if (armature == null) {
            jointCount = 0;
        } else {
            jointCount = armature.getJointCount();
        }

        this.bindTransforms = new ArrayList<>(jointCount);
        this.transforms = new ArrayList<>(jointCount);

        if (armature != null) {
            Cloner cloner = new Cloner();
            /*
             * Clone the Armature, but not any of its target geometries
             * or attachment nodes.
             */
            for (Joint joint : armature.getJointList()) {
                Node attachment = MySkeleton.getAttachments(joint);
                if (attachment != null) {
                    cloner.setClonedValue(attachment, attachment);
                }
                Geometry target = MySkeleton.getTargetGeometry(joint);
                if (target != null) {
                    cloner.setClonedValue(target, target);
                }
            }
            Armature clonedArmature = cloner.clone(armature);
            clonedArmature.applyBindPose();

            for (int jointIndex = 0; jointIndex < jointCount; ++jointIndex) {
                Transform transform = new Transform();
                transforms.add(transform);

                Joint cloneJoint = clonedArmature.getJoint(jointIndex);
                Transform bindTransform
                        = cloneJoint.getLocalTransform(); // alias
                bindTransforms.add(bindTransform);
            }
        }

        assert bindTransforms.size() == jointCount;
        assert transforms.size() == jointCount;
    }

    /**
     * Instantiate bind pose for the specified Skeleton.
     *
     * @param skeleton (may be null, otherwise an alias is created)
     */
    public Pose(Skeleton skeleton) {
        this.armature = null;
        this.skeleton = skeleton;

        int boneCount;
        if (skeleton == null) {
            boneCount = 0;
        } else {
            boneCount = skeleton.getBoneCount();
        }

        this.bindTransforms = null;
        this.transforms = new ArrayList<>(boneCount);

        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Transform transform = new Transform();
            transforms.add(transform);
        }

        assert transforms.size() == boneCount;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Apply this Pose to the specified Armature.
     *
     * @param targetArmature the Armature to modify (not null)
     */
    public void applyTo(Armature targetArmature) {
        Validate.nonNull(targetArmature, "target armature");
        int numJoints = countBones();
        assert targetArmature.getJointCount() == numJoints : numJoints;

        // Copy local transforms from Pose to Armature.
        Transform tempTransform = new Transform();
        for (int jointIndex = 0; jointIndex < numJoints; ++jointIndex) {
            localTransform(jointIndex, tempTransform);
            Joint targetJoint = targetArmature.getJoint(jointIndex);
            targetJoint.setLocalTransform(tempTransform);
        }
    }

    /**
     * Apply this Pose to the specified Skeleton.
     *
     * @param targetSkeleton the Skeleton to modify (not null)
     */
    public void applyTo(Skeleton targetSkeleton) {
        Validate.nonNull(targetSkeleton, "target skeleton");
        int numBones = countBones();
        assert targetSkeleton.getBoneCount() == numBones : numBones;

        // Copy local transforms from Pose to Skeleton.
        Transform tempTransform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            localTransform(boneIndex, tempTransform);
            Bone targetBone = targetSkeleton.getBone(boneIndex);
            MySkeleton.setLocalTransform(targetBone, tempTransform);
        }
    }

    /**
     * Copy the local bind transform of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform bindTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        if (skeleton == null) { // new animation system
            Transform transform = bindTransforms.get(boneIndex); // alias
            result.set(transform);
        } else { // old animation system
            Bone bone = skeleton.getBone(boneIndex);
            MySkeleton.copyBindTransform(bone, result);
        }

        return result;
    }

    /**
     * Convert this Pose to an Animation. The resulting Animation will have zero
     * duration, a single keyframe at t=0, and all its tracks will be
     * BoneTracks.
     *
     * @param animationName name for the new Animation (not null)
     * @return a new instance
     */
    public Animation capture(String animationName) {
        Validate.nonNull(animationName, "animation name");

        // Start with an empty animation.
        float duration = 0f;
        Animation result = new Animation(animationName, duration);

        // Add a BoneTrack for each bone that's not in bind pose.
        int numBones = countBones();
        Transform transform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            userTransform(boneIndex, transform);
            if (!MyMath.isIdentity(transform)) {
                Vector3f translation = transform.getTranslation(); // alias
                Quaternion rotation = transform.getRotation(); // alias
                Vector3f scale = transform.getScale(); // alias
                BoneTrack track = MyAnimation
                        .newBoneTrack(boneIndex, translation, rotation, scale);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Convert this Pose to a new AnimClip. The result will have zero duration,
     * a single keyframe at t=0, and all its tracks will be TransformTracks.
     *
     * @param clipName name for the new clip (not null)
     * @return a new instance
     */
    public AnimClip captureToClip(String clipName) {
        Validate.nonNull(clipName, "clip name");

        // Start with an empty clip.
        AnimClip result = new AnimClip(clipName);
        /*
         * Add a TransformTrack for each Joint
         * with a non-identity local transform.
         */
        int numJoints = countBones();
        List<AnimTrack<?>> trackList = new ArrayList<>(numJoints);
        for (int jointIndex = 0; jointIndex < numJoints; ++jointIndex) {
            Transform transform = localTransform(jointIndex, null);
            if (!MyMath.isIdentity(transform)) {
                HasLocalTransform target = armature.getJoint(jointIndex);
                float[] times = {0f};
                Vector3f[] translations = {transform.getTranslation()};
                Quaternion[] rotations = {transform.getRotation()};
                Vector3f[] scales = {transform.getScale()};
                TransformTrack track = new TransformTrack(
                        target, times, translations, rotations, scales);
                trackList.add(track);
            }
        }

        int numTracks = trackList.size();
        AnimTrack<?>[] trackArray = new AnimTrack[numTracks];
        trackList.toArray(trackArray);
        result.setTracks(trackArray);

        return result;
    }

    /**
     * Count the joints/bones in this Pose.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        int count = transforms.size();
        return count;
    }

    /**
     * Find the index of the named joint/bone in this Pose.
     *
     * @param boneName which joint/bone (not null)
     * @return the joint/bone index (&ge;0) or -1 if not found
     */
    public int findBone(String boneName) {
        int result;

        if (skeleton == null) { // new animation system
            result = armature.getJointIndex(boneName);
        } else { // old animation system
            result = skeleton.getBoneIndex(boneName);
        }
        return result;
    }

    /**
     * Access the Armature or Skeleton, if known.
     *
     * @return the pre-existing instance, or null
     */
    public Object findSkeleton() {
        if (skeleton == null) { // new animation system
            return armature;
        } else { // old animation system
            return skeleton;
        }
    }

    /**
     * Calculate the local rotation of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the rotation in local coordinates (either storeResult or a new
     * instance)
     */
    public Quaternion localRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        // Start with the local bind rotation.
        Quaternion bindRotation;
        if (skeleton == null) { // new animation system
            Transform bindTransform = bindTransforms.get(boneIndex); // alias
            bindRotation = bindTransform.getRotation(); // alias
        } else { // old animation system
            Bone bone = skeleton.getBone(boneIndex);
            bindRotation = bone.getBindRotation(); // alias
        }

        // Apply its user/animation rotation.
        Transform userTransform = transforms.get(boneIndex); // alias
        Quaternion userRotation = userTransform.getRotation(); // alias
        Quaternion result = bindRotation.mult(userRotation, storeResult);

        return result;
    }

    /**
     * Calculate the local transform of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the Transform in local coordinates (either storeResult or a new
     * instance)
     */
    public Transform localTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        // Start with the local bind transform.
        Transform result = bindTransform(boneIndex, storeResult);
        /*
         * Apply the user/animation transform in a simple (yet peculiar) way
         * to obtain the local transform.
         */
        Transform user = userTransform(boneIndex, null);
        result.getTranslation().addLocal(user.getTranslation());
        result.getRotation().multLocal(user.getRotation());
        result.getScale().multLocal(user.getScale());

        return result;
    }

    /**
     * Calculate the location of the indexed bone in the coordinate system of an
     * animated spatial.
     *
     * @param boneIndex which bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return location in model space (either storeResult or a new instance)
     */
    public Vector3f modelLocation(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform modelTransform
                = modelTransform(boneIndex, null); // TODO garbage
        Vector3f result = modelTransform.getTranslation(storeResult);

        return result;
    }

    /**
     * Calculate the orientation of the indexed joint/bone in the coordinate
     * system of an animated spatial.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return orientation in model space (either storeResult or a new instance)
     */
    public Quaternion modelOrientation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        boolean isRoot;
        int parentIndex;
        if (skeleton == null) { // new animation system
            Joint joint = armature.getJoint(boneIndex);
            Joint parentJoint = joint.getParent();
            isRoot = (parentJoint == null);
            parentIndex = armature.getJointIndex(parentJoint);
        } else { // old animation system
            Bone bone = skeleton.getBone(boneIndex);
            Bone parentBone = bone.getParent();
            isRoot = (parentBone == null);
            parentIndex = skeleton.getBoneIndex(parentBone);
        }

        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;
        if (isRoot) { // For a root joint/bone, use the local rotation.
            localRotation(boneIndex, result);
        } else {
            /*
             * For a non-root joint/bone, use the parent's model orientation
             * times the local rotation.
             */
            modelOrientation(parentIndex, result);
            Quaternion localRotation
                    = localRotation(boneIndex, null); // TODO garbage
            result.multLocal(localRotation);
        }

        return result;
    }

    /**
     * Calculate the model transform of the indexed joint/bone. When applied as
     * a left factor, the model transform converts from the joint/bone local
     * coordinate system to the coordinate system of an animated spatial.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform modelTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        // Start with the joint/bone local transform.
        Transform result = localTransform(boneIndex, storeResult);

        Transform parent = null;
        if (skeleton == null) {
            Joint bone = armature.getJoint(boneIndex);
            Joint parentJoint = bone.getParent();
            if (parentJoint != null) {
                int parentIndex = parentJoint.getId();
                parent = modelTransform(parentIndex, null);
            }

        } else {
            Bone bone = skeleton.getBone(boneIndex);
            Bone parentBone = bone.getParent();
            if (parentBone != null) {
                int parentIndex = skeleton.getBoneIndex(parentBone);
                parent = modelTransform(parentIndex, null);
            }
        }

        if (parent != null) {
            Transform local = result.clone();
            /*
             * Apply the parent's model transform in a very peculiar way
             * to obtain the bone's model transform.
             */
            Vector3f mTranslation = result.getTranslation(); // alias
            Quaternion mRotation = result.getRotation(); // alias
            Vector3f mScale = result.getScale(); // alias
            parent.getRotation().mult(local.getRotation(), mRotation);
            parent.getScale().mult(local.getScale(), mScale);
            MyQuaternion.rotate(
                    parent.getRotation(), local.getTranslation(), mTranslation);
            mTranslation.multLocal(parent.getScale());
            mTranslation.addLocal(parent.getTranslation());
        }

        return result;
    }

    /**
     * Instantiate bind pose for the specified SkeletonControl or
     * SkinningControl.
     *
     * @param sControl the Control to use (not null, must be a SkeletonControl
     * or a SkinningControl)
     * @return a new instance
     */
    public static Pose newInstance(AbstractControl sControl) {
        Pose result;
        if (sControl instanceof SkeletonControl) {
            Skeleton skeleton = ((SkeletonControl) sControl).getSkeleton();
            result = new Pose(skeleton);
        } else {
            Armature armature = ((SkinningControl) sControl).getArmature();
            result = new Pose(armature);
        }

        return result;
    }

    /**
     * Enumerate all bones/joints in a pre-order depth-first traversal of the
     * skeleton/armature, such that child bones/joints are never visited before
     * their ancestors.
     *
     * @return a new array of joint/bone indices
     */
    public int[] preOrderIndices() {
        int boneCount = transforms.size();
        int[] result = new int[boneCount];

        if (armature != null) { // new animation system
            List<Joint> list = MySkeleton.preOrderJoints(armature);
            assert list.size() == boneCount : boneCount;
            for (int index = 0; index < boneCount; ++index) {
                Joint joint = list.get(index);
                result[index] = joint.getId();
            }

        } else if (skeleton != null) { // old animation system
            List<Bone> list = MySkeleton.preOrderBones(skeleton);
            assert list.size() == boneCount : boneCount;
            for (int index = 0; index < boneCount; ++index) {
                Bone bone = list.get(index);
                result[index] = skeleton.getBoneIndex(bone);
            }
        }

        return result;
    }

    /**
     * Reset the user/animation rotation of the indexed joint/bone to identity.
     *
     * @param boneIndex which joint/bone (&ge;0)
     */
    public void resetRotation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Quaternion rotation = transform.getRotation(); // alias
        rotation.loadIdentity();
    }

    /**
     * Reset the user/animation scale of the indexed joint/bone to identity.
     *
     * @param boneIndex which joint/bone (&ge;0)
     */
    public void resetScale(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Vector3f scale = transform.getScale(); // alias
        scale.set(1f, 1f, 1f);
    }

    /**
     * Reset the user/animation translation of the indexed joint/bone to zero.
     *
     * @param boneIndex which joint/bone (&ge;0)
     */
    public void resetTranslation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Vector3f translation = transform.getTranslation(); // alias
        translation.zero();
    }

    /**
     * Enumerate all root bones/joints in the skeleton/armature.
     *
     * @return a new array of indices
     */
    public int[] rootBoneIndices() {
        int numRootBones = 0;
        if (armature != null) { // new animation system
            numRootBones = MySkeleton.countRootJoints(armature);
        } else if (skeleton != null) { // old animation system
            numRootBones = MySkeleton.countRootBones(skeleton);
        }
        int[] result = new int[numRootBones];

        if (armature != null) { // new animation system
            Joint[] roots = armature.getRoots();
            for (int rootIndex = 0; rootIndex < numRootBones; ++rootIndex) {
                Joint root = roots[rootIndex];
                result[rootIndex] = root.getId();
            }

        } else if (skeleton != null) { // old animation system
            Bone[] roots = skeleton.getRoots();
            for (int rootIndex = 0; rootIndex < numRootBones; ++rootIndex) {
                Bone root = roots[rootIndex];
                int boneIndex = skeleton.getBoneIndex(root);
                assert boneIndex >= 0 : boneIndex;
                result[rootIndex] = boneIndex;
            }
        }

        return result;
    }

    /**
     * Alter the user/animation transform of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone to transform (&ge;0)
     * @param transform the desired Transform (not null, unaffected)
     */
    public void set(int boneIndex, Transform transform) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(transform, "transform");

        Transform boneTransform = transforms.get(boneIndex); // alias
        boneTransform.set(transform);
    }

    /**
     * Alter the user/animation rotation of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone to rotate (&ge;0)
     * @param rotation the desired rotation (not null, unaffected)
     */
    public void setRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = transforms.get(boneIndex); // alias
        boneTransform.setRotation(rotation);
    }

    /**
     * Alter the user/animation scale of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone to scale (&ge;0)
     * @param scale the desired scale factor for each axis (not null,
     * unaffected)
     */
    public void setScale(int boneIndex, Vector3f scale) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Transform boneTransform = transforms.get(boneIndex); // alias
        boneTransform.setScale(scale);
    }

    /**
     * Configure this Pose for the specified Animation at the specified time.
     *
     * @param animation which Animation (not null, unaffected)
     * @param time the animation time (in seconds)
     * @param techniques the tweening techniques to use (not null, unaffected)
     */
    public void setToAnimation(
            Animation animation, float time, TweenTransforms techniques) {
        Validate.nonNull(animation, "animation");
        Validate.nonNull(techniques, "techniques");

        int numBones = transforms.size();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Transform transform = transforms.get(boneIndex); // alias
            BoneTrack track = MyAnimation.findBoneTrack(animation, boneIndex);
            if (track == null) {
                transform.loadIdentity();
            } else {
                float duration = animation.getLength();
                techniques.transform(track, time, duration, null, transform);
            }
        }
    }

    /**
     * Configure this Pose to represent bind pose.
     */
    public void setToBind() {
        for (Transform transform : transforms) {
            transform.loadIdentity();
        }
    }

    /**
     * Configure this Pose for the specified AnimClip at the specified time.
     *
     * @param clip which AnimClip (not null, unaffected)
     * @param time the animation time (in seconds)
     */
    public void setToClip(AnimClip clip, double time) {
        Validate.nonNull(clip, "animation");

        int numJoints = transforms.size();
        for (int jointIndex = 0; jointIndex < numJoints; ++jointIndex) {
            Transform transform = transforms.get(jointIndex); // alias
            TransformTrack track
                    = MyAnimation.findTransformTrack(clip, jointIndex);
            if (track == null) {
                transform.loadIdentity();
            } else {
                Transform bt = bindTransforms.get(jointIndex); // alias
                transform.set(bt);
                track.getDataAtTime(time, transform);
                userForLocal(jointIndex, transform, transform);
            }
        }
    }

    /**
     * Configure this Pose by re-targeting the specified source pose.
     *
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     */
    public void setToRetarget(Pose sourcePose, SkeletonMapping map) {
        Validate.nonNull(sourcePose, "source pose");
        Validate.nonNull(map, "map");

        if (skeleton == null) { // new animation system
            Joint[] rootJoints = armature.getRoots();
            for (Joint rootJoint : rootJoints) {
                retargetJoints(rootJoint, sourcePose, map);
            }

        } else { // old animation system
            Bone[] rootBones = skeleton.getRoots();
            for (Bone rootBone : rootBones) {
                retargetBones(rootBone, sourcePose, map);
            }
        }
    }

    /**
     * Alter the user/animation translation of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone to translate (&ge;0)
     * @param translation the desired translation (not null, unaffected)
     */
    public void setTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(translation, "translation");

        Transform boneTransform = transforms.get(boneIndex); // alias
        boneTransform.setTranslation(translation);
    }

    /**
     * Calculate skinning matrices for this Pose.
     *
     * @param storeResult storage for the results (modified if not null)
     * @return skinning matrices (either storeResult or a new instance)
     */
    public Matrix4f[] skin(Matrix4f[] storeResult) {
        int numBones = transforms.size();
        Matrix4f[] result;
        if (storeResult == null) {
            result = new Matrix4f[numBones];
        } else {
            result = storeResult;
            assert result.length >= numBones : numBones;
        }

        // Allocate temporary storage.
        Vector3f skTranslation = new Vector3f();
        Quaternion skRotation = new Quaternion();
        Matrix3f skRotMatrix = new Matrix3f();
        Vector3f skScale = new Vector3f();
        Transform msTransform = new Transform();

        Vector3f msTranslation = msTransform.getTranslation(); // alias
        Quaternion msRotation = msTransform.getRotation(); // alias
        Vector3f msScale = msTransform.getScale(); // alias

        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            modelTransform(boneIndex, msTransform);
            Matrix4f matrix4f = result[boneIndex];
            if (matrix4f == null) {
                matrix4f = new Matrix4f();
                result[boneIndex] = matrix4f;
            }
            /*
             * Calculate the skinning transform for the joint/bone.
             * Compare with Bone.getOffsetTransform() and
             * SeparateJointModelTransform.getOffsetTransform().
             *
             * TODO handle MatrixJointModelTransform differently?
             */
            if (skeleton == null) { // new animation system
                msTransform.toTransformMatrix(matrix4f);
                Joint joint = armature.getJoint(boneIndex);
                Matrix4f mbi = joint.getInverseModelBindMatrix();
                matrix4f.mult(mbi, matrix4f);

            } else { // old animation system
                Bone bone = skeleton.getBone(boneIndex);
                Vector3f mbiTranslation = bone.getModelBindInversePosition();
                Quaternion mbiRotation = bone.getModelBindInverseRotation();
                Vector3f mbiScale = bone.getModelBindInverseScale();
                msScale.mult(mbiScale, skScale);

                msRotation.mult(mbiRotation, skRotation);
                skRotation.toRotationMatrix(skRotMatrix);

                skScale.mult(mbiTranslation, skTranslation);
                MyQuaternion.rotate(skRotation, skTranslation, skTranslation);
                skTranslation.addLocal(msTranslation);

                matrix4f.setTransform(skTranslation, skScale, skRotMatrix);
            }
        }

        return result;
    }

    /**
     * Determine the user/animation Transform for the indexed joint/bone to give
     * it the specified local Transform.
     *
     * @param boneIndex the index of the subject bone/joint (&ge;0)
     * @param localTransform the desired local Transform (not null)
     * @param storeResult storage for the result (modified if not null, may be
     * localTransform)
     * @return the required user Transform (either storeResult or a new
     * instance)
     */
    public Transform userForLocal(
            int boneIndex, Transform localTransform, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(localTransform, "local transform");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        result.set(localTransform);
        Transform bind = bindTransform(boneIndex, null); // TODO garbage
        result.getTranslation().subtractLocal(bind.getTranslation());
        Quaternion userRotation = result.getRotation(); // alias
        bind.getRotation().inverseLocal().mult(userRotation, userRotation);
        result.getScale().divideLocal(bind.getScale());

        return result;
    }

    /**
     * Calculate the user/animation rotation for the indexed joint/bone to give
     * it the specified orientation in the coordinate system of an animated
     * spatial.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param modelOrientation desired orientation (not null, not zero,
     * unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Quaternion userForModel(int boneIndex, Quaternion modelOrientation,
            Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonZero(modelOrientation, "model orientation");

        Quaternion bind;
        Quaternion local;
        if (skeleton == null) { // new animation system
            bind = bindTransforms.get(boneIndex).getRotation(); // alias
            Joint joint = armature.getJoint(boneIndex);
            local = localForModel(joint, modelOrientation, null);
        } else { // old animation system
            Bone bone = skeleton.getBone(boneIndex);
            bind = bone.getBindRotation(); // alias
            local = localForModel(bone, modelOrientation, null);
        }
        Quaternion inverseBind = bind.inverse();
        Quaternion result = inverseBind.mult(local, storeResult);

        return result;
    }

    /**
     * Copy the user/animation rotation of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the user rotation (either storeResult or a new instance)
     */
    public Quaternion userRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Quaternion result = transform.getRotation(storeResult);

        return result;
    }

    /**
     * Copy the user/animation scale of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the user scale factor for each axis (either storeResult or a new
     * instance)
     */
    public Vector3f userScale(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Vector3f result = transform.getScale(storeResult);

        return result;
    }

    /**
     * Copy the user/animation transform of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform userTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        if (storeResult == null) {
            return transform.clone();
        } else {
            return storeResult.set(transform);
        }
    }

    /**
     * Copy the user/animation translation of the indexed joint/bone.
     *
     * @param boneIndex which joint/bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return user translation (either storeResult or a new instance)
     */
    public Vector3f userTranslation(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex); // alias
        Vector3f result = transform.getTranslation(storeResult);

        return result;
    }
    // *************************************************************************
    // JmeCloneable methods

    /**
     * Convert this shallow-cloned instance into a deep-cloned one, using the
     * specified cloner and original to resolve copied fields.
     *
     * @param cloner the cloner currently cloning this control (not null)
     * @param original the control from which this control was shallow-cloned
     * (not null)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.armature = cloner.clone(armature);
        this.skeleton = cloner.clone(skeleton);

        int numTransforms = transforms.size();
        List<Transform> originalTransforms = transforms;
        this.transforms = new ArrayList<>(numTransforms);
        for (Transform t : originalTransforms) {
            Transform tClone = t.clone();
            transforms.add(tClone);
        }

        if (bindTransforms != null) {
            originalTransforms = bindTransforms;
            this.bindTransforms = new ArrayList<>(numTransforms);
            for (Transform t : originalTransforms) {
                Transform tClone = t.clone();
                bindTransforms.add(tClone);
            }
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public Pose jmeClone() {
        try {
            Pose clone = (Pose) clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the local rotation for the specified Bone to give it the
     * specified orientation in the coordinate system of an animated spatial.
     *
     * @param bone which Bone (not null, unaffected)
     * @param modelOrientation the desired orientation (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return rotation (either storeResult or a new instance)
     */
    private Quaternion localForModel(
            Bone bone, Quaternion modelOrientation, Quaternion storeResult) {
        assert bone != null;
        assert modelOrientation != null;
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        Bone parent = bone.getParent();
        if (parent == null) {
            result.set(modelOrientation);
        } else { // Factor in the orientation of the parent bone.
            int parentIndex = skeleton.getBoneIndex(parent);
            Quaternion parentMo = modelOrientation(parentIndex, null);
            Quaternion parentImo = parentMo.inverse();
            parentImo.mult(modelOrientation, result);
        }

        return result;
    }

    /**
     * Calculate the local rotation for the specified Joint to give it the
     * specified orientation in the coordinate system of an animated spatial.
     *
     * @param joint which Joint (not null, unaffected)
     * @param modelOrientation the desired orientation (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return rotation (either storeResult or a new instance)
     */
    private Quaternion localForModel(
            Joint joint, Quaternion modelOrientation, Quaternion storeResult) {
        assert joint != null;
        assert modelOrientation != null;
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        Joint parent = joint.getParent();
        if (parent == null) {
            result.set(modelOrientation);
        } else { // Factor in the orientation of the parent joint.
            int parentIndex = parent.getId();
            Quaternion parentMo = modelOrientation(parentIndex, null);
            Quaternion parentImo = parentMo.inverse();
            parentImo.mult(modelOrientation, result);
        }

        return result;
    }

    /**
     * Configure the specified Bone and its descendents by re-targeting the
     * specified source pose. Note: recursive!
     *
     * @param bone the Bone to start with (not null, unaffected)
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     */
    private void retargetBones(
            Bone bone, Pose sourcePose, SkeletonMapping map) {
        assert bone != null;
        assert sourcePose != null;
        assert map != null;

        int targetIndex = skeleton.getBoneIndex(bone);
        Transform userTransform = transforms.get(targetIndex); // alias
        userTransform.loadIdentity();

        String targetName = bone.getName();
        BoneMapping boneMapping = map.get(targetName); // alias
        if (boneMapping != null) {
            // Calculate the orientation of the source bone in model space.
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourcePose.findBone(sourceName);
            Quaternion mo = sourcePose.modelOrientation(sourceIndex, null);

            // Set the orientation of the target bone in user space:
            Quaternion userRotation = userForModel(targetIndex, mo, null);
            Quaternion twist = boneMapping.getTwist(); // alias
            Quaternion rot = userTransform.getRotation(); // alias
            userRotation.mult(twist, rot);
            MyQuaternion.normalizeLocal(rot);
        }

        List<Bone> children = bone.getChildren();
        for (Bone childBone : children) {
            retargetBones(childBone, sourcePose, map);
        }
    }

    /**
     * Configure the specified Armature and its descendents by re-targeting the
     * specified source pose. Note: recursive!
     *
     * @param joint the Joint to start with (not null, unaffected)
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     */
    private void retargetJoints(
            Joint joint, Pose sourcePose, SkeletonMapping map) {
        assert joint != null;
        assert sourcePose != null;
        assert map != null;

        int targetIndex = joint.getId();
        Transform userTransform = transforms.get(targetIndex); // alias
        userTransform.loadIdentity();

        String targetName = joint.getName();
        BoneMapping boneMapping = map.get(targetName); // alias
        if (boneMapping != null) {
            // Calculate the orientation of the source joint in model space.
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourcePose.findBone(sourceName);
            Quaternion mo = sourcePose.modelOrientation(sourceIndex, null);

            // Set the orientation of the target joint in user space:
            Quaternion userRotation = userForModel(targetIndex, mo, null);
            Quaternion twist = boneMapping.getTwist(); // alias
            Quaternion rot = userTransform.getRotation(); // alias
            userRotation.mult(twist, rot);
            MyQuaternion.normalizeLocal(rot);
        }

        List<Joint> children = joint.getChildren();
        for (Joint child : children) {
            retargetJoints(child, sourcePose, map);
        }
    }
}
