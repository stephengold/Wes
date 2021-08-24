/*
 Copyright (c) 2020, Stephen Gold
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
import com.jme3.anim.SkinningControl;
import com.jme3.app.StatsAppState;
import com.jme3.audio.openal.ALAudioRenderer;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.MaterialLoader;
import com.jme3.scene.plugins.ogre.MeshLoader;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.MySpatial;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.HelpUtils;
import jme3utilities.ui.InputMode;
import jme3utilities.wes.AnimationEdit;
import jme3utilities.wes.TweenTransforms;

/**
 * An ActionApplication to demonstrate trimming and stretching an AnimClip.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TrimAnimation extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TrimAnimation.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TrimAnimation.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * Node for displaying hotkey help in the GUI scene
     */
    private Node helpNode;
    /**
     * Node for displaying "toggle help: H" in the GUI scene
     */
    private Node minHelpNode;
    /**
     * root node of the loaded Sinbad model
     */
    private Node sinbadModelRoot;
    /**
     * debug visualizer for Sinbad's skeleton
     */
    private SkeletonVisualizer sv;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TrimAnimation application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        Logger.getLogger(ALAudioRenderer.class.getName())
                .setLevel(Level.SEVERE);

        TrimAnimation application = new TrimAnimation();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);

        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        settings.setAudioRenderer(null);
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
        addSinbad();
        /*
         * Create a trimmed version of the "SliceHorizontal" clip.
         */
        AnimComposer composer = sinbadModelRoot.getControl(AnimComposer.class);
        AnimClip slice = composer.getAnimClip("SliceHorizontal");
        float startTime = 0.308f;
        float endTime = 0.344f;
        TweenTransforms techniques = new TweenTransforms();
        AnimClip trimmed = AnimationEdit.extractAnimation(slice, startTime,
                endTime, techniques, "");
        /*
         * Create a version of the trimmed clip that runs 15x slower/longer.
         */
        float newDuration = 15f * (endTime - startTime);
        AnimClip warn = AnimationEdit.setDuration(trimmed, newDuration, "warn");
        composer.addAnimClip(warn);
        /*
         * Play the resulting clip repeatedly.
         */
        composer.setCurrentAction("warn");
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
        dim.bind("toggle help", KeyInput.KEY_H);
        dim.bind("toggle pause", KeyInput.KEY_PAUSE, KeyInput.KEY_PERIOD);
        dim.bind("toggle skeleton", KeyInput.KEY_V);
        /*
         * The help node can't be created until all hotkeys are bound.
         */
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
                case "toggle help":
                    toggleHelp();
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
     * Attach a Node to display hotkey help.
     */
    private void addHelp() {
        float x = 10f;
        float y = cam.getHeight() - 40f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle rectangle = new Rectangle(x, y, width, height);

        attachHelpNode(rectangle);
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
     * Attach a Sinbad model to the root node.
     */
    private void addSinbad() {
        sinbadModelRoot = (Node) assetManager.loadModel(
                "Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(sinbadModelRoot);

        List<Spatial> list = MySpatial.listSpatials(sinbadModelRoot);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setHeight(sinbadModelRoot, 2f);
        center(sinbadModelRoot);
        /*
         * Add a skeleton visualizer.
         */
        SkinningControl sc = sinbadModelRoot.getControl(SkinningControl.class);
        sv = new SkeletonVisualizer(assetManager, sc);
        rootNode.addControl(sv);
        sv.setLineColor(ColorRGBA.Yellow);
    }

    /**
     * Generate full and minimal versions of the hotkey help. Attach the minimal
     * one to the GUI scene.
     *
     * @param bounds the desired screen coordinates (not null, unaffected)
     */
    private void attachHelpNode(Rectangle bounds) {
        InputMode inputMode = getDefaultInputMode();
        float extraSpace = 20f;
        helpNode = HelpUtils.buildNode(inputMode, bounds, guiFont, extraSpace);
        helpNode.move(0f, 0f, 1f); // move (slightly) to the front

        InputMode dummyMode = new InputMode("dummy") {
            @Override
            protected void defaultBindings() {
            }

            @Override
            public void onAction(String s, boolean b, float f) {
            }
        };
        dummyMode.bind("toggle help", KeyInput.KEY_H);

        float width = 100f; // in pixels
        float height = bounds.height;
        float x = bounds.x + bounds.width - width;
        float y = bounds.y;
        Rectangle dummyBounds = new Rectangle(x, y, width, height);

        minHelpNode = HelpUtils.buildNode(dummyMode, dummyBounds, guiFont, 0f);
        guiNode.attachChild(minHelpNode);
    }

    /**
     * Translate a model's center so that the model rests on the X-Z plane, and
     * its center lies on the Y axis.
     */
    private static void center(Spatial model) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1], null);
        Vector3f offset = new Vector3f(center.x, minMax[0].y, center.z);

        Vector3f location = model.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(model, location);
    }

    /**
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);

        cam.setLocation(new Vector3f(0.42f, 1.67f, 3.47f));
        cam.setRotation(new Quaternion(-0.009f, 0.990377f, -0.1142f, -0.0777f));

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
    private static void setHeight(Spatial model, float height) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        float oldHeight = minMax[1].y - minMax[0].y;

        model.scale(height / oldHeight);
    }

    /**
     * Toggle between the full help node and the minimal one.
     */
    private void toggleHelp() {
        if (helpNode.getParent() == null) {
            minHelpNode.removeFromParent();
            guiNode.attachChild(helpNode);
        } else {
            helpNode.removeFromParent();
            guiNode.attachChild(minHelpNode);
        }
    }

    /**
     * Toggle all animations: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }
}
