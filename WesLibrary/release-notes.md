# release notes for the Wes library and related examples

## Version 0.4.4for32 released on TBD

 + Rename the `Pose.apply()` method to `applyTo()`. (API change)
 + Add `normalizeQuaternions()` methods to the `AnimationEdit` and
   `TrackEdit` classes.
 + Base on version 4.1 of the jme3-utilities-heart library.
 + Upgrade to Gradle v5.6.4 .

## Version 0.4.3for32 released on 23 September 2019

 + Base on version 4.0 of the jme3-utilities-heart library.
 + Upgrade to Gradle v5.6.2 .

## Version 0.4.2 released on 26 July 2019

Base on version 2.30 of the jme3-utilities-heart library.

## Version 0.4.1 released on 7 June 2019

 + Turn on Java 7 compatibility by default.
 + Make the `gradlew` script executable.
 + Base on version 2.28.1 of the jme3-utilities-heart library.
 + Upgrade to Gradle v5.3.1 .

## Version 0.4.0 released on 18 April 2019

 + Move 4 methods from `TrackEdit` to a new `AnimationEdit` class. (API change)
 + Add a new `reverseAnimation()` method to the `AnimationEdit` class.
 + Add a `ReverseAnimation` example.

## Version 0.3.12 released on 15 April 2019

 + Add method to apply a `Pose` to a `Skeleton`.
 + Add Oto and Puppet to the `FlashMobDemo` example.
 + Re-implement `Pose.preOrderIndices()`.
 + Base on version 2.25 of the jme3-utilities-heart library.
 + Target JME 3.2.3-stable .

## Version 0.3.11 released on 12 March 2019

Base on version 2.22 of the jme3-utilities-heart library.

## Version 0.3.10 released on 9 March 2019

 + Fix an infinite loop in `TweenVectors.CentripetalSpline`
 + Base on version 2.20 of the jme3-utilities-heart library.

## Version 0.3.9 released on 16 January 2019

 + Add the `FlashMobDemo` app and related resources.
 + Base on version 2.18 of the jme3-utilities-heart library.
 + Target JME v3.2.2-stable .

On 15 January 2019, Wes moved from the Jme3-utilities Project
to a new GitHub repo.

## Version 0.3.8 released on 28 December 2018

 + Base on version 2.17 of the jme3-utilities-heart library.
 + Target JME v3.2.2-beta1 .

## Version 0.3.7 released on 23 September 2018

 + Rename `WesVersion.getVersionShort()` to `versionShort()`. (API change)
 + Base on version 2.10 of the jme3-utilities-heart library.

## Version 0.3.6 released on 24 July 2018

Base on version 2.5 of the heart library.

## Version 0.3.5 released on 14 February 2018

Remove an irrelevant test against track length from
`TrackEdit.setFrameTime()`.

## Version 0.3.4 released on 7 February 2018

 + Fix a validation bug in `TrackEdit.truncate()`.
 + Add `extractAnimation()`, `reverse()`, and `setFrameTime()` methods to the
   `TrackEdit` class.

## Version 0.3.3 released on 2 February 2018

 + Add a `simplify()` method to the `TrackEdit` class.
 + Relax a validation constraint in `TweenRotations.lerp()`.

## Version 0.3.2 released on 25 January 2018

Base on heart library v2.0 to make this library physics-independent.

## Version 0.3.1 released on 22 January 2018

 + Add `chain()` and `delayAll()` methods to the `TrackEdit` class.
 + Target JME v3.2.1 .

## Version 0.3.0for32 released on 5 December 2017

 + 1st release to target JME v3.2
 + Utilize `setTrackSpatial()` with spatial tracks

## Version 0.2.4 released on 12 November 2017

 + Add an `endWeight` argument to the `TrackEdit.wrap()` method: an API change.
 + Handle null skeleton in `Pose.rootBoneIndices()`.

## Version 0.2.3 released on 8 September 2017

 + Add fallback transforms to the `interpolate()` and `transform()` methods in
   `TweenTransforms`, for tracks that don't include all 3 transform components.
   These are API changes.

## Version 0.2.2 released on 7 September 2017

 + Generalize `BoneTrack` methods to also work for `SpatialTracks`. (This involved
   some API changes.)
 + Rename `TweenTransforms.boneTransform()` to `transform()`.
 + Add `newTrack()` and `setKeyframes()` methods to `TrackEdit` class.

## Version 0.2.1 released on 4 September 2017

This was the initial baseline release.