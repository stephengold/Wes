<img height="150" src="https://i.imgur.com/1iPoJpl.png">

The [Wes Project][wes] is about editing
animations used in the [jMonkeyEngine Game Engine][jme].

It contains 2 sub-projects:

 1. WesLibrary: the Wes runtime library (in Java)
 2. WesExamples: demos, examples, and test software (in Java)

Summary of features:

 + retarget animations from one model/skeleton to another
 + interpolate between keyframes using a wide assortment of algorithms
 + reverse an animation track
 + trim a track in the time domain (behead and/or truncate)
 + chain 2 tracks together

## Contents of this document

 + [Downloads](#downloads)
 + [Conventions](#conventions)
 + [History](#history)
 + [How to install the SDK and the Wes Project](#install)
 + [How to add Wes to an existing project](#add)
 + [Acknowledgments](#acks)

<a name="downloads"/>

## Downloads

Older releases (v0.2.1 through v0.3.8) can be downloaded from
[the Jme3-utilities Project](https://github.com/stephengold/jme3-utilities/releases).

Maven artifacts are available from
[JFrog Bintray](https://bintray.com/stephengold/jme3utilities).

<a name="conventions"/>

## Conventions

Package names begin with
`jme3utilities.wes` (if Stephen Gold holds the copyright) or
`com.jme3.bullet` (if the jMonkeyEngine Project holds the copyright).

The source code is compatible with JDK 7.

<a name="history"/>

## History

Since January 2019, the Wes Project has been an independent project at
[GitHub][wes].

From September 2017 to January 2019, Wes was a sub-project of
[the Jme3-Utilities Project][utilities].

Much of the code was developed for the [Maud editor][maud].

The retargeting code was originally developed by Rémy Bouquet (aka "nehon")
for his [Bvhretarget Project][bvhretarget].

The evolution of Wes is chronicled in
[its release notes](https://github.com/stephengold/Wes/blob/master/WesLibrary/release-notes.md).

<a name="install"/>

## How to install the SDK and the Wes Project

### jMonkeyEngine3 (jME3) Software Development Kit (SDK)

Wes currently targets Version 3.2.2 of jMonkeyEngine.
You are welcome to use the Engine without also using the SDK, but I use the SDK,
and the following installation instructions assume you will too.

The hardware and software requirements of the SDK are documented on
[the JME wiki](https://jmonkeyengine.github.io/wiki/jme3/requirements.html).

 1. Download a jMonkeyEngine 3.2 SDK from
    [GitHub](https://github.com/jMonkeyEngine/sdk/releases).
 2. Install the SDK, which includes:
    + the engine itself,
    + an integrated development environment (IDE) based on NetBeans,
    + various plugins, and
    + the Blender 3D application.
 3. To open the Wes project in the IDE (or NetBeans), you will need the
    `Gradle Support` plugin.  Download and install it before proceeding.
    If this plugin isn't shown in the IDE's "Plugins" tool,
    you can download it from
    [GitHub](https://github.com/kelemen/netbeans-gradle-project/releases).
    You don't need this plugin if you merely want to use a pre-built Wes
    release in an Ant project.

### Source files

Clone the repository using Git:

 1. Open the Clone wizard in the IDE:
     + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    `https://github.com/stephengold/Wes.git`
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    which doesn't already contain "Wes".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Complete" dialog appears, click on the "Open Project..."
    button.
12. Expand the root project node to reveal the sub-projects.
13. Select both sub-projects using control-click, then click on the
    "Open" button.

### Build the project

 1. In the "Projects" window, right-click on the "WesExamples" sub-project to
    select it.
 2. Select "Build".

<a name="add"/>

## How to add Wes to an existing project

To add Wes to an existing JME3 project, simply add the library to the classpath.

The Wes library depends on the jme3-utilities-heart library,
which in turn depends on the standard jme3-core library.

#### For Gradle projects

For projects built using Maven or Gradle, it is sufficient to specify the
dependency on the Wes library.  The build tools should automatically
resolve the remaining dependencies automatically.

Because Wes is not on JCenter yet, you have to explicitly specify the
repository location:

    repositories {
        maven { url 'https://dl.bintray.com/stephengold/jme3utilities' }
        jcenter()
    }
    dependencies {
        compile 'jme3utilities:Wes:0.3.8'
    }

#### For Ant projects

For project built using Ant, download the Wes and jme3-utilities-heart
libraries from GitHub:

   + https://github.com/stephengold/jme3-utilities/releases/tag/sky-0.9.15
   + https://github.com/stephengold/jme3-utilities/releases/tag/heart-2.18.0

You'll want both class JARs
and probably the `-sources` and `-javadoc` JARs as well.

Open the project's properties in the IDE (JME 3.2 SDK or NetBeans 8.2):

 1. Right-click on the project (not its assets) in the "Projects" window.
 2. Select "Properties to open the "Project Properties" dialog.
 3. Under "Categories:" select "Libraries".
 4. Click on the "Compile" tab.
 5. Add the `jme3-utilities-heart` class JAR:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the "jme3-utilities" project folder.
    + Open the "heart" sub-project folder.
    + Navigate to the "build/libs" folder.
    + Select the "jme3-utilities-heart-2.18.0.jar" file.
    + Click on the "Open" button.
 6. (optional) Add JARs for javadoc and sources:
    + Click on the "Edit" button.
    + Click on the "Browse..." button to the right of "Javadoc:"
    + Select the "jme3-utilities-heart-2.18.0-javadoc.jar" file.
    + Click on the "Open" button.
    + Click on the "Browse..." button to the right of "Sources:"
    + Select the "jme3-utilities-heart-2.18.0-sources.jar" file.
    + Click on the "Open" button again.
    + Click on the "OK" button to close the "Edit Jar Reference" dialog.
 7. Similarly, add the `Wes` JAR(s).
 8. Click on the "OK" button to exit the "Project Properties" dialog.

[bsd3]: https://opensource.org/licenses/BSD-3-Clause "3-Clause BSD License"
[bvhretarget]: https://github.com/Nehon/bvhretarget "Bvhretarget Project"
[chrome]: https://www.google.com/chrome "Chrome"
[elements]: http://www.adobe.com/products/photoshop-elements.html "Photoshop Elements"
[findbugs]: http://findbugs.sourceforge.net "FindBugs Project"
[git]: https://git-scm.com "Git"
[github]: https://github.com "GitHub"
[gradle]: https://gradle.org "Gradle Project"
[jfrog]: https://www.jfrog.com "JFrog"
[jme]: http://jmonkeyengine.org  "jMonkeyEngine Project"
[makehuman]: http://www.makehumancommunity.org/ "MakeHuman Community"
[markdown]: https://daringfireball.net/projects/markdown "Markdown Project"
[maud]: https://github.com/stephengold/Maud "Maud editor"
[netbeans]: https://netbeans.org "NetBeans Project"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-Utilities Project"
[winmerge]: http://winmerge.org "WinMerge Project"
[wes]: https://github.com/stephengold/Wes "Wes Project"

<a name="acks"/>

## Acknowledgments

Like most projects, the Wes Project builds on the work of many who
have gone before.  I therefore acknowledge the following
artists and software developers:

+ Rémy Bouquet (aka "nehon") for creating the [Bvhretarget Project][bvhretarget]
  (on which `BoneMapping` and `SkeletonMapping` are based)
  and also for many helpful insights
+ Paul Speed, for helpful insights
+ the creators of (and contributors to) the following software:
    + the [FindBugs][] source-code analyzer
    + the [Git][] revision-control system and GitK commit viewer
    + the [Google Chrome web browser][chrome]
    + the [Gradle][] build tool
    + the Java compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + LWJGL, the Lightweight Java Game Library
    + the [MakeHuman][] Community
    + the [Markdown][] document conversion tool
    + Microsoft Windows
    + the [NetBeans][] integrated development environment
    + the PMD source-code analyzer
    + the [WinMerge][] differencing and merging tool

I am grateful to [JFrog][] and [Github][] for providing free hosting for the
Wes Project and many other open-source projects.

I'm also grateful to my dear Holly, for keeping me sane.

If I've misattributed anything or left anyone out, please let me know so I can
correct the situation: sgold@sonic.net