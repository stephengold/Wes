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

import com.jme3.math.Quaternion;
import jme3utilities.Validate;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyQuaternion;

/**
 * Enumerate and implement some interpolation techniques on time sequences of
 * unit quaternions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum TweenRotations {
    // *************************************************************************
    // values

    /**
     * cyclic normalized linear (Nlerp) interpolation
     */
    LoopNlerp,
    /**
     * cyclic spherical linear (Slerp) or "great arc" interpolation using
     * shortcuts
     */
    LoopQuickSlerp,
    /**
     * cyclic spherical linear (Slerp) or "great arc" interpolation
     */
    LoopSlerp,
    /**
     * cyclic cubic-spline interpolation based on the Squad function
     */
    LoopSpline,
    /**
     * acyclic normalized linear (Nlerp) interpolation
     */
    Nlerp,
    /**
     * acyclic spherical linear (Slerp) or "great arc" interpolation using
     * shortcuts
     */
    QuickSlerp,
    /**
     * acyclic spherical linear (Slerp) or "great arc" interpolation
     */
    Slerp,
    /**
     * acyclic cubic-spline interpolation based on the Squad function
     */
    Spline;
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate among unit quaternions in a time sequence using this
     * technique.
     *
     * @param time parameter value
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time for looping (&ge;times[lastIndex])
     * @param samples function values (not null, unaffected, same length as
     * times, each norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion interpolate(float time, float[] times, float cycleTime,
            Quaternion[] samples, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == samples.length;
        int lastIndex = times.length - 1;
        assert cycleTime >= times[lastIndex] : cycleTime;
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        if (lastIndex == 0 || time < times[0]) {
            result.set(samples[0]);
            return result;
        }

        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        loopLerp(time, lastIndex - 1, times, cycleTime,
                                samples, result);
                    } else { // fall back on acyclic
                        lerp(time, times, samples, result);
                    }
                } else {
                    loopLerp(
                            time, lastIndex, times, cycleTime, samples, result);
                }
                break;

            case LoopSpline:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        loopSpline(time, lastIndex - 1, times, cycleTime,
                                samples, result);
                    } else { // fall back on acyclic
                        spline(time, times, samples, result);
                    }
                } else {
                    loopSpline(
                            time, lastIndex, times, cycleTime, samples, result);
                }
                break;

            case Nlerp:
            case QuickSlerp:
            case Slerp:
                lerp(time, times, samples, result);
                break;

            case Spline:
                spline(time, times, samples, result);
                break;

            default:
                throw new IllegalStateException("this = " + this);
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in a time sequence using this
     * technique and some precomputed parameters.
     *
     * @param time parameter value
     * @param curve curve parameters (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion interpolate(
            float time, RotationCurve curve, Quaternion storeResult) {
        Validate.nonNull(curve, "curve");
        Quaternion result;

        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
            case Nlerp:
            case QuickSlerp:
            case Slerp:
                float[] times = curve.getTimes();
                float cycleTime = curve.getCycleTime();
                Quaternion[] samples = curve.getSamples();
                result = interpolate(
                        time, times, cycleTime, samples, storeResult);
                break;

            case LoopSpline:
                result = loopSpline(time, curve, storeResult);
                break;

            case Spline:
                result = spline(time, curve, storeResult);
                break;

            default:
                throw new IllegalStateException("this = " + this);
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * linear (Nlerp/Slerp) interpolation. Nlerp is essentially what AnimControl
     * uses to interpolate rotations in bone tracks.
     *
     * @param time parameter value (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param samples function values (not null, unaffected, same length as
     * times, each norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion lerp(float time, float[] times, Quaternion[] samples,
            Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(samples, "samples");
        assert times.length == samples.length;
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = samples[index1];

        if (index1 >= times.length - 1) { // the last point to use
            result.set(q1);
        } else {
            int index2 = index1 + 1;
            float inter12 = times[index2] - times[index1];
            assert inter12 > 0f : inter12;
            float t = (time - times[index1]) / inter12;
            Quaternion q2 = samples[index2];
            lerp(t, q1, q2, result);
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using linear
     * (Nlerp/Slerp) interpolation.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param lastIndex (index of the last point to use, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[lastIndex])
     * @param samples function values (not null, unaffected, each norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public Quaternion loopLerp(float time, int lastIndex, float[] times,
            float cycleTime, Quaternion[] samples, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(lastIndex, "last index");
        Validate.nonNull(times, "times");
        Validate.nonNull(samples, "samples");
        assert times.length > lastIndex : times.length;
        assert samples.length > lastIndex : samples.length;
        assert cycleTime > times[lastIndex] : cycleTime;

        int index1 = MyArray.findPreviousIndex(time, times);
        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < lastIndex) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = cycleTime - times[lastIndex];
        }
        assert interval > 0f : interval;

        float t = (time - times[index1]) / interval;
        Quaternion q1 = samples[index1];
        Quaternion q2 = samples[index2];
        Quaternion result = lerp(t, q1, q2, storeResult);

        return result;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param lastIndex (index of the last point to use, &ge;1)
     * @param times (not null, unaffected, in strictly ascending order,
     * times[0]==0)
     * @param cycleTime cycle time (&gt;times[lastIndex])
     * @param samples function values (not null, unaffected, each norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion loopSpline(
            float time, int lastIndex, float[] times, float cycleTime,
            Quaternion[] samples, Quaternion storeResult) {
        Validate.inRange(time, "time", 0f, cycleTime);
        Validate.positive(lastIndex, "last index");
        Validate.nonNull(times, "times");
        Validate.nonNull(samples, "samples");
        assert times.length > lastIndex : times.length;
        assert samples.length > lastIndex : samples.length;
        assert cycleTime > times[lastIndex] : cycleTime;

        int index1 = MyArray.findPreviousIndex(time, times);
        int index2; // keyframe index
        float interval; // interval between keyframes
        if (index1 < lastIndex) {
            index2 = index1 + 1;
            interval = times[index2] - times[index1];
        } else {
            index2 = 0;
            interval = cycleTime - times[lastIndex];
        }
        assert interval > 0f : interval;
        float t = (time - times[index1]) / interval;
        int index0 = (index1 == 0) ? lastIndex : index1 - 1;
        int index3 = (index2 == lastIndex) ? 0 : index2 + 1;
        Quaternion q0 = samples[index0];
        Quaternion q1 = samples[index1];
        Quaternion q2 = samples[index2];
        Quaternion q3 = samples[index3];
        Quaternion result = flipSpline(t, q0, q1, q2, q3, storeResult);

        return result;
    }

    /**
     * Generate a rotation curve.
     *
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param cycleTime end time of loop (&ge;times[lastIndex])
     * @param samples function values (not null, unaffected, same length as
     * times, each norm==1)
     * @return a new instance
     */
    public RotationCurve precompute(
            float[] times, float cycleTime, Quaternion[] samples) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert times.length == samples.length;
        int lastIndex = times.length - 1;
        assert cycleTime >= times[lastIndex] : cycleTime;

        RotationCurve result = new RotationCurve(times, cycleTime, samples);
        switch (this) {
            case LoopNlerp:
            case LoopQuickSlerp:
            case LoopSlerp:
            case Nlerp:
            case QuickSlerp:
            case Slerp:
                break;

            case LoopSpline:
                if (times[lastIndex] == cycleTime) {
                    if (lastIndex > 1) { // ignore the final point
                        precomputeLoopSpline(result, lastIndex - 1);
                    } else { // fall back on acyclic
                        precomputeSpline(result);
                    }
                } else {
                    precomputeLoopSpline(result, lastIndex);
                }
                break;

            case Spline:
                precomputeSpline(result);
                break;

            default:
                throw new IllegalStateException("this = " + this);
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;times[0])
     * @param times (not null, unaffected, length&gt;0, in strictly ascending
     * order)
     * @param samples function values (not null, unaffected, same length as
     * times, norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    public static Quaternion spline(float time, float[] times,
            Quaternion[] samples, Quaternion storeResult) {
        Validate.nonNull(times, "times");
        assert times.length > 0;
        assert time >= times[0] : time;
        Validate.nonNull(samples, "samples");
        assert times.length == samples.length;
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = samples[index1];
        int lastIndex = times.length - 1;

        if (index1 == lastIndex) {
            result.set(q1);
            return result;
        }
        // TODO try substituting q for a at the ends of the spline
        int index0 = (index1 == 0) ? 0 : index1 - 1;
        int index2 = index1 + 1;
        int index3 = (index2 == lastIndex) ? lastIndex : index2 + 1;
        float inter12 = times[index2] - times[index1];
        float t = (time - times[index1]) / inter12;
        Quaternion q0 = samples[index0];
        Quaternion q2 = samples[index2];
        Quaternion q3 = samples[index3];
        flipSpline(t, q0, q1, q2, q3, result);

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Set the sign of the specified Quaternion so that its dot product with
     * another Quaternion is non-negative.
     *
     * @param qa the Quaternion to test (not null, unaffected)
     * @param qb the Quaternion for comparison (not null, unaffected)
     * @return a new instance if qa . qb &lt 0, otherwise qa
     */
    private static Quaternion align(Quaternion qa, Quaternion qb) {
        Quaternion result;

        if (qa.dot(qb) < 0f) {
            result = qa.clone();
            result.negate(); // modifies result!
        } else {
            result = qa;
        }

        return result;
    }

    /**
     * Interpolate between the 2 middle unit quaternions in a sequence of 4
     * using cubic-spline interpolation based on the Squad function.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value preceding q1 (not null, unaffected, norm=1)
     * @param q1 function value at start of interval (not null, unaffected,
     * norm=1)
     * @param q2 function value at end of interval (not null, unaffected,
     * norm=1)
     * @param q3 function value following q2 (not null, unaffected, norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion flipSpline(float t, Quaternion q0, Quaternion q1,
            Quaternion q2, Quaternion q3, Quaternion storeResult) {
        /*
         * Flip signs as necessary to make dot products of successive
         * sampled values non-negative.
         */
        Quaternion qq0 = align(q0, q1);
        Quaternion qq2 = align(q2, q1);
        Quaternion qq3 = align(q3, qq2);

        // Calculate Squad parameter "a" at either end of the central interval.
        Quaternion a1 = MyQuaternion.squadA(qq0, q1, qq2, null);
        Quaternion a2 = MyQuaternion.squadA(q1, qq2, qq3, null);
        Quaternion result = MyQuaternion.squad(t, q1, a1, a2, qq2, storeResult);

        return result;
    }

    /**
     * Interpolate between 2 unit quaternions using linear (Nlerp/Slerp)
     * interpolation.
     *
     * @param t descaled parameter value (&ge;0, &le;1)
     * @param q0 function value at t=0 (not null, unaffected, norm=1)
     * @param q1 function value at t=1 (not null, unaffected, norm=1)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private Quaternion lerp(
            float t, Quaternion q0, Quaternion q1, Quaternion storeResult) {
        Validate.inRange(t, "t", 0f, 1f);
        MyQuaternion.validateUnit(q0, "q0", 0.0005f);
        MyQuaternion.validateUnit(q1, "q1", 0.0005f);
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        if (MyQuaternion.ne(q0, q1)) {
            switch (this) {
                case LoopNlerp:
                case Nlerp:
                    result.set(q0);
                    result.nlerp(q1, t);
                    break;
                case LoopQuickSlerp:
                case QuickSlerp:
                    Quaternion q2copy = q1.clone();
                    result.slerp(q0, q2copy, t);
                    break;
                case LoopSlerp:
                case Slerp:
                    /*
                     * Flip signs as necessary to make dot product
                     * of the sampled values non-negative.
                     */
                    if (q0.dot(q1) < 0f) {
                        Quaternion negQ1 = q1.mult(-1f);
                        MyQuaternion.slerp(t, q0, negQ1, result);
                    } else {
                        MyQuaternion.slerp(t, q0, q1, result);
                    }
                    break;
                default:
                    throw new IllegalStateException("this = " + this);
            }
        } else {
            result.set(q0);
        }

        return result;
    }

    /**
     * Interpolate among unit quaternions in a cyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;0, &le;cycleTime)
     * @param curve rotation curve (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion loopSpline(
            float time, RotationCurve curve, Quaternion storeResult) {
        assert time >= 0f : time;
        float cycleTime = curve.getCycleTime();
        Validate.inRange(time, "time", 0f, cycleTime);

        int lastIndex = curve.getLastIndex();
        float[] times = curve.getTimes();
        int index1 = MyArray.findPreviousIndex(time, times);
        if (index1 > lastIndex) {
            index1 = lastIndex;
        }

        // Interpolate using the Squad function.
        float intervalDuration = curve.getIntervalDuration(index1);
        float t = (time - times[index1]) / intervalDuration;
        Quaternion a1 = curve.getControlPoint1(index1);
        Quaternion a2 = curve.getControlPoint2(index1);
        Quaternion q1 = curve.getStartValue(index1);
        Quaternion q2 = curve.getEndValue(index1);
        Quaternion result = MyQuaternion.squad(t, q1, a1, a2, q2, storeResult);

        return result;
    }

    /**
     * Precompute curve parameters for middle of 3 intervals in a spline.
     *
     * @param curve (not null, modified)
     * @param index1 index of the q1 sample (&ge;0)
     * @param inter12 duration of interval (&gt;0)
     * @param q0 function value preceding q1 (not null, unaffected, norm=1)
     * @param q1 function value at start of interval (not null, unaffected,
     * norm=1)
     * @param q2 function value at end of interval (not null, unaffected,
     * norm=1)
     * @param q3 function value following q2 (not null, unaffected, norm=1)
     */
    private static void precomputeFlipSpline(
            RotationCurve curve, int index1, float inter12,
            Quaternion q0, Quaternion q1, Quaternion q2, Quaternion q3) {
        /*
         * Flip signs as necessary to make dot products of successive
         * sampled values non-negative.
         */
        Quaternion qq0 = align(q0, q1);
        Quaternion qq2 = align(q2, q1);
        Quaternion qq3 = align(q3, qq2);
        curve.setParameters(index1, qq2, inter12);

        Quaternion a = MyQuaternion.squadA(qq0, q1, qq2, null);
        Quaternion b = MyQuaternion.squadA(q1, qq2, qq3, null);
        curve.setControlPoints(index1, a, b);
    }

    /**
     * Precompute curve parameters for a cyclic spline.
     *
     * @param curve (not null, modified)
     * @param lastIndex index of the last point to use (&ge;0)
     */
    private static void precomputeLoopSpline(
            RotationCurve curve, int lastIndex) {
        curve.setLastIndex(lastIndex);

        float[] times = curve.getTimes();
        float cycleTime = curve.getCycleTime();
        Quaternion[] samples = curve.getSamples();

        for (int index1 = 0; index1 <= lastIndex; ++index1) {
            int index0 = (index1 == 0) ? lastIndex : index1 - 1;
            int index2;
            float inter12; // interval between keyframes
            if (index1 < lastIndex) {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            } else {
                index2 = 0;
                inter12 = cycleTime - times[lastIndex];
            }
            assert inter12 > 0f : inter12;
            int index3 = (index2 == lastIndex) ? 0 : index2 + 1;
            Quaternion q0 = samples[index0];
            Quaternion q1 = samples[index1];
            Quaternion q2 = samples[index2];
            Quaternion q3 = samples[index3];
            precomputeFlipSpline(curve, index1, inter12, q0, q1, q2, q3);
        }
    }

    /**
     * Precompute curve parameters for an acyclic spline.
     *
     * @param curve (not null, modified)
     */
    private static void precomputeSpline(RotationCurve curve) {
        Quaternion[] samples = curve.getSamples();
        int lastIndex = samples.length - 1;
        curve.setLastIndex(lastIndex);

        float[] times = curve.getTimes();

        for (int index1 = 0; index1 <= lastIndex; ++index1) {
            // TODO try substituting q for a at the ends of the spline
            int index0;
            if (index1 == 0) {
                index0 = 0;
            } else {
                index0 = index1 - 1;
            }
            float inter12;
            int index2;
            if (index1 == lastIndex) {
                index2 = lastIndex;
                inter12 = curve.getCycleTime() - times[index1] + 0.001f;
            } else {
                index2 = index1 + 1;
                inter12 = times[index2] - times[index1];
            }
            int index3 = (index2 == lastIndex) ? lastIndex : index2 + 1;

            Quaternion q0 = samples[index0];
            Quaternion q1 = samples[index1];
            Quaternion q2 = samples[index2];
            Quaternion q3 = samples[index3];
            precomputeFlipSpline(curve, index1, inter12, q0, q1, q2, q3);
        }
    }

    /**
     * Interpolate among unit quaternions in an acyclic time sequence using
     * cubic-spline interpolation based on the Squad function.
     *
     * @param time parameter value (&ge;times[0])
     * @param curve rotation curve (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return an interpolated unit quaternion (either storeResult or a new
     * instance)
     */
    private static Quaternion spline(
            float time, RotationCurve curve, Quaternion storeResult) {
        Quaternion result
                = (storeResult == null) ? new Quaternion() : storeResult;

        float[] times = curve.getTimes();
        assert time >= times[0] : time;
        int index1 = MyArray.findPreviousIndex(time, times);
        Quaternion q1 = curve.getStartValue(index1);
        int lastIndex = curve.getLastIndex();
        if (index1 == lastIndex) {
            result.set(q1);
        } else { // Interpolate using the Squad function.
            float intervalDuration = curve.getIntervalDuration(index1);
            float t = (time - times[index1]) / intervalDuration;
            Quaternion a1 = curve.getControlPoint1(index1);
            Quaternion a2 = curve.getControlPoint2(index1);
            Quaternion q2 = curve.getEndValue(index1);
            MyQuaternion.squad(t, q1, a1, a2, q2, result);
        }

        return result;
    }
}
