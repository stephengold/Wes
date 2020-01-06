<img height="150" src="https://i.imgur.com/1iPoJpl.png">

The [Wes Project][wes] is about editing
animations used in the [jMonkeyEngine Game Engine][jme].

It contains 2 sub-projects:

 1. WesLibrary: the Wes runtime library (in Java)
 2. WesExamples: demos, examples, and test software (in Java)

Java source code is provided under
[a FreeBSD license](https://github.com/stephengold/Wes/blob/master/LICENSE).

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
 + [History](#history)
 + [How to build Wes from source](#build)
 + [How to add Wes to an existing project](#add)
 + [An overview of the demo applications](#demos)
 + [Acknowledgments](#acks)

<a name="downloads"/>

## Downloads

Newer releases (since v0.3.9) can be downloaded from
[GitHub](https://github.com/stephengold/Wes/releases).

Older releases (v0.2.1 through v0.3.8) can be downloaded from
[the Jme3-utilities Project](https://github.com/stephengold/jme3-utilities/releases).

Maven artifacts are available from
[JFrog Bintray](https://bintray.com/stephengold/jme3utilities/Wes).

[Jump to table of contents](#toc)

<a name="conventions"/>

## Conventions

Package names begin with
`jme3utilities.` (if Stephen Gold holds the copyright) or
`com.jme3.` (if the jMonkeyEngine Project holds the copyright).

Both the source code and the pre-built libraries are compatible with JDK 7.

[Jump to table of contents](#toc)

<a name="history"/>

## History

Much of the code was developed for the [Maud editor][maud].

The retargeting code was originally developed by Rémy Bouquet (aka "nehon")
for his [Bvhretarget Project][bvhretarget].

From September 2017 to January 2019, Wes was a sub-project of
[the Jme3-utilities Project][utilities].

Since January 2019, the Wes Project has been a separate project at
[GitHub][wes].

The evolution of Wes is chronicled in
[its release notes](https://github.com/stephengold/Wes/blob/master/WesLibrary/release-notes.md).

[Jump to table of contents](#toc)

<a name="build"/>

## How to build Wes from source

Wes currently targets Version 3.2.4 of jMonkeyEngine.
You are welcome to use the Engine without installing
its Integrated Development Environment (IDE),
but I use the IDE, so I tend to assume you will too.

### IDE setup

If you already have the IDE installed, skip to step 6.

The hardware and software requirements of the IDE are documented at
[the JME wiki](https://jmonkeyengine.github.io/wiki/jme3/requirements.html).

 1. Download a jMonkeyEngine 3.2 Software Development Kit (SDK) from
    [GitHub](https://github.com/jMonkeyEngine/sdk/releases).
 2. Install the SDK, which includes:
    + the engine itself,
    + an IDE based on [NetBeans][],
    + various IDE plugins, and
    + the [Blender 3D][blender] application.
 3. Open the IDE.
 4. The first time you open the IDE, it prompts you to
    specify a folder for storing projects:
    + Fill in the "Folder name" text box.
    + Click on the "Set Project Folder" button.
 5. The first time you open the IDE, you should update
    all the pre-installed plugins:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Update" button to open the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + After the plugins have downloaded, click "Finish".
    + The IDE will restart.
 6. In order to open the Wes Project in the IDE (or NetBeans),
    you will need to install the `Gradle Support` plugin:
    + Menu bar -> "Tools" -> "Plugins" to open the "Plugins" dialog.
    + Click on the "Available Plugins" tab.
    + Check the box next to "Gradle Support" in the "Gradle" category.
     If this plugin isn't shown in the IDE's "Plugins" tool,
     you can download it from
     [GitHub](https://github.com/kelemen/netbeans-gradle-project/releases).
    + Click on the "Install" button to open the "Plugin Installer" wizard.
    + Click on the "Next >" button.
    + Check the box next to
     "I accept the terms in all the license agreements."
    + Click on the "Install" button.
    + When the "Verify Certificate" dialog appears,
     click on the "Continue" button.
    + Click on the "Finish" button.
    + The IDE will restart.

### Source files

Clone the Wes repository using Git:

 1. Open the "Clone Repository" wizard in the IDE:
     + Menu bar -> "Team" -> "Git" -> "Clone..." or
     + Menu bar -> "Team" -> "Remote" -> "Clone..."
 2. For "Repository URL:" specify
    `https://github.com/stephengold/Wes.git`
 3. Clear the "User:" and "Password:" text boxes.
 4. For "Clone into:" specify a writable folder (on a local filesystem)
    that doesn't already contain "Wes".
 5. Click on the "Next >" button.
 6. Make sure the "master" remote branch is checked.
 7. Click on the "Next >" button again.
 8. Make sure the Checkout Branch is set to "master".
 9. Make sure the "Scan for NetBeans Projects after Clone" box is checked.
10. Click on the "Finish" button.
11. When the "Clone Completed" dialog appears, click on the "Open Project..."
    button.
12. Expand the root project node to reveal the 2 sub-projects.
13. Select both sub-projects using control-click, then click on the
    "Open" button.

### Build the project

 1. In the "Projects" window of the IDE,
    right-click on the "WesExamples" sub-project to select it.
 2. Select "Build".

### How to build Wes without an IDE

 1. Install build software:
   + a Java Development Kit and
   + [Gradle]
 2. Download and extract the source code from GitHub:
   + using Git:
     + `git clone https://github.com/stephengold/Wes.git`
     + `cd Wes`
     + `git checkout 0.4.7for32`
   + using a web browser:
     + browse to [https://github.com/stephengold/Wes/releases/latest](https://github.com/stephengold/Wes/releases/latest)
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

After a successful build, new jars will be found in `WesLibrary/build/libs`.

You can also install the library artifact to your local Maven cache:
 + using Bash:  `./gradlew :WesLibrary:publishToMavenLocal`
 + using Windows Command Prompt:  `.\gradlew :WesLibrary:publishToMavenLocal`

[Jump to table of contents](#toc)

<a name="add"/>

## How to add Wes to an existing project

To add Wes to an existing JME3 project, simply add the library to the classpath.

The Wes library depends on the jme3-utilities-heart library,
which in turn depends on the standard jme3-core library.

#### For Gradle projects

For projects built using Maven or Gradle, it is sufficient to specify the
dependency on the Wes library.  The build tools should automatically
resolve the remaining dependencies automatically.

Because Wes is not on JCenter,
you must explicitly specify its repository URL:

    repositories {
        maven { url 'https://dl.bintray.com/stephengold/jme3utilities' }
        jcenter()
    }
    dependencies {
        compile 'jme3utilities:Wes:0.4.7for32'
    }

#### For Ant projects

For project built using [Ant][], download the Wes and jme3-utilities-heart
libraries from GitHub:

   + https://github.com/stephengold/Wes/releases/tag/0.4.7for32
   + https://github.com/stephengold/jme3-utilities/releases/tag/heart-4.3.0for32

You'll want both class jars
and probably the `-sources` and `-javadoc` jars as well.

Open the project's properties in the IDE (JME 3.2 SDK or NetBeans 8.2):

 1. Right-click on the project (not its assets) in the "Projects" window.
 2. Select "Properties to open the "Project Properties" dialog.
 3. Under "Categories:" select "Libraries".
 4. Click on the "Compile" tab.
 5. Add the `jme3-utilities-heart` class jar:
    + Click on the "Add JAR/Folder" button.
    + Navigate to the "jme3-utilities" project folder.
    + Open the "heart" sub-project folder.
    + Navigate to the "build/libs" folder.
    + Select the "jme3-utilities-heart-4.3.0for32.jar" file.
    + Click on the "Open" button.
 6. (optional) Add jars for javadoc and sources:
    + Click on the "Edit" button.
    + Click on the "Browse..." button to the right of "Javadoc:"
    + Select the "jme3-utilities-heart-4.3.0for32-javadoc.jar" file.
    + Click on the "Open" button.
    + Click on the "Browse..." button to the right of "Sources:"
    + Select the "jme3-utilities-heart-4.3.0for32-sources.jar" file.
    + Click on the "Open" button again.
    + Click on the "OK" button to close the "Edit Jar Reference" dialog.
 7. Similarly, add the `Wes` jar(s).
 8. Click on the "OK" button to exit the "Project Properties" dialog.

[ant]: https://ant.apache.org "Apache Ant Project"
[blender]: https://docs.blender.org "Blender Project"
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
[mint]: https://linuxmint.com "Linux Mint Project"
[netbeans]: https://netbeans.org "NetBeans Project"
[utilities]: https://github.com/stephengold/jme3-utilities "Jme3-utilities Project"
[vegdahl]: http://www.cessen.com "Nathan Vegdahl"
[winmerge]: http://winmerge.org "WinMerge Project"
[wes]: https://github.com/stephengold/Wes "Wes Project"

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
    + the [FindBugs][] source-code analyzer
    + the [Git][] revision-control system and GitK commit viewer
    + the [Google Chrome web browser][chrome]
    + the [Gradle][] build tool
    + the Java compiler, standard doclet, and runtime environment
    + [jMonkeyEngine][jme] and the jME3 Software Development Kit
    + the [Linux Mint][mint] operating system
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

[Jump to table of contents](#toc)