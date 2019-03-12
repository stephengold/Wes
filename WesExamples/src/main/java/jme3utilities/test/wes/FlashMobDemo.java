/*
 Copyright (c) 2019, Stephen Gold
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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.StatsAppState;
import com.jme3.asset.AssetKey;
import com.jme3.audio.openal.ALAudioRenderer;
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
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.wes.TrackEdit;
import jme3utilities.wes.TweenTransforms;

/**
 * An animation retargeting demo.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class FlashMobDemo extends ActionApplication {
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

    /*
     * Sinbad's "Dance" animation
     */
    private Animation sinbadAnimation;
    /*
     * list of animation channels
     */
    final private List<AnimChannel> allChannels = new ArrayList<>(3);
    /*
     * list of skeleton visualizers
     */
    final private List<SkeletonVisualizer> visualizers = new ArrayList<>(3);
    /*
     * loaded Jaime model
     */
    private Node jaime;
    /*
     * loaded MhGame model
     */
    private Node mhGame;
    /*
     * Sinbad's Skeleton
     */
    private Skeleton sinbadSkeleton;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        FlashMobDemo application = new FlashMobDemo();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);

        settings.setGammaCorrection(true);
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        Logger.getLogger(MaterialLoader.class.getName()).setLevel(Level.SEVERE);
        Logger.getLogger(MeshLoader.class.getName()).setLevel(Level.SEVERE);

        configureCamera();
        ColorRGBA bgColor = new ColorRGBA(0.2f, 0.2f, 1f, 1f);
        viewPort.setBackgroundColor(bgColor);
        addLighting();

        stateManager.getState(StatsAppState.class).toggleStats();
        addBox();

        addJaime();
        addMhGame();
        addSinbad();
        /*
         * Configure the skeleton visualizers.
         */
        for (SkeletonVisualizer sv : visualizers) {
            sv.setLineColor(ColorRGBA.Yellow); // TODO clean up visualization
            rootNode.addControl(sv);
        }

        /*
         * Load the Sinbad-to-Jaime skeleton map.
         */
        AssetKey<SkeletonMapping> s2jKey
                = new AssetKey<>("SkeletonMaps/SinbadToJaime.j3o");
        SkeletonMapping s2j = assetManager.loadAsset(s2jKey);
        /*
         * Retarget the "Dance" animation from Sinbad to Jaime.
         */
        AnimControl animControl = jaime.getControl(AnimControl.class);
        Skeleton skeleton = animControl.getSkeleton();
        TweenTransforms techniques = new TweenTransforms();
        Animation dance = TrackEdit.retargetAnimation(sinbadAnimation,
                sinbadSkeleton, skeleton, s2j, techniques, "Dance");
        animControl.addAnim(dance);

        /*
         * Load the Sinbad-to-MhGame skeleton map.
         */
        AssetKey<SkeletonMapping> s2mKey
                = new AssetKey<>("SkeletonMaps/SinbadToMhGame.j3o");
        SkeletonMapping s2m = assetManager.loadAsset(s2mKey);
        /*
         * Retarget the "Dance" animation from Sinbad to MhGame.
         */
        animControl = mhGame.getControl(AnimControl.class);
        skeleton = animControl.getSkeleton();
        dance = TrackEdit.retargetAnimation(sinbadAnimation,
                sinbadSkeleton, skeleton, s2m, techniques, "Dance");
        animControl.addAnim(dance);

        /*
         * Play the "Dance" animation on all channels.
         */
        for (AnimChannel animChannel : allChannels) {
            animChannel.setAnim("Dance");
        }
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("dump scenes", KeyInput.KEY_P);
        dim.bind("signal orbitLeft", KeyInput.KEY_LEFT);
        dim.bind("signal orbitRight", KeyInput.KEY_RIGHT);
        dim.bind("toggle pause", KeyInput.KEY_PERIOD);
        dim.bind("toggle skeleton", KeyInput.KEY_V);
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
                case "toggle pause":
                    togglePause();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
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
     * Add a Jaime model with Sinbad's "Dance" animation.
     */
    private void addJaime() {
        jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        rootNode.attachChild(jaime);

        List<Spatial> list
                = MySpatial.listSpatials(jaime, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        jaime.rotate(0f, FastMath.PI, 0f); // facing +Z
        setHeight(jaime, 2f);
        center(jaime);
        jaime.move(-1f, 0f, -1f); // behind Sinbad and to his right
        /*
         * Add an animation channel.
         */
        AnimControl animControl = jaime.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        allChannels.add(animChannel);
        /*
         * Add a skeleton visualizer.
         */
        SkeletonControl sc = jaime.getControl(SkeletonControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
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
     * Add an MhGame model.
     */
    private void addMhGame() {
        mhGame = (Node) assetManager.loadModel(
                "Models/MhGame/MhGame.mesh.xml");
        rootNode.attachChild(mhGame);

        List<Spatial> list
                = MySpatial.listSpatials(mhGame, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setHeight(mhGame, 2f);
        center(mhGame);
        mhGame.move(1f, 0f, -1f); // behind Sinbad and to his left
        /*
         * Add an animation channel.
         */
        AnimControl animControl = mhGame.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        allChannels.add(animChannel);
        /*
         * Add a skeleton visualizer.
         */
        SkeletonControl sc = mhGame.getControl(SkeletonControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        visualizers.add(sv);
    }

    /**
     * Add a Sinbad model.
     */
    private void addSinbad() {
        Node cgModel = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(cgModel);

        List<Spatial> list
                = MySpatial.listSpatials(cgModel, Spatial.class, null);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setHeight(cgModel, 2f);
        center(cgModel);
        cgModel.move(0f, 0f, 1f); // in front of the origin

        AnimControl animControl = cgModel.getControl(AnimControl.class);
        sinbadAnimation = animControl.getAnim("Dance");

        SkeletonControl sc = cgModel.getControl(SkeletonControl.class);
        sinbadSkeleton = sc.getSkeleton();
        /*
         * Add an animation channel.
         */
        AnimChannel animChannel = animControl.createChannel();
        allChannels.add(animChannel);
        /*
         * Add a skeleton visualizer.
         */
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        visualizers.add(sv);
    }

    /**
     * Translate a model's center so that the model rests on the X-Z plane, and
     * its center lies on the Y axis.
     */
    private void center(Spatial model) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1], null);
        Vector3f offset = new Vector3f(center.x, minMax[0].y, center.z);

        Vector3f location = model.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(model, location);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);

        cam.setLocation(new Vector3f(-3.5f, 2.3f, 2.1f));
        cam.setRotation(new Quaternion(0.087f, 0.876f, -0.17f, 0.44f));

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
     * Scale the specified model uniformly so that it has the specified height.
     *
     * @param model (not null, modified)
     * @param height (in world units)
     */
    private void setHeight(Spatial model, float height) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        float oldHeight = minMax[1].y - minMax[0].y;

        model.scale(height / oldHeight);
    }

    /**
     * Toggle all animations: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle the skeleton visualizers on/off.
     */
    private void toggleSkeleton() {
        for (SkeletonVisualizer sv : visualizers) {
            boolean enabled = sv.isEnabled();
            sv.setEnabled(!enabled);
        }
    }
}
