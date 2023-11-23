/*
 Copyright (c) 2017-2023, Stephen Gold
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
import com.jme3.anim.Armature;
import com.jme3.anim.TransformTrack;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.clone.Cloner;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MyMesh;
import jme3utilities.Validate;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods used to translate an animated C-G model for support or
 * traction.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class SupportUtils {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SupportUtils.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Z}
     */
    final private static Vector3f zAxis = new Vector3f(0f, 0f, 1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private SupportUtils() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Find the point of vertical support (minimum Y coordinate) for the
     * specified geometry transformed by the specified skinning matrices.
     *
     * @param geometry (not null)
     * @param skinningMatrices (not null, unaffected)
     * @param storeLocation point in world coordinates (not null, modified)
     * @return index of vertex in the geometry's mesh (&ge;0) or -1 if none
     * found
     */
    public static int findSupport(Geometry geometry,
            Matrix4f[] skinningMatrices, Vector3f storeLocation) {
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Validate.nonNull(storeLocation, "store location");

        int bestIndex = -1;
        float bestY = Float.POSITIVE_INFINITY;

        Vector3f meshLoc = new Vector3f();
        Vector3f worldLoc = new Vector3f();

        Mesh mesh = geometry.getMesh();
        int maxWeightsPerVertex = mesh.getMaxNumWeights();

        VertexBuffer posBuf
                = mesh.getBuffer(VertexBuffer.Type.BindPosePosition);
        FloatBuffer posBuffer = (FloatBuffer) posBuf.getDataReadOnly();
        posBuffer.rewind();

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getDataReadOnly();
        weightBuffer.rewind();

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        Buffer boneIndexBuffer = biBuf.getData();
        boneIndexBuffer.rewind();

        int numVertices = posBuffer.remaining() / MyVector3f.numAxes;
        for (int vertexIndex = 0; vertexIndex < numVertices; ++vertexIndex) {
            float bx = posBuffer.get(); // bind position
            float by = posBuffer.get();
            float bz = posBuffer.get();

            meshLoc.zero();
            for (int wIndex = 0; wIndex < maxWeightsPerVertex; ++wIndex) {
                float weight = weightBuffer.get();
                int boneIndex = MyBuffer.readIndex(boneIndexBuffer);
                if (weight != 0f) {
                    Matrix4f s = skinningMatrices[boneIndex];
                    float xOff = s.m00 * bx + s.m01 * by + s.m02 * bz + s.m03;
                    float yOff = s.m10 * bx + s.m11 * by + s.m12 * bz + s.m13;
                    float zOff = s.m20 * bx + s.m21 * by + s.m22 * bz + s.m23;
                    meshLoc.x += weight * xOff;
                    meshLoc.y += weight * yOff;
                    meshLoc.z += weight * zOff;
                }
            }

            if (geometry.isIgnoreTransform()) {
                worldLoc.set(meshLoc);
            } else {
                geometry.localToWorld(meshLoc, worldLoc);
            }
            if (worldLoc.y < bestY) {
                bestIndex = vertexIndex;
                bestY = worldLoc.y;
                storeLocation.set(worldLoc);
            }

            for (int wIndex = maxWeightsPerVertex; wIndex < 4; ++wIndex) {
                weightBuffer.get();
                MyBuffer.readIndex(boneIndexBuffer);
            }
        }

        return bestIndex;
    }

    /**
     * Find the point of vertical support (minimum Y coordinate) for the meshes
     * in the specified subtree, each transformed by the specified skinning
     * matrices.
     *
     * @param subtree (may be null)
     * @param skinningMatrices (not null, unaffected)
     * @param storeLocation point in world coordinates (not null, modified)
     * @param storeGeometry (not null, modified)
     * @return index of vertex in storeGeometry's mesh (&ge;0) or -1 if none
     * found
     */
    public static int findSupport(Spatial subtree, Matrix4f[] skinningMatrices,
            Vector3f storeLocation, Geometry[] storeGeometry) {
        Validate.nonNull(skinningMatrices, "skinning matrices");
        Validate.nonNull(storeLocation, "store location");
        Validate.nonNull(storeGeometry, "store geometry");
        assert storeGeometry.length == 1 : storeGeometry.length;

        int bestIndex = -1;
        storeGeometry[0] = null;
        float bestY = Float.POSITIVE_INFINITY;
        Vector3f tmpLocation = new Vector3f();

        if (subtree instanceof Geometry) {
            Geometry geometry = (Geometry) subtree;
            int index = findSupport(geometry, skinningMatrices, tmpLocation);
            if (tmpLocation.y < bestY) {
                bestIndex = index;
                storeGeometry[0] = geometry;
                storeLocation.set(tmpLocation);
            }

        } else if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            Geometry[] tmpGeometry = new Geometry[1];
            for (Spatial child : children) {
                int index = findSupport(child, skinningMatrices, tmpLocation,
                        tmpGeometry);
                if (tmpLocation.y < bestY) {
                    bestIndex = index;
                    bestY = tmpLocation.y;
                    storeGeometry[0] = tmpGeometry[0];
                    storeLocation.set(tmpLocation);
                }
            }
        }

        return bestIndex;
    }

    /**
     * Calculate the sensitivity of the indexed vertex to translations of the
     * indexed Bone in the specified pose.
     *
     * @param boneIndex which bone to translate (&ge;0)
     * @param geometry (not null)
     * @param vertexIndex index into the geometry's vertices (&ge;0)
     * @param pose (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the sensitivity matrix (either storeResult or a new instance)
     */
    public static Matrix3f sensitivity(int boneIndex, Geometry geometry,
            int vertexIndex, Pose pose, Matrix3f storeResult) {
        Validate.nonNull(geometry, "geometry");
        Matrix3f result = (storeResult == null) ? new Matrix3f() : storeResult;

        // Create a clone of the Pose for temporary modifications.
        Cloner cloner = new Cloner();
        Object skeleton = pose.findSkeleton();
        if (skeleton != null) {  // Don't clone the skeleton!
            cloner.setClonedValue(skeleton, skeleton);
        }
        Pose testPose = cloner.clone(pose);

        Vector3f testWorld = new Vector3f();
        Vector3f baseWorld = new Vector3f();
        int numBones = pose.countBones();
        Matrix4f[] matrices = new Matrix4f[numBones];

        pose.userTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, baseWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(xAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        result.setColumn(0, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(yAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        result.setColumn(1, testWorld);

        pose.userTranslation(boneIndex, testWorld);
        testWorld.addLocal(zAxis);
        testPose.setTranslation(boneIndex, testWorld);
        testPose.skin(matrices);
        MyMesh.vertexWorldLocation(geometry, vertexIndex, matrices, testWorld);
        testWorld.subtractLocal(baseWorld);
        result.setColumn(2, testWorld);

        return result;
    }

    /**
     * Calculate translations of the specified joint to put the point of support
     * initially at the specified Y-coordinate.
     *
     * @param oldClip the clip to be replaced (not null, unaffected)
     * @param jointIndex the index of the target joint (&ge;0)
     * @param armature the Armature of the model (not null, unaffected)
     * @param subtree the scene-graph subtree containing all vertices to
     * consider (not null, unaffected)
     * @param supportY world Y-coordinate for support
     * @return a new array of translation vectors or {@code null} if
     * unsuccessful
     */
    public static Vector3f[] translateForInitialSupport(
            AnimClip oldClip, int jointIndex, Armature armature,
            Spatial subtree, float supportY) {
        Validate.nonNull(oldClip, "old clip");
        Validate.nonNegative(jointIndex, "joint index");
        Validate.nonNull(armature, "armature");
        Validate.nonNull(subtree, "subtree");

        TransformTrack oldTrack
                = MyAnimation.findJointTrack(oldClip, jointIndex);
        assert oldTrack != null;

        Pose tempPose = new Pose(armature);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        float[] times = oldTrack.getTimes(); // alias
        float trackTime = times[0];
        tempPose.setToClip(oldClip, trackTime);
        tempPose.skin(skinningMatrices);

        Geometry[] geometryRef = new Geometry[1];
        Vector3f worldOffset = new Vector3f();
        Matrix3f sensMat = new Matrix3f();

        // Calculate the world offset for the first keyframe:
        int vertexIndex = findSupport(subtree, skinningMatrices, worldOffset,
                geometryRef);
        assert vertexIndex != -1;
        worldOffset.x = 0f;
        worldOffset.y = supportY - worldOffset.y;
        worldOffset.z = 0f;

        // Convert the world offset to a bone offset:
        Geometry geometry = geometryRef[0];
        sensitivity(jointIndex, geometry, vertexIndex, tempPose, sensMat);
        float det = sensMat.determinant();
        if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
            return null;
        }
        sensMat.invertLocal();
        Vector3f boneOffset = sensMat.mult(worldOffset, null);

        // Apply the bone offset across the entire track:
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        int numKeyframes = times.length;
        Vector3f[] result = new Vector3f[numKeyframes];
        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            // Modify the keyframe's translation:
            Vector3f translation = oldTranslations[frameIndex];
            result[frameIndex] = translation.add(boneOffset);
        }

        return result;
    }

    /**
     * Calculate translations of the specified joint to keep the point of
     * support at the specified Y-coordinate.
     *
     * @param oldClip the clip to be replaced (not null, unaffected)
     * @param jointIndex the index of the target joint (&ge;0)
     * @param armature the Armature of the model (not null, unaffected)
     * @param subtree the scene-graph subtree containing all vertices to
     * consider (not null, unaffected)
     * @param supportY world Y-coordinate for support
     * @return a new array of translation vectors or {@code null} if
     * unsuccessful
     */
    public static Vector3f[] translateForSupport(
            AnimClip oldClip, int jointIndex, Armature armature,
            Spatial subtree, float supportY) {
        Validate.nonNull(oldClip, "old clip");
        Validate.nonNegative(jointIndex, "joint index");
        Validate.nonNull(armature, "armature");
        Validate.nonNull(subtree, "subtree");

        TransformTrack oldTrack
                = MyAnimation.findJointTrack(oldClip, jointIndex);
        assert oldTrack != null;

        Pose tempPose = new Pose(armature);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Geometry[] geometryRef = new Geometry[1];
        Vector3f worldOffset = new Vector3f();
        Matrix3f sensMat = new Matrix3f();

        // Calculate a new bone translation for each keyframe:
        float[] times = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        int numKeyframes = times.length;
        Vector3f[] result = new Vector3f[numKeyframes];
        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            float trackTime = times[frameIndex];
            tempPose.setToClip(oldClip, trackTime);
            tempPose.skin(skinningMatrices);
            int vertexIndex = findSupport(
                    subtree, skinningMatrices, worldOffset, geometryRef);
            assert vertexIndex != -1;
            worldOffset.x = 0f;
            worldOffset.y = supportY - worldOffset.y;
            worldOffset.z = 0f;

            // Convert the world offset to a bone offset:
            Geometry geometry = geometryRef[0];
            sensitivity(jointIndex, geometry, vertexIndex, tempPose, sensMat);
            float det = sensMat.determinant();
            if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
                return null;
            }
            sensMat.invertLocal();
            Vector3f boneOffset = sensMat.mult(worldOffset, null);

            // Modify the keyframe's translation:
            Vector3f translation = oldTranslations[frameIndex];
            result[frameIndex] = translation.add(boneOffset);
        }

        return result;
    }

    /**
     * Calculate translations of the specified joint to simulate traction at the
     * point of support.
     *
     * @param oldClip the clip to be replaced (not null, unaffected)
     * @param jointIndex the index of the target joint (&ge;0)
     * @param armature the Armature of the model (not null, unaffected)
     * @param subtree the scene-graph subtree containing all vertices to
     * consider (not null, unaffected)
     * @return a new array of translation vectors or {@code null} if
     * unsuccessful
     */
    public static Vector3f[] translateForTraction(AnimClip oldClip,
            int jointIndex, Armature armature, Spatial subtree) {
        Validate.nonNull(oldClip, "old clip");
        Validate.nonNegative(jointIndex, "joint index");
        Validate.nonNull(armature, "armature");
        Validate.nonNull(subtree, "subtree");

        TransformTrack oldTrack
                = MyAnimation.findJointTrack(oldClip, jointIndex);
        assert oldTrack != null;

        Pose tempPose = new Pose(armature);
        int numBones = tempPose.countBones();

        Matrix4f[] skinningMatrices = new Matrix4f[numBones];
        Geometry[] previousGeometryRef = new Geometry[1];
        Vector3f previousWorld = new Vector3f();
        Vector3f worldOffset = new Vector3f();
        Vector3f w = new Vector3f();
        Matrix3f sensMat = new Matrix3f();

        // Calculate a new bone translation for each keyframe:
        float[] times = oldTrack.getTimes(); // alias
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        int numKeyframes = times.length;
        Vector3f[] result = new Vector3f[numKeyframes];
        int previousVertexIndex = -1;
        for (int frameIndex = 0; frameIndex < numKeyframes; ++frameIndex) {
            float trackTime = times[frameIndex];
            tempPose.setToClip(oldClip, trackTime);
            tempPose.skin(skinningMatrices);

            Vector3f translation = oldTranslations[frameIndex];
            if (previousVertexIndex == -1) {
                worldOffset.zero(); // no offset for first keyframe
                result[frameIndex] = translation;

            } else {
                MyMesh.vertexWorldLocation(previousGeometryRef[0],
                        previousVertexIndex, skinningMatrices, w);
                previousWorld.subtractLocal(w);
                worldOffset.addLocal(previousWorld);

                // Convert the world offset to a bone offset:
                sensitivity(jointIndex, previousGeometryRef[0],
                        previousVertexIndex, tempPose, sensMat);
                float determinant = sensMat.determinant();
                if (FastMath.abs(determinant) <= FastMath.FLT_EPSILON) {
                    return null;
                }
                sensMat.invertLocal();
                Vector3f boneOffset = sensMat.mult(worldOffset, null);
                result[frameIndex] = translation.add(boneOffset);
            }
            /*
             * Using the original skinning matrices, pick a vertex to serve as
             * a reference for the next frame.
             */
            previousVertexIndex = findSupport(subtree, skinningMatrices,
                    previousWorld, previousGeometryRef);
            assert previousVertexIndex != -1;
            assert previousGeometryRef[0] != null;
        }

        return result;
    }
}
