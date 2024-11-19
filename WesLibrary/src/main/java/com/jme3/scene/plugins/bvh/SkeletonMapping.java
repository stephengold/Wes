package com.jme3.scene.plugins.bvh;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data used to map poses from one skeleton to another: a collection of bone
 * mappings.
 *
 * @author Nehon
 */
public class SkeletonMapping implements Cloneable, Savable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SkeletonMapping.class.getName());
    // *************************************************************************
    // fields

    /**
     * map from target-bone names to bone mappings
     */
    private Map<String, BoneMapping> mappings = new HashMap<>(50);
    // *************************************************************************
    // constructors

    /**
     * A no-arg constructor to avoid javadoc warnings from JDK 18.
     */
    public SkeletonMapping() {
        // do nothing
    }

    /**
     * Construct a one-to-one mapping for the specified Armature.
     *
     * @param armature the Armature to provide the joint names (not null,
     * unaffected)
     */
    public SkeletonMapping(Armature armature) {
        List<Joint> jointList = armature.getJointList();
        for (Joint joint : jointList) {
            String name = joint.getName();
            BoneMapping boneMapping = new BoneMapping(name, name);
            addMapping(boneMapping);
        }
    }

    /**
     * Construct a one-to-one mapping for the specified Skeleton.
     *
     * @param skeleton the Skeleton to provide the bone names (not null,
     * unaffected)
     */
    public SkeletonMapping(Skeleton skeleton) {
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex) {
            Bone bone = skeleton.getBone(boneIndex);
            String name = bone.getName();
            BoneMapping boneMapping = new BoneMapping(name, name);
            addMapping(boneMapping);
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a bone mapping to this skeleton mapping.
     *
     * @param mapping bone mapping (not null)
     */
    public void addMapping(BoneMapping mapping) {
        String targetBoneName = mapping.getTargetName();
        mappings.put(targetBoneName, mapping);
    }

    /**
     * Empty this skeleton mapping.
     */
    public void clear() {
        mappings.clear();
    }

    /**
     * Count the number of bone mappings in this skeleton mapping.
     *
     * @return count (&ge;0)
     */
    public int countMappings() {
        int result = mappings.size();
        return result;
    }

    /**
     * Find the bone mapping for the named target bone.
     *
     * @param targetBoneName which target bone
     * @return the pre-existing instance, or null if none found
     */
    public BoneMapping get(String targetBoneName) {
        BoneMapping result = mappings.get(targetBoneName);
        return result;
    }

    /**
     * Find the bone mapping for the named source bone.
     *
     * @param sourceBoneName which source bone
     * @return the pre-existing instance, or null if none found
     */
    public BoneMapping getForSource(String sourceBoneName) {
        BoneMapping result = null;
        for (BoneMapping boneMapping : mappings.values()) {
            String sourceName = boneMapping.getSourceName();
            if (sourceName.equals(sourceBoneName)) {
                result = boneMapping;
                break;
            }
        }

        return result;
    }

    /**
     * Generate an inverse for this mapping.
     *
     * @return a new mapping
     */
    public SkeletonMapping inverse() {
        SkeletonMapping result = new SkeletonMapping();
        for (BoneMapping boneMapping : mappings.values()) {
            Quaternion twist = boneMapping.getTwist(); // alias
            Quaternion inverseTwist = twist.inverse();
            String targetName = boneMapping.getTargetName();
            String sourceName = boneMapping.getSourceName();
            result.map(sourceName, targetName, inverseTwist);
        }

        return result;
    }

    /**
     * Enumerate all source bones in this mapping.
     *
     * @return a new list of names
     */
    public List<String> listSourceBones() {
        int numMappings = mappings.size();
        List<String> result = new ArrayList<>(numMappings);
        for (BoneMapping boneMapping : mappings.values()) {
            String name = boneMapping.getSourceName();
            result.add(name);
        }

        return result;
    }

    /**
     * Enumerate all target bones in this mapping.
     *
     * @return a new list of names
     */
    public List<String> listTargetBones() {
        int numMappings = mappings.size();
        List<String> result = new ArrayList<>(numMappings);
        result.addAll(mappings.keySet());

        return result;
    }

    /**
     * Builds a BoneMapping with the given bone from the target skeleton and the
     * given bone from the source skeleton.
     *
     * @param targetBone the name of the bone from the target skeleton.
     * @param sourceBone the name of the bone from the source skeleton.
     * @return a new instance
     */
    public BoneMapping map(String targetBone, String sourceBone) {
        BoneMapping mapping = new BoneMapping(targetBone, sourceBone);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Add a mapping from the named bone in the target skeleton to the named
     * bone in the source skeleton, applying the specified twist.
     *
     * @param targetName name of bone in the target skeleton
     * @param sourceName name of bone in the source skeleton
     * @param twist twist rotation to apply to the animation data
     * @return a new instance
     */
    public BoneMapping map(
            String targetName, String sourceName, Quaternion twist) {
        BoneMapping boneMapping = mappings.get(targetName);
        if (boneMapping == null) {
            boneMapping = new BoneMapping(targetName, sourceName, twist);
            mappings.put(targetName, boneMapping);
        } else {
            logger.log(Level.WARNING, "Multiple mappings for target bone {0}.",
                    targetName);
        }

        return boneMapping;
    }

    /**
     * Add a mapping from the named bone in the target skeleton to the named
     * bone in the source skeleton, applying the specified twist.
     *
     * @param targetBone name of bone in the target skeleton
     * @param sourceBone name of bone in the source skeleton
     * @param twistAngle twist rotation angle
     * @param twistAxis twist rotation axis
     * @return a new instance
     */
    public BoneMapping map(String targetBone, String sourceBone,
            float twistAngle, Vector3f twistAxis) {
        BoneMapping mapping = new BoneMapping(
                targetBone, sourceBone, twistAngle, twistAxis);
        mappings.put(targetBone, mapping);

        return mapping;
    }

    /**
     * Remove a bone mapping from this skeleton mapping.
     *
     * @param mapping bone mapping (not null)
     */
    public void removeMapping(BoneMapping mapping) {
        String targetBoneName = mapping.getTargetName();
        BoneMapping oldMapping = mappings.remove(targetBoneName);
        assert oldMapping == mapping;
    }

    /**
     * Update this mapping after a source bone is renamed.
     *
     * @param oldName old name of bone
     * @param newName new name for bone
     */
    public void renameSourceBone(String oldName, String newName) {
        for (BoneMapping boneMapping : mappings.values()) {
            String sourceName = boneMapping.getSourceName();
            if (sourceName.equals(oldName)) {
                boneMapping.setSourceName(newName);
            }
        }
    }

    /**
     * Update this mapping after a target bone is renamed.
     *
     * @param oldName old name of bone
     * @param newName new name for bone
     */
    public void renameTargetBone(String oldName, String newName) {
        BoneMapping boneMapping = get(oldName);
        if (boneMapping != null) {
            removeMapping(boneMapping);
            boneMapping.setTargetName(newName);
            addMapping(boneMapping);
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this mapping.
     *
     * @return a new mapping, equivalent to this one
     * @throws java.lang.CloneNotSupportedException if superclass isn't
     * cloneable
     */
    @Override
    public SkeletonMapping clone() throws CloneNotSupportedException {
        super.clone();
        SkeletonMapping result = new SkeletonMapping();
        for (BoneMapping boneMapping : mappings.values()) {
            Quaternion twist = boneMapping.getTwist(); // alias
            twist = twist.clone();
            String targetName = boneMapping.getTargetName();
            String sourceName = boneMapping.getSourceName();
            result.map(targetName, sourceName, twist);
        }

        return result;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this mapping.
     *
     * @param im importer to use (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        this.mappings = (Map<String, BoneMapping>) ic.readStringSavableMap(
                "mappings", new HashMap<String, BoneMapping>(50));
    }

    /**
     * Serialize this instance.
     *
     * @param ex exporter to use (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeStringSavableMap(mappings, "mappings", null);
    }
}
