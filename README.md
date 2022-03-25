<img height="150" src="https://i.imgur.com/1iPoJpl.png" alt="Wes Project logo">

[The Wes Project][wes] is about editing animations used in
[the jMonkeyEngine (JME) game engine][jme].

It contains 2 sub-projects:

1. WesLibrary: the Wes runtime library
2. WesExamples: demos, examples, and non-automated test software

Complete source code (in Java) is provided under
[a 3-clause BSD license][license].


<a name="toc"></a>

## Contents of this document

+ [Important features](#features)
+ [How to add Wes to an existing project](#add)
+ [How to build Wes from source](#build)
+ [Downloads](#downloads)
+ [Conventions](#conventions)
+ [An overview of the demo applications](#demos)
+ [External links](#links)
+ [History](#history)
+ [Acknowledgments](#acks)


<a name="features"></a>

## Important features

+ extract a time-range from an animation
+ resample an animation track at a new frame rate
+ retarget animations from one model/skeleton to another
+ interpolate between keyframes using a wide assortment of algorithms
+ reverse an animation track
+ chain 2 tracks together
+ repair certain issues with animation tracks
+ convert a traveling animation to an in-place cyclic animation

Most of the features work with both the old and new animation systems.

[Jump to table of contents](#toc)


<a name="add"></a>

## How to add Wes to an existing project

Adding the Wes Library to an existing [jMonkeyEngine][jme] project should be
a simple matter of adding it to the classpath.

The Wes Library depends on [the Heart Library][heart],
which in turn depends on
the standard "jme3-core" library from jMonkeyEngine.

For projects built using Maven or [Gradle], it is sufficient to specify the
dependency on the Wes Library.  The build tools should automatically
resolve the remaining dependencies.

### Gradle-built projects

Add to the project’s "build.gradle" file:

    repositories {
        mavenCentral()
    }
    dependencies {
        implementation 'com.github.stephengold:Wes:0.7.1'
    }

For some older versions of Gradle,
it's necessary to replace `implementation` with `compile`.

### Maven-built projects

Add to the project’s "pom.xml" file:

    <repositories>
      <repository>
        <id>mvnrepository</id>
        <url>https://repo1.maven.org/maven2/</url>
      </repository>
    </repositories>

    <dependency>
      <groupId>com.github.stephengold</groupId>
      <artifactId>Wes</artifactId>
      <version>0.7.1</version>
    </dependency>

### Ant-built projects

For projects built using [Ant], download the Wes and [Heart]
libraries from GitHub:

+ https://github.com/stephengold/Wes/releases/tag/latest
+ https://github.com/stephengold/Heart/releases/tag/7.3.0

You'll want both class jars
and probably the `-sources` and `-javadoc` jars as well.

Open the project's properties in the IDE (JME 3.2 SDK or NetBeans 8.2):

1. Right-click on the project (not its assets) in the "Projects" window.
2. Select "Properties" to open the "Project Properties" dialog.
3. Under "Categories:" select "Libraries".
4. Click on the "Compile" tab.
5. Add the [Heart] class jar:
  + Click on the "Add JAR/Folder" button.
  + Navigate to the download folder.
  + Select the "Heart-7.3.0.jar" file.
  + Click on the "Open" button.
6. (optional) Add jars for javadoc and sources:
  + Click on the "Edit" button.
  + Click on the "Browse..." button to the right of "Javadoc:"
  + Select the "Heart-7.3.0-javadoc.jar" file.
  + Click on the "Open" button.
  + Click on the "Browse..." button to the right of "Sources:"
  + Select the "Heart-7.3.0-sources.jar" file.
  + Click on the "Open" button again.
  + Click on the "OK" button to close the "Edit Jar Reference" dialog.
7. Similarly, add the Wes jar(s).
8. Click on the "OK" button to exit the "Project Properties" dialog.

[Jump to table of contents](#toc)


<a name="build"></a>

## How to build Wes from source

1. Install a [Java Development Kit (JDK)][adoptium],
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation:
   (The path might be something like "C:\Program Files\Java\jre1.8.0_301"
   or "/usr/lib/jvm/java-8-openjdk-amd64/" or
   "/Library/Java/JavaVirtualMachines/liberica-jdk-17-full.jdk/Contents/Home" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the Wes source code from GitHub:
  + using Git:
    + `git clone https://github.com/stephengold/Wes.git`
    + `cd Wes`
    + `git checkout -b latest 0.7.1`
  + using a web browser:
    + browse to [the latest release][latest]
    + follow the "Source code (zip)" link
    + save the ZIP file
    + extract the contents of the saved ZIP file
    + `cd` to the extracted directory/folder
4. Run the [Gradle] wrapper:
  + using Bash or PowerShell or Zsh: `./gradlew build`
  + using Windows Command Prompt: `.\gradlew build`

After a successful build,
Maven artifacts will be found in "WesLibrary/build/libs".

You can install the artifacts to your local Maven repository:
+ using Bash or PowerShell or Zsh: `./gradlew install`
+ using Windows Command Prompt: `.\gradlew install`

You can restore the project to a pristine state:
+ using Bash or PowerShell or Zsh: `./gradlew clean`
+ using Windows Command Prompt: `.\gradlew clean`

[Jump to table of contents](#toc)


<a name="downloads"></a>

## Downloads

Newer releases (since v0.3.9) can be downloaded from
[GitHub](https://github.com/stephengold/Wes/releases).

Old releases (v0.2.1 through v0.3.8) can be downloaded from
[the Jme3-utilities Project](https://github.com/stephengold/jme3-utilities/releases).

Newer Maven artifacts (since v0.6.2) are available from
[MavenCentral](https://search.maven.org/artifact/com.github.stephengold/Wes).

Old Maven artifacts (v0.4.8 thru v0.6.1) are available from JCenter.

[Jump to table of contents](#toc)


<a name="conventions"></a>

## Conventions

Package names begin with
`jme3utilities.` (if Stephen Gold holds the copyright) or
`com.jme3.` (if the jMonkeyEngine Project holds the copyright).

The source code is compatible with JDK 7.
The pre-built libraries are compatible with JDK 8.

[Jump to table of contents](#toc)


<a name="demos"></a>

## An overview of the demo applications

Demo applications have been created to showcase certain features of Wes.
The following demos are found in the `jme3utilities.wes.test` package of
the WesExamples sub-project:

### ConvertToInPlace

Demonstrates converting the "hurricane_kick" animation
(from Adi Barda's ninja-fighter model) to an in-place cyclic animation.
The unmodified animation is shown on a white skeleton,
synchronized with the converted animation, which is shown on a yellow skeleton.

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


<a name="links"></a>

## External links

+ [the Wes Animation Toolkit page](https://store.jmonkeyengine.org/15054f52-c439-4bfb-9a73-80260b486333)
  at [JmonkeyStore](https://store.jmonkeyengine.org)

[Jump to table of contents](#toc)


<a name="history"></a>

## History

The evolution of this project is chronicled in
[its release log][log].

Much of the code was originally developed for the [Maud editor][maud].

The retargeting code was originally developed by Rémy Bouquet (aka "nehon")
for his [Bvhretarget Project][bvhretarget].

From September 2017 to January 2019, Wes was a sub-project of
[the Jme3-utilities Project][utilities].

Since January 2019, Wes has been a separate project, hosted at
[GitHub][wes].

[Jump to table of contents](#toc)


<a name="acks"></a>

## Acknowledgments

Like most projects, the Wes Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Rémy Bouquet (aka "nehon") for creating the Jaime model and
  the [Bvhretarget Project][bvhretarget]
  (on which `BoneMapping` and `SkeletonMapping` are based)
  and also for many helpful insights
+ Adi Barda, for creating the ninja-fighter model.
+ Zi Ye, for creating the Sinbad model
+ [Nathan Vegdahl][vegdahl], for creating the Puppet model
+ Paul Speed, for helpful insights
+ Ali (aka "Ali_RS) for sharing a code snippet that inspired
  the `convertToInPlace()` methods.
+ plus the creators of (and contributors to) the following software:
    + the [FindBugs] source-code analyzer
    + the [Firefox] and [Chrome] web browsers
    + the [Git] revision-control system and GitK commit viewer
    + the [Gradle] build tool
    + the [IntelliJ IDEA][idea] and [NetBeans] integrated development environments
    + the [Java] compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman] 3-D character creation tool
    + the [Markdown] document-conversion tool
    + the [Meld] visual merge tool
    + Microsoft Windows
    + the PMD source-code analyzer
    + the [WinMerge] differencing and merging tool

I am grateful to [GitHub], [Sonatype], [JFrog], and [Imgur]
for providing free hosting for this project
and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know, so I can
correct the situation: sgold@sonic.net

[Jump to table of contents](#toc)


[adoptium]: https://adoptium.net/releases.html "Adoptium Project"
[ant]: https://ant.apache.org "Apache Ant Project"
[blender]: https://docs.blender.org "Blender Project"
[bvhretarget]: https://github.com/Nehon/bvhretarget "Bvhretarget Project"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: https://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[firefox]: https://www.mozilla.org/en-US/firefox "Firefox"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gltf]: https://www.khronos.org/gltf "glTF Project"
[gradle]: https://gradle.org "Gradle Project"
[heart]: https://github.com/stephengold/Heart "Heart Project"
[idea]: https://www.jetbrains.com/idea/ "IntelliJ IDEA"
[imgur]: https://imgur.com/ "Imgur"
[java]: https://java.com "Java"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: https://jmonkeyengine.org  "jMonkeyEngine Project"
[latest]: https://github.com/stephengold/Wes/releases/latest "latest release"
[license]: https://github.com/stephengold/Wes/blob/master/LICENSE "Wes license"
[log]: https://github.com/stephengold/Wes/blob/master/WesLibrary/release-notes.md "release log"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[maud]: https://github.com/stephengold/Maud "Maud editor"
[meld]: https://meldmerge.org "Meld Tool"
[mint]: https://linuxmint.com "Linux Mint Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[ogre]: http://www.ogre3d.org "Ogre Project"
[sonatype]: https://www.sonatype.com "Sonatype"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-utilities Project"
[vegdahl]: https://www.cessen.com "Nathan Vegdahl"
[wes]: https://github.com/stephengold/Wes "Wes Project"
[winmerge]: https://winmerge.org "WinMerge Project"
