/*
 Copyright (c) 2022, Stephen Gold
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
import com.jme3.font.BitmapText;
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
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.Collection;
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
 * Demonstrate conversion of a traveling animation into an in-place animation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ConvertToInPlace extends AbstractDemo {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(ConvertToInPlace.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = ConvertToInPlace.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * animation control of the modified C-G model
     */
    private static AnimComposer composer;
    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private static BitmapText statusText;
    /**
     * collection of all skeleton visualizers in the scene
     */
    final private static Collection<SkeletonVisualizer> visualizers
            = new ArrayList<>(2);
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the ConvertToInPlace application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        ConvertToInPlace application = new ConvertToInPlace();
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
        configureCamera();

        // Set the background to light blue.
        ColorRGBA backgroundColor = new ColorRGBA(0.2f, 0.2f, 1f, 1f);
        viewPort.setBackgroundColor(backgroundColor);

        addLighting();
        addBox();
        attachWorldAxes(1f);

        // the unmodified animation in white, the modified one in yellow
        composer = addFighter(0.7f, -0.7f, ColorRGBA.Yellow);
        AnimComposer control = addFighter(-0.7f, 0.7f, ColorRGBA.White);

        // Attach status text to the GUI.
        statusText = new BitmapText(guiFont);
        statusText.setLocalTranslation(10f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);
        /*
         * Create an in-place cyclic animation
         * based on the "hurricane_kick" clip
         * and play it at 1/4 speed.
         */
        String originalName = "hurricane_kick";
        AnimClip originalClip = composer.getAnimClip(originalName);
        String convertedName = "hurricane_kick_in_place";
        AnimClip convertedClip
                = AnimationEdit.convertToInPlace(originalClip, convertedName);
        composer.addAnimClip(convertedClip);
        composer.setCurrentAction(convertedName);
        composer.setGlobalSpeed(0.25f);

        // Play the original animation (also at 1/4 speed) for comparison.
        control.setCurrentAction(originalName);
        control.setGlobalSpeed(0.25f);
        /*
         * Start with skeleton visualization enabled
         * and the render-statistics overlay hidden.
         */
        stateManager.getState(StatsAppState.class).toggleStats();
        toggleSkeleton();
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

        // Update the displayed status.
        double animationMsec = 1000.0 * composer.getTime();
        String text = String.format("animation time = %.0f ms", animationMsec);
        statusText.setText(text);
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
     * Attach a ninja-fighter model (with visualizer) to the root node and
     * return its main AnimComposer.
     *
     * @param x the desired initial X coordinate
     * @param z the desired initial Z coordinate
     * @param svColor the desired color for the SkeletonVisualizer (not null)
     */
    private AnimComposer addFighter(float x, float z, ColorRGBA svColor) {
        /*
         * Temporarily suppress warnings (from the glTF loader)
         * regarding animations that use non-linear interpolation.
         */
        Logger loaderLogger
                = Logger.getLogger("com.jme3.scene.plugins.gltf.GltfLoader");
        Level savedLevel = loaderLogger.getLevel();
        loaderLogger.setLevel(Level.SEVERE);

        String assetPath = "Models/ninja-fighter/ninja.gltf";
        Node scene = (Node) assetManager.loadModel(assetPath);
        loaderLogger.setLevel(savedLevel);

        Spatial ninja = scene.getChild("Ninja");
        rootNode.attachChild(ninja);

        List<Spatial> list = MySpatial.listSpatials(ninja);
        for (Spatial spatial : list) {
            spatial.setCullHint(Spatial.CullHint.Never);
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        setCgmHeight(ninja, 2f);
        centerCgm(ninja);
        ninja.move(x, 0f, z);

        // Add a skeleton visualizer.
        SkinningControl sc = ninja.getControl(SkinningControl.class);
        SkeletonVisualizer sv = new SkeletonVisualizer(assetManager, sc);
        InfluenceUtil.hideNonInfluencers(sv, sc);
        sv.setLineColor(svColor);
        ninja.addControl(sv);
        visualizers.add(sv);

        AnimComposer result = ninja.getControl(AnimComposer.class);

        return result;
    }

    /**
     * Attach a Node to display hotkey help.
     */
    private void addHelp() {
        float x = 10f;
        float y = cam.getHeight() - 30f;
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
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(2f);

        cam.setLocation(new Vector3f(6.7f, 2.9f, 4.4f));
        cam.setRotation(new Quaternion(0.0508f, -0.82009f, 0.0731f, 0.56527f));

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
        dumper.setDumpCull(true);
        //dumper.setDumpMatParam(true);
        //dumper.setDumpOverride(true);
        dumper.setDumpShadow(true);
        dumper.setDumpTransform(true);
        //dumper.setDumpUser(true);
        dumper.dump(renderManager);
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
