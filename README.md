<img height="150" src="https://i.imgur.com/1iPoJpl.png">

The [Wes Project][wes] is about editing animations used in the
[jMonkeyEngine game engine][jme].

It contains 2 sub-projects:

 1. WesLibrary: the Wes runtime library (in Java)
 2. WesExamples: demos, examples, and test software (in Java)

Complete source code (in Java) is provided under
[a 3-clause BSD license][license].

Summary of features:

 + retarget animations from one model/skeleton to another
 + interpolate between keyframes using a wide assortment of algorithms
 + reverse an animation track
 + trim a track in the time domain (behead and/or truncate)
 + chain 2 tracks together
 + repair certain issues with animation tracks

<a name="toc"/>

## Contents of this document

 + [Downloads](#downloads)
 + [Conventions](#conventions)
 + [How to build Wes from source](#build)
 + [How to add Wes to an existing project](#add)
 + [An overview of the demo applications](#demos)
 + [External links](#links)
 + [History](#history)
 + [Acknowledgments](#acks)

<a name="downloads"/>

## Downloads

Newer releases (since v0.3.9) can be downloaded from
[GitHub](https://github.com/stephengold/Wes/releases).

Older releases (v0.2.1 through v0.3.8) can be downloaded from
[the Jme3-utilities Project](https://github.com/stephengold/jme3-utilities/releases).

Newer Maven artifacts (since v0.4.8) are available from
[JCenter](https://bintray.com/stephengold/com.github.stephengold/Wes).

Older Maven artifacts (v0.2.0 through v0.4.7) are available from
[JFrog Bintray](https://bintray.com/stephengold/jme3utilities/Wes).

[Jump to table of contents](#toc)

<a name="conventions"/>

## Conventions

Package names begin with
`jme3utilities.` (if Stephen Gold holds the copyright) or
`com.jme3.` (if the jMonkeyEngine Project holds the copyright).

Both the source code and the pre-built libraries are compatible with JDK 7.

[Jump to table of contents](#toc)

<a name="build"/>

## How to build Wes from source

 1. Install build software:
   + a Java Development Kit and
   + [Gradle]
 2. Download and extract the source code from GitHub:
   + using Git:
     + `git clone https://github.com/stephengold/Wes.git`
     + `cd Wes`
     + `git checkout -b latest 0.6.0`
   + using a web browser:
     + browse to [https://github.com/stephengold/Wes/releases/latest][latest]
     + follow the "Source code (zip)" link
     + save the ZIP file
     + unzip the saved ZIP file
     + `cd` to the extracted directory/folder
 3. Set the `JAVA_HOME` environment variable:
   + using Bash:  `export JAVA_HOME="` *path to your JDK* `"`
   + using Windows Command Prompt:  `set JAVA_HOME="` *path to your JDK* `"`
 4. Run the Gradle wrapper:
   + using Bash:  `./gradlew build`
   + using Windows Command Prompt:  `.\gradlew build`

After a successful build,
Maven artifacts will be found in `WesLibrary/build/libs`.

You can install the Maven artifacts to your local cache:
 + using Bash:  `./gradlew :WesLibrary:publishToMavenLocal`
 + using Windows Command Prompt:  `.\gradlew :WesLibrary:publishToMavenLocal`

[Jump to table of contents](#toc)

<a name="add"/>

## How to add Wes to an existing project

Adding the Wes Library to an existing [jMonkeyEngine][jme] project should be
a simple matter of adding it to the classpath.

The Wes Library depends on [the Heart Library][heart],
which in turn depends on
the standard jme3-core library from jMonkeyEngine.

#### For Gradle projects

For projects built using Maven or Gradle, it is sufficient to specify the
dependency on the Wes Library.  The build tools should automatically
resolve the remaining dependencies automatically.

    repositories {
        jcenter()
    }
    dependencies {
        compile 'com.github.stephengold:Wes:0.6.0'
    }

#### For Ant projects

For project built using [Ant], download the Wes and Heart
libraries from GitHub:

 + https://github.com/stephengold/Wes/releases/tag/0.6.0
 + https://github.com/stephengold/Heart/releases/tag/6.0.0

You'll want both class jars
and probably the `-sources` and `-javadoc` jars as well.

Open the project's properties in the IDE (JME 3.2 SDK or NetBeans 8.2):

 1. Right-click on the project (not its assets) in the "Projects" window.
 2. Select "Properties to open the "Project Properties" dialog.
 3. Under "Categories:" select "Libraries".
 4. Click on the "Compile" tab.
 5. Add the `Heart` class jar:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the download folder.
    + Select the "Heart-6.0.0.jar" file.
    + Click on the "Open" button.
 6. (optional) Add jars for javadoc and sources:
    + Click on the "Edit" button.
    + Click on the "Browse..." button to the right of "Javadoc:"
    + Select the "Heart-6.0.0-javadoc.jar" file.
    + Click on the "Open" button.
    + Click on the "Browse..." button to the right of "Sources:"
    + Select the "Heart-6.0.0-sources.jar" file.
    + Click on the "Open" button again.
    + Click on the "OK" button to close the "Edit Jar Reference" dialog.
 7. Similarly, add the `Wes` jar(s).
 8. Click on the "OK" button to exit the "Project Properties" dialog.

[Jump to table of contents](#toc)

<a name="demos"/>

## An overview of the demo applications

Demo applications have been created to showcase certain features of Wes.
The following demos are found in the `jme3utilities.wes.test` package of
the WesExamples sub-project:

### FlashMobDemo

Demonstrates retargeting the "Dance" animation clip of the Sinbad model
to 4 unrelated models: Jaime, MhGame, Oto, and Puppet.

### ReverseAnimation

Demonstrates reversing the "StandUpBack" animation clip of the Sinbad model to
generate a "LieDown" clip.

### TrimAnimation

Demonstrates trimming and stretching the "SliceHorizontal" animation clip of
the Sinbad model to generate a "warn" clip.

[Jump to table of contents](#toc)

<a name="links"/>

## External links

  + [the Wes Animation Toolkit page](https://jmonkeystore.com/15054f52-c439-4bfb-9a73-80260b486333)
    at [JmonkeyStore](https://jmonkeystore.com/)

[ant]: https://ant.apache.org "Apache Ant Project"
[blender]: https://docs.blender.org "Blender Project"
[bsd3]: https://opensource.org/licenses/BSD-3-Clause "3-Clause BSD License"
[bvhretarget]: https://github.com/Nehon/bvhretarget "Bvhretarget Project"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: http://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[firefox]: https://www.mozilla.org/en-US/firefox "Firefox"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gradle]: https://gradle.org "Gradle Project"
[heart]: https://github.com/stephengold/Heart "Heart Project"
[imgur]: https://imgur.com/ "Imgur"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org  "jMonkeyEngine Project"
[latest]: https://github.com/stephengold/Wes/releases/latest "latest release"
[license]: https://github.com/stephengold/Wes/blob/for_jME3.2/LICENSE "Wes license"
[log]: https://github.com/stephengold/Wes/blob/for_jME3.2/WesLibrary/release-notes.md "release log"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[maud]: https://github.com/stephengold/Maud "Maud editor"
[mint]: https://linuxmint.com "Linux Mint Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-utilities Project"
[vegdahl]: http://www.cessen.com "Nathan Vegdahl"
[wes]: https://github.com/stephengold/Wes "Wes Project"
[winmerge]: http://winmerge.org "WinMerge Project"

[Jump to table of contents](#toc)

<a name="history"/>

## History

The evolution of the project is chronicled in [its release log][log].

Much of the code was originally developed for the [Maud editor][maud].

The retargeting code was originally developed by Rémy Bouquet (aka "nehon")
for his [Bvhretarget Project][bvhretarget].

From September 2017 to January 2019, Wes was a sub-project of
[the Jme3-utilities Project][utilities].

Since January 2019, Wes has been a separate project at
[GitHub][wes].

[Jump to table of contents](#toc)

<a name="acks"/>

## Acknowledgments

Like most projects, the Wes Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Rémy Bouquet (aka "nehon") for creating the Jaime model and
  the [Bvhretarget Project][bvhretarget]
  (on which `BoneMapping` and `SkeletonMapping` are based)
  and also for many helpful insights
+ Zi Ye, for creating the Sinbad model
+ [Nathan Vegdahl][vegdahl], for creating the Puppet model
+ Paul Speed, for helpful insights
+ the creators of (and contributors to) the following software:
    + the [FindBugs] source-code analyzer
    + the [Git] revision-control system and GitK commit viewer
    + the [Firefox] and [Google Chrome][chrome] web browsers
    + the [Gradle] build tool
    + the Java compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman] Community
    + the [Markdown] document-conversion tool
    + Microsoft Windows
    + the [NetBeans] integrated development environment
    + the PMD source-code analyzer
    + the [WinMerge] differencing and merging tool

I am grateful to [JFrog] and [Github], and [Imgur]
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)