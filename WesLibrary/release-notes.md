# release log for the Wes library and related examples

## Version 0.6.8: released on 23 January 2022

 + Bugfix:  logic error in the `TrackEdit.behead()` method
 + Deleted the `Pose.clone()` method.
 + Added checks for translation and rotation identities
   to the `TrackEdit.simplify(TransformTrack)` method.
 + Finalized the `WesVersion` class.
 + Added messages to 18 exceptions.
 + Targeted JME version 3.4.0-stable and Java v8.
 + Based on v7.2.0 of the Heart Library and v0.9.6 of jme3-utilities-ui.
 + Began using LWJGL v3 and `AbstractDemo` in examples.
 + Upgraded to Gradle v7.3.3 .

## Version 0.6.7: released on 22 August 2021

 + Based on v7.0.0 of the Heart Library and v0.9.5 of jme3-utilities-ui.
 + Upgraded to Gradle v7.2 .

## Version 0.6.6: released on 1 June 2021

 + Handle null components in transform tracks.
 + Target JME version 3.4.0-stable .

## Version 0.6.5+for34: released on 25 May 2021

Bugfix: `TrackEdit.truncate()` generates repetitious keyframes (Wes issue #2)

## Version 0.6.4+for34: released on 24 May 2021

 + Added a `simplify()` method with tolerance to the `TrackEdit` class.
 + Target JME version 3.4.0-beta4 .
 + Upgrade to Gradle v7.0.2 .

## Version 0.6.3+for34 released on 24 April 2021

 + Base on v6.4.3+for34 of the Heart Library
   and v0.9.3+for34 of jme3-utilities-ui.
 + Target JME version 3.4.0-beta1 .
 + Upgrade to Gradle v7.0 .

## Version 0.6.2 released on 9 February 2021

 + Published to MavenCentral instead of JCenter.
 + Base on v6.4.2 of the Heart Library and v0.9.2 of jme3-utilities-ui.
 + Upgrade to Gradle v6.8.2 .

## Version 0.6.1 released on 6 September 2020

Bugfix: `Pose` calculates skinning incorrectly for an `Armature`.

## Version 0.6.0 released on 29 August 2020

 + Added a `userForLocal()` method to the `Pose` class.
 + Added methods to better support the new animation system:
   + a `captureToClip()` method in the `Pose` class
   + 12 methods in the `TrackEdit` class
   + `removeRepeats()` and `zeroFirst()` methods for `AnimClip`
 + Improved the example applications.
 + Upgrade to Gradle v6.6.1 .

## Version 0.5.2 released on 22 August 2020

 + Bugfix: `TrackEdit.normalizeQuaternions()` was ineffective.
 + Added a factory method to construct a `Pose` from a `SkeletonControl`
   or `SkinningControl`.
 + Enhanced a `Pose` constructor to reduce cloning.

## Version 0.5.1 released on 16 August 2020

 + Base on v6.0 of the Heart Library and v0.9.0 of jme3-utilities-ui.
 + Upgrade to Gradle v6.6 .

## Version 0.5.0 released on 22 May 2020

 + Added methods to better support the new animation system:
   + `AnimationEdit.extractAnimation()` for `AnimClip`
   + `AnimationEdit.setDuration()` for `AnimClip`
   + `TrackEdit.behead()` for `TransformTrack`
   + `TrackEdit.reverse()` for `MorphTrack`
   + `TrackEdit.setDuration()` for `TransformTrack`
   + `TrackEdit.truncate()` for `TransformTrack`
   + `TweenTransforms.interpolate()` for `TransformTrack`
 + Added the `TrimAnimation` example app.
 + Base on v5.4 of the Heart Library and v0.8.3 of jme3-utilities-ui.
 + Upgrade to Gradle v6.4.1 .

## Version 0.4.9 released on 1 April 2020

 + Base on version 5.2.1 of the Heart Library.
 + Upgrade to Gradle v6.3 .
 + Target JME version 3.3.0-stable .

## Version 0.4.8for33 released on 5 February 2020

 + Change the Maven groupId to "com.github.stephengold" .
 + Base on version 5.0 of the Heart Library.
 + Upgrade to Gradle v6.1.1 .

## Version 0.4.7for33 released on 4 January 2020

 + Improve animation synchronization in `FlashMobDemo`.
 + Add help nodes, anti-aliasing, and screenshots to the `FlashMobDemo`
   and `ReverseAnimation` applications.
 + Base on version 4.3 of the jme3-utilities-heart library.
 + Target the NEW JME 3.3.0-beta1 .

## Version 0.4.6for33 released on 9 December 2019

 + Add a `normalizeQuaternions()` method for `AnimTrack`.
 + Synchronize the animations in `FlashMobDemo`.
 + Base on version 4.2 of the jme3-utilities-heart library.
 + Upgrade to Gradle v6.0.1 .
 + Target JME v3.3.0-beta1, which was later deleted!

## Version 0.4.5for33 released on 8 November 2019

 + Add `normalizeQuaternions()` methods to the `AnimationEdit` and
   `TrackEdit` classes.
 + Base on version 4.1 of the jme3-utilities-heart library.
 + Upgrade to Gradle v5.6.4 .

## Version 0.4.3for33 released on 1 October 2019

 + Add support for `Armature`, `AnimClip`, `AnimComposer`, and `Joint`.
 + Target JME 3.3.0-alpha5 .

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