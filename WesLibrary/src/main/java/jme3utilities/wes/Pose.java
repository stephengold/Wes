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
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
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

/**
 * Encapsulate a pose for a particular Skeleton.
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
     * user/animation transforms that describe this Pose, one for each skeleton
     * bone
     */
    private List<Transform> transforms;
    /**
     * Skeleton on which this Pose is based, or null for none
     * <p>
     * This Skeleton provides the name, index, parent, children, and bind
     * transform of each Bone. All other Bone data are disregarded. In
     * particular, the bones' {local/model}{Pos/Rot/Scale} and userControl
     * fields are ignored.
     */
    private Skeleton skeleton;
    // *************************************************************************
    // constructors

    /**
     * Instantiate bind pose for the specified Skeleton.
     *
     * @param skeleton (may be null, otherwise an alias is created)
     */
    public Pose(Skeleton skeleton) {
        this.skeleton = skeleton;

        int boneCount;
        if (skeleton == null) {
            boneCount = 0;
        } else {
            boneCount = skeleton.getBoneCount();
        }

        transforms = new ArrayList<>(boneCount);

        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Transform transform = new Transform();
            transforms.add(transform);
        }

        assert transforms.size() == boneCount;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Apply this Pose to the specified Skeleton. TODO rename applyTo()
     *
     * @param targetSkeleton the Skeleton to modify (not null)
     */
    public void apply(Skeleton targetSkeleton) {
        Validate.nonNull(targetSkeleton, "target skeleton");
        int numBones = countBones();
        assert targetSkeleton.getBoneCount() == numBones : numBones;
        /*
         * Copy local transforms from Pose to Skeleton.
         */
        Transform tempTransform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            localTransform(boneIndex, tempTransform);
            Bone targetBone = targetSkeleton.getBone(boneIndex);
            MySkeleton.setLocalTransform(targetBone, tempTransform);
        }
    }

    /**
     * Copy the local bind transform of the indexed Bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform bindTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;

        Bone bone = skeleton.getBone(boneIndex);
        MySkeleton.copyBindTransform(bone, result);

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
        /*
         * Start with an empty animation.
         */
        float duration = 0f;
        Animation result = new Animation(animationName, duration);
        /*
         * Add a BoneTrack for each bone that's not in bind pose.
         */
        int numBones = countBones();
        Transform transform = new Transform();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            userTransform(boneIndex, transform);
            if (!MyMath.isIdentity(transform)) {
                Vector3f translation = transform.getTranslation();
                Quaternion rotation = transform.getRotation();
                Vector3f scale = transform.getScale();
                BoneTrack track = MyAnimation.newBoneTrack(boneIndex,
                        translation, rotation, scale);
                result.addTrack(track);
            }
        }

        return result;
    }

    /**
     * Count the bones in this Pose.
     *
     * @return count (&ge;0)
     */
    public int countBones() {
        int count = transforms.size();
        assert count >= 0 : count;
        return count;
    }

    /**
     * Find the index of the named Bone in this Pose.
     *
     * @param boneName which Bone (not null)
     * @return the bone index (&ge;0) or -1 if not found
     */
    public int findBone(String boneName) {
        int result = skeleton.getBoneIndex(boneName);
        return result;
    }

    /**
     * Calculate the local rotation of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the rotation in local coordinates (either storeResult or a new
     * instance)
     */
    public Quaternion localRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the local bind rotation.
         */
        Bone bone = skeleton.getBone(boneIndex);
        Quaternion bindRotation = bone.getBindRotation(); // alias
        /*
         * Apply its user/animation rotation.
         */
        Transform userTransform = transforms.get(boneIndex);
        Quaternion userRotation = userTransform.getRotation();
        storeResult = bindRotation.mult(userRotation, storeResult);

        return storeResult;
    }

    /**
     * Calculate the local transform of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the Transform in local coordinates (either storeResult or a new
     * instance)
     */
    public Transform localTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the local bind transform.
         */
        storeResult = bindTransform(boneIndex, storeResult);
        /*
         * Apply the user/animation transform in a simple (yet peculiar) way
         * to obtain the local transform.
         */
        Transform user = userTransform(boneIndex, null);
        storeResult.getTranslation().addLocal(user.getTranslation());
        storeResult.getRotation().multLocal(user.getRotation());
        storeResult.getScale().multLocal(user.getScale());

        return storeResult;
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

        Transform modelTransform = modelTransform(boneIndex, null);
        Vector3f modelLocation = modelTransform.getTranslation();
        if (storeResult == null) {
            storeResult = modelLocation;
        } else {
            storeResult.set(modelLocation);
        }

        return storeResult;
    }

    /**
     * Calculate the orientation of the indexed Bone in the coordinate system of
     * an animated spatial.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return orientation in model space (either storeResult or a new instance)
     */
    public Quaternion modelOrientation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Bone bone = skeleton.getBone(boneIndex);
        Bone parentBone = bone.getParent();
        if (parentBone == null) {
            /*
             * For a root bone, use the local rotation.
             */
            storeResult = localRotation(boneIndex, storeResult);
        } else {
            int parentIndex = skeleton.getBoneIndex(parentBone);
            /*
             * For a non-root bone, use the parent's model orientation
             * times the local rotation.
             */
            storeResult = modelOrientation(parentIndex, storeResult);
            Quaternion localRotation = localRotation(boneIndex, null);
            storeResult.multLocal(localRotation);
        }

        return storeResult;
    }

    /**
     * Calculate the model transform of the indexed Bone. When applied as a left
     * factor, the model transform converts from the bone's coordinate system to
     * the coordinate system of an animated spatial.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform modelTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        /*
         * Start with the bone's local transform.
         */
        storeResult = localTransform(boneIndex, storeResult);

        Bone bone = skeleton.getBone(boneIndex);
        Bone parentBone = bone.getParent();
        if (parentBone != null) {
            Transform local = storeResult.clone();

            int parentIndex = skeleton.getBoneIndex(parentBone);
            Transform parent = modelTransform(parentIndex, null);
            /*
             * Apply the parent's model transform in a very peculiar way
             * to obtain the bone's model transform.
             */
            Vector3f mTranslation = storeResult.getTranslation();
            Quaternion mRotation = storeResult.getRotation();
            Vector3f mScale = storeResult.getScale();
            parent.getRotation().mult(local.getRotation(), mRotation);
            parent.getScale().mult(local.getScale(), mScale);
            parent.getRotation().mult(local.getTranslation(), mTranslation);
            mTranslation.multLocal(parent.getScale());
            mTranslation.addLocal(parent.getTranslation());
        }

        return storeResult;
    }

    /**
     * Enumerate all bones in a pre-order depth-first traversal of the Skeleton,
     * such that child bones are never visited before their ancestors.
     *
     * @return a new array of bone indices
     */
    public int[] preOrderIndices() {
        int boneCount = transforms.size();
        int[] result = new int[boneCount];

        if (skeleton != null) {
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
     * Reset the user/animation rotation of the indexed Bone to identity.
     *
     * @param boneIndex which Bone (&ge;0)
     */
    public void resetRotation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Quaternion rotation = transform.getRotation();
        rotation.loadIdentity();
    }

    /**
     * Reset the user/animation scale of the indexed Bone to identity.
     *
     * @param boneIndex which Bone (&ge;0)
     */
    public void resetScale(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Vector3f scale = transform.getScale();
        scale.set(1f, 1f, 1f);
    }

    /**
     * Reset the user/animation translation of the indexed Bone to zero.
     *
     * @param boneIndex which Bone (&ge;0)
     */
    public void resetTranslation(int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        Vector3f translation = transform.getTranslation();
        translation.zero();
    }

    /**
     * Enumerate all root bones in the skeleton.
     *
     * @return a new array of indices
     */
    public int[] rootBoneIndices() {
        int numRootBones = 0;
        Bone[] roots = null;
        if (skeleton != null) {
            roots = skeleton.getRoots();
            numRootBones = roots.length;
        }
        int[] result = new int[numRootBones];

        for (int rootIndex = 0; rootIndex < numRootBones; rootIndex++) {
            Bone root = roots[rootIndex];
            int boneIndex = skeleton.getBoneIndex(root);
            assert boneIndex >= 0 : boneIndex;
            result[rootIndex] = boneIndex;
        }

        return result;
    }

    /**
     * Alter the user/animation transform of the indexed Bone.
     *
     * @param boneIndex which Bone to transform (&ge;0)
     * @param transform the desired Transform (not null, unaffected)
     */
    public void set(int boneIndex, Transform transform) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(transform, "transform");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.set(transform);
    }

    /**
     * Alter the user/animation rotation of the indexed Bone.
     *
     * @param boneIndex which Bone to rotate (&ge;0)
     * @param rotation the desired rotation (not null, unaffected)
     */
    public void setRotation(int boneIndex, Quaternion rotation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(rotation, "rotation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setRotation(rotation);
    }

    /**
     * Alter the user/animation scale of the indexed Bone.
     *
     * @param boneIndex which Bone to scale (&ge;0)
     * @param scale the desired scale factor for each axis (not null,
     * unaffected)
     */
    public void setScale(int boneIndex, Vector3f scale) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(scale, "scale");
        Validate.positive(scale.x, "x scale");
        Validate.positive(scale.y, "y scale");
        Validate.positive(scale.z, "z scale");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setScale(scale);
    }

    /**
     * Alter the user/animation translation of the indexed bone. TODO re-order
     * methods
     *
     * @param boneIndex which bone to translate (&ge;0)
     * @param translation (not null, unaffected)
     */
    public void setTranslation(int boneIndex, Vector3f translation) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(translation, "translation");

        Transform boneTransform = transforms.get(boneIndex);
        boneTransform.setTranslation(translation);
    }

    /**
     * Configure this Pose for the specified Animation at the specified time.
     *
     * @param animation which Animation (not null, unaffected)
     * @param time the animation time (in seconds)
     * @param techniques the tweening techniques to use (not null, unaffected)
     */
    public void setToAnimation(Animation animation, float time,
            TweenTransforms techniques) {
        Validate.nonNull(animation, "animation");
        Validate.nonNull(techniques, "techniques");

        int numBones = transforms.size();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Transform transform = transforms.get(boneIndex);
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
        int count = transforms.size();
        for (int boneIndex = 0; boneIndex < count; ++boneIndex) {
            Transform transform = transforms.get(boneIndex);
            transform.loadIdentity();
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

        Bone[] rootBones = skeleton.getRoots();
        for (Bone rootBone : rootBones) {
            retargetBones(rootBone, sourcePose, map);
        }
    }

    /**
     * Calculate skinning matrices for this Pose.
     *
     * @param storeResult (modified if not null)
     * @return skinning matrices (either storeResult or a new instance)
     */
    public Matrix4f[] skin(Matrix4f[] storeResult) {
        int numBones = transforms.size();
        if (storeResult == null) {
            storeResult = new Matrix4f[numBones];
        } else {
            assert storeResult.length >= numBones : numBones;
        }
        /*
         * Allocate temporary storage.
         */
        Vector3f skTranslation = new Vector3f();
        Quaternion skRotation = new Quaternion();
        Matrix3f skRotMatrix = new Matrix3f();
        Vector3f skScale = new Vector3f();
        Transform msTransform = new Transform();

        Vector3f msTranslation = msTransform.getTranslation();
        Quaternion msRotation = msTransform.getRotation();
        Vector3f msScale = msTransform.getScale();

        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex);
            modelTransform(boneIndex, msTransform);
            /*
             * Calculate the skinning transform for the Bone.
             * Compare with Bone.getOffsetTransform()
             */
            Vector3f mbiScale = bone.getModelBindInverseScale();
            msScale.mult(mbiScale, skScale);

            Quaternion mbiRotation = bone.getModelBindInverseRotation();
            msRotation.mult(mbiRotation, skRotation);
            skRotation.toRotationMatrix(skRotMatrix);

            Vector3f mbiTranslation = bone.getModelBindInversePosition();
            skScale.mult(mbiTranslation, skTranslation);
            skRotation.mult(skTranslation, skTranslation);
            skTranslation.addLocal(msTranslation);

            Matrix4f matrix4f = storeResult[boneIndex];
            if (matrix4f == null) {
                matrix4f = new Matrix4f();
                storeResult[boneIndex] = matrix4f;
            }
            matrix4f.setTransform(skTranslation, skScale, skRotMatrix);
        }

        return storeResult;
    }

    /**
     * Calculate the user/animation rotation for the indexed Bone to give it the
     * specified orientation in the coordinate system of an animated spatial.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param modelOrientation desired orientation (not null, not zero,
     * unaffected)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Quaternion userForModel(int boneIndex, Quaternion modelOrientation,
            Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonZero(modelOrientation, "model orienation");

        Bone bone = skeleton.getBone(boneIndex);
        Quaternion bind = bone.getBindRotation();
        Quaternion local = localForModel(bone, modelOrientation, null);
        Quaternion inverseBind = bind.inverse();
        storeResult = inverseBind.mult(local, storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation rotation of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the user rotation (either storeResult or a new instance)
     */
    public Quaternion userRotation(int boneIndex, Quaternion storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getRotation(storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation scale of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return the user scale factor for each axis (either storeResult or a new
     * instance)
     */
    public Vector3f userScale(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getScale(storeResult);

        return storeResult;
    }

    /**
     * Copy the user/animation transform of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform userTransform(int boneIndex, Transform storeResult) {
        Validate.nonNegative(boneIndex, "bone index");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Transform transform = transforms.get(boneIndex);
        storeResult.set(transform);

        return storeResult;
    }

    /**
     * Copy the user/animation translation of the indexed Bone.
     *
     * @param boneIndex which Bone (&ge;0)
     * @param storeResult storage for the result (modified if not null)
     * @return user translation (either storeResult or a new instance)
     */
    public Vector3f userTranslation(int boneIndex, Vector3f storeResult) {
        Validate.nonNegative(boneIndex, "bone index");

        Transform transform = transforms.get(boneIndex);
        storeResult = transform.getTranslation(storeResult);

        return storeResult;
    }
    // *************************************************************************
    // Cloneable methods

    /**
     * Create a deep copy of this Pose.
     *
     * @return a new Pose, equivalent to this one
     */
    @Override
    public Pose clone() {
        Pose clone;
        try {
            clone = (Pose) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }

        int numTransforms = transforms.size();
        List<Transform> originalTransforms = transforms;
        transforms = new ArrayList<>(numTransforms);
        for (Transform t : originalTransforms) {
            Transform tClone = t.clone();
            transforms.add(tClone);
        }

        return clone;
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
        skeleton = cloner.clone(skeleton);

        int numTransforms = transforms.size();
        List<Transform> originalTransforms = transforms;
        transforms = new ArrayList<>(numTransforms);
        for (Transform t : originalTransforms) {
            Transform tClone = t.clone();
            transforms.add(tClone);
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
            Pose clone = (Pose) super.clone();
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
    private Quaternion localForModel(Bone bone, Quaternion modelOrientation,
            Quaternion storeResult) {
        assert bone != null;
        assert modelOrientation != null;
        if (storeResult == null) {
            storeResult = new Quaternion();
        }

        Bone parent = bone.getParent();
        if (parent == null) {
            storeResult.set(modelOrientation);
        } else {
            /*
             * Factor in the orientation of the parent bone.
             */
            int parentIndex = skeleton.getBoneIndex(parent);
            Quaternion parentMo = modelOrientation(parentIndex, null);
            Quaternion parentImo = parentMo.inverse();
            parentImo.mult(modelOrientation, storeResult);
        }

        return storeResult;
    }

    /**
     * Configure the specified Bone and its descendents by re-targeting the
     * specified source pose. Note: recursive!
     *
     * @param bone the Bone to start with (not null, unaffected)
     * @param sourcePose which source pose to re-target (not null, unaffected)
     * @param map the skeleton map to use (not null, unaffected)
     */
    private void retargetBones(Bone bone, Pose sourcePose,
            SkeletonMapping map) {
        assert bone != null;
        assert sourcePose != null;
        assert map != null;

        int targetIndex = skeleton.getBoneIndex(bone);
        Transform userTransform = transforms.get(targetIndex);
        userTransform.loadIdentity();

        String targetName = bone.getName();
        BoneMapping boneMapping = map.get(targetName);
        if (boneMapping != null) {
            /*
             * Calculate the orientation of the source bone in model space.
             */
            String sourceName = boneMapping.getSourceName();
            int sourceIndex = sourcePose.findBone(sourceName);
            Quaternion mo = sourcePose.modelOrientation(sourceIndex, null);

            Quaternion userRotation = userForModel(targetIndex, mo, null);
            Quaternion twist = boneMapping.getTwist();
            userRotation.mult(twist, userTransform.getRotation());
            userTransform.getRotation().normalizeLocal();
        }

        List<Bone> children = bone.getChildren();
        for (Bone childBone : children) {
            retargetBones(childBone, sourcePose, map);
        }
    }
}
