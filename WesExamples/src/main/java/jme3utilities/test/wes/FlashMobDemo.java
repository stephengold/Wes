/*
 Copyright (c) 2019-2022, Stephen Gold
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
package jme3utilities.test.wes;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.StatsAppState;
import com.jme3.asset.AssetKey;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InfluenceUtil;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.ui.AbstractDemo;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.wes.AnimationEdit;

/**
 * Demonstrate animation retargeting.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class FlashMobDemo extends AbstractDemo {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(FlashMobDemo.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = FlashMobDemo.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * Sinbad's "Dance" animation
     */
    private AnimClip sinbadClip;
    /**
     * Sinbad's Armature
     */
    private Armature sinbadArmature;
    /**
     * true once {@link #startup1()} has completed, until then false
     */
    private boolean didStartup1 = false;
    /**
     * list of animation channels
     */
    final private List<AnimChannel> allChannels = new ArrayList<>(2);
    /**
     * list of composers
     */
    final private List<AnimComposer> composers = new ArrayList<>(3);
    /**
     * list of skeleton visualizers
     */
    final private List<SkeletonVisualizer> visualizers = new ArrayList<>(5);
    /**
     * root node of the loaded Jaime model
     */
    private Node jaime;
    /**
     * root node of the loaded MhGame model
     */
    private Node mhGame;
    /**
     * root node of the loaded Oto model
     */
    private Node oto;
    /**
     * root node of the loaded Puppet model
     */
    private Node puppet;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the FlashMobDemo application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        FlashMobDemo application = new FlashMobDemo();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setAudioRenderer(null);
        settings.setGammaCorrection(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setSamples(4); // anti-aliasing
        settings.setTitle(applicationName); // Customize the window's title bar.
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // AbstractDemo methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        Logger.getLogger(MaterialLoader.class.getName()).setLevel(Level.SEVERE);
        Logger.getLogger(MeshLoader.class.getName()).setLevel(Level.SEVERE);

        configureCamera();

        // Set the background to light blue.
        ColorRGBA backgroundColor = new ColorRGBA(0.2f, 0.2f, 1f, 1f);
        viewPort.setBackgroundColor(backgroundColor);

        addLighting();
        addBox();
        addJaime();
        addMhGame();
        addOto();
        addPuppet();
        addSinbad();

        // Configure the skeleton visualizers.
        for (SkeletonVisualizer sv : visualizers) {
            sv.setLineColor(ColorRGBA.Yellow);
            rootNode.addControl(sv);
        }

        // Load the Sinbad-to-Jaime skeleton map.
        AssetKey<SkeletonMapping> s2jKey
                = new AssetKey<>("SkeletonMaps/SinbadToJaime.j3o");
        SkeletonMapping s2j = assetManager.loadAsset(s2jKey);

        // Retarget the "Dance" animation from Sinbad to Jaime.
        AnimControl animControl = jaime.getControl(AnimControl.class);
        Skeleton skeleton = animControl.getSkeleton();
        Animation dance = AnimationEdit.retargetAnimation(sinbadClip,
                sinbadArmature, skeleton, s2j, "Dance");
        animControl.addAnim(dance);

        // Load the Sinbad-to-MhGame skeleton map.
        AssetKey<SkeletonMapping> s2mKey
                = new AssetKey<>("SkeletonMaps/SinbadToMhGame.j3o");
        SkeletonMapping s2m = assetManager.loadAsset(s2mKey);

        // Retarget the "Dance" animation from Sinbad to MhGame.
        SkinningControl skinningControl
                = mhGame.getControl(SkinningControl.class);
        Armature armature = skinningControl.getArmature();
        AnimClip danceClip = AnimationEdit.retargetAnimation(sinbadClip,
                sinbadArmature, armature, s2m, "Dance");
        AnimComposer composer = mhGame.getControl(AnimComposer.class);
        composer.addAnimClip(danceClip);

        // Load the Sinbad-to-Oto skeleton map.
        AssetKey<SkeletonMapping> s2oKey
                = new AssetKey<>("SkeletonMaps/SinbadToOto.j3o");
        SkeletonMapping s2o = assetManager.loadAsset(s2oKey);

        // Retarget the "Dance" animation from Sinbad to Oto.
        skinningControl = oto.getControl(SkinningControl.class);
        armature = skinningControl.getArmature();
        danceClip = AnimationEdit.retargetAnimation(sinbadClip, sinbadArmature,
                armature, s2o, "Dance");
        composer = oto.getControl(AnimComposer.class);
        composer.addAnimClip(danceClip);

        // Load the Puppet-to-Sinbad skeleton map.
        AssetKey<SkeletonMapping> p2sKey
                = new AssetKey<>("SkeletonMaps/PuppetToSinbad.j3o");
        SkeletonMapping p2s = assetManager.loadAsset(p2sKey);

        // Invert the skeleton map.
        SkeletonMapping s2p = p2s.inverse();

        // Retarget the "Dance" animation from Sinbad to Puppet.
        animControl = puppet.getControl(AnimControl.class);
        skeleton = animControl.getSkeleton();
        dance = AnimationEdit.retargetAnimation(sinbadClip, sinbadArmature,
                skeleton, s2p, "Dance");
        animControl.addAnim(dance);

        // Start with skeleton and the render-statistics overlay hidden.
        stateManager.getState(StatsAppState.class).toggleStats();
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("dump scenes", KeyInput.KEY_P);
        dim.bindSignal(CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bindSignal(CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bindSignal("orbitLeft", KeyInput.KEY_LEFT);
        dim.bindSignal("orbitRight", KeyInput.KEY_RIGHT);
        dim.bind(asToggleHelp, KeyInput.KEY_H);
        dim.bind(asTogglePause, KeyInput.KEY_PAUSE, KeyInput.KEY_PERIOD);
        dim.bind("toggle skeleton", KeyInput.KEY_V);

        // The help node can't be created until all hotkeys are bound.
        addHelp();
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case "dump scenes":
                    dumpScenes();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        if (!didStartup1) {
            startup1();
            didStartup1 = true;
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add a large static box to serve as a platform.
     */
    private void addBox() {
        float halfExtent = 50f; // mesh units
        Mesh mesh = new Box(halfExtent, halfExtent, halfExtent);
        Geometry geometry = new Geometry("box", mesh);
        rootNode.attachChild(geometry);

        geometry.move(0f, -halfExtent, 0f);
        ColorRGBA color = new ColorRGBA(0f, 0.2f, 0f, 1f);
        Material material = MyAsset.createShadedMaterial(assetManager, color);
        geometry.setMaterial(material);
        geometry.setShadowMode(RenderQueue.ShadowMode.Receive);
    }

    /**
     * Attach a Node to display hotkey help.
     */
    private void addHelp() {
        float x = 10f;
        float y = cam.getHeight() - 10f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle rectangle = new Rectangle(x, y, width, height);

        attachHelpNode(rectangle);
    }

    /**
     * Attach a Jaime model (with visualizer) to the root node.
     */
    private void addJaime() {
        jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        rootNode.attachChild(jaime);

        List<Spatial> list = MySpatial.listSpatials(jaime);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        jaime.rotate(0f, FastMath.PI, 0f); // facing +Z
        setCgmHeight(jaime, 2f);
        centerCgm(jaime);
        jaime.move(-2f, 0f, 0f); // behind Sinbad and to his right
        /*
         * Add an animation channel.
         */
        AnimControl animControl = jaime.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        allChannels.add(animChannel);

        // Add a skeleton visualizer.
        SkeletonControl sc = jaime.getControl(SkeletonControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        InfluenceUtil.hideNonInfluencers(sv, sc);
        visualizers.add(sv);
    }

    /**
     * Add lighting and shadows to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -2f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.5f);
        viewPort.addProcessor(dlsr);
    }

    /**
     * Attach an MhGame model to the root node.
     */
    private void addMhGame() {
        mhGame = (Node) assetManager.loadModel("Models/MhGame/MhGame.mesh.xml");
        rootNode.attachChild(mhGame);

        List<Spatial> list = MySpatial.listSpatials(mhGame);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setCgmHeight(mhGame, 2f);
        centerCgm(mhGame);
        mhGame.move(2f, 0f, -1f); // behind Sinbad and to his left
        /*
         * Add composer to the master list.
         */
        AnimComposer composer = mhGame.getControl(AnimComposer.class);
        composers.add(composer);
        /*
         * Add a skeleton visualizer.
         */
        SkinningControl sc = mhGame.getControl(SkinningControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        visualizers.add(sv);
    }

    /**
     * Attach an Oto model to the root node.
     */
    private void addOto() {
        oto = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        rootNode.attachChild(oto);

        List<Spatial> list = MySpatial.listSpatials(oto);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setCgmHeight(oto, 2f);
        centerCgm(oto);
        oto.move(0f, 0f, -1f); // directly behind Sinbad
        /*
         * Add composer to the master list.
         */
        AnimComposer composer = oto.getControl(AnimComposer.class);
        composers.add(composer);
        /*
         * Add a skeleton visualizer.
         */
        SkinningControl sc = oto.getControl(SkinningControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        visualizers.add(sv);
    }

    /**
     * Attach a Puppet model to the root node.
     */
    private void addPuppet() {
        Node loadedNode = (Node) assetManager.loadModel("Models/Puppet/Puppet.j3o");
        puppet = (Node) loadedNode.getChild(0);
        rootNode.attachChild(puppet);

        List<Spatial> list = MySpatial.listSpatials(puppet);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setCgmHeight(puppet, 2f);
        centerCgm(puppet);
        puppet.move(2f, 0f, 1f);
        /*
         * Add an animation channel.
         */
        AnimControl animControl = puppet.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        allChannels.add(animChannel);
        /*
         * Add a skeleton visualizer.
         */
        SkeletonControl sc = puppet.getControl(SkeletonControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        InfluenceUtil.hideNonInfluencers(sv, sc);
        visualizers.add(sv);
    }

    /**
     * Attach a Sinbad model to the root node.
     */
    private void addSinbad() {
        Node cgModel = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(cgModel);

        List<Spatial> list = MySpatial.listSpatials(cgModel);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setCgmHeight(cgModel, 2f);
        centerCgm(cgModel);
        cgModel.move(0f, 0f, 1f); // in front of the origin

        AnimComposer composer = cgModel.getControl(AnimComposer.class);
        sinbadClip = composer.getAnimClip("Dance");

        SkinningControl sc = cgModel.getControl(SkinningControl.class);
        sinbadArmature = sc.getArmature();
        /*
         * Add composer to the master list.
         */
        composers.add(composer);
        /*
         * Add a skeleton visualizer.
         */
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        visualizers.add(sv);
    }

    /**
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);

        cam.setLocation(new Vector3f(2.85f, 3.46f, 5.29f));
        cam.setRotation(new Quaternion(-0.054f, 0.946336f, -0.2308f, -0.2197f));

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Process a "dump scenes" action.
     */
    private void dumpScenes() {
        Dumper dumper = new Dumper();
        //dumper.setDumpBucket(true);
        //dumper.setDumpCull(true);
        //dumper.setDumpMatParam(true);
        //dumper.setDumpOverride(true);
        //dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        //dumper.setDumpUser(true);
        dumper.dump(renderManager);
    }

    /**
     * Initialization performed during the first invocation of
     * {@link #simpleUpdate(float)}.
     */
    private void startup1() {
        /*
         * Play the "Dance" animation on all channels.
         */
        for (AnimChannel animChannel : allChannels) {
            animChannel.setAnim("Dance");
            animChannel.setTime(0f);
        }
        /*
         * Play the "Dance" clip on all composers.
         */
        for (AnimComposer poser : composers) {
            poser.setCurrentAction("Dance");
            poser.setTime(AnimComposer.DEFAULT_LAYER, 0f);
        }
    }

    /**
     * Toggle all the skeleton visualizers on/off.
     */
    private void toggleSkeleton() {
        for (SkeletonVisualizer sv : visualizers) {
            boolean enabled = sv.isEnabled();
            sv.setEnabled(!enabled);
        }
    }
}
