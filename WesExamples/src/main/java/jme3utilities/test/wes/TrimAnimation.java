/*
 Copyright (c) 2020-2022, Stephen Gold
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
import jme3utilities.MyString;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.ui.AbstractDemo;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.wes.AnimationEdit;
import jme3utilities.wes.TweenTransforms;

/**
 * Demonstrate trimming and stretching an AnimClip.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TrimAnimation extends AbstractDemo {
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
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        String title = applicationName + " " + MyString.join(arguments);
        TrimAnimation application = new TrimAnimation();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setAudioRenderer(null);
        settings.setRenderer(AppSettings.LWJGL_OPENGL32);
        settings.setSamples(4); // anti-aliasing
        settings.setTitle(title); // Customize the window's title bar.
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
        super.actionInitializeApplication();

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
     * Calculate screen bounds for a detailed help node. Meant to be overridden.
     *
     * @param viewPortWidth (in pixels, &gt;0)
     * @param viewPortHeight (in pixels, &gt;0)
     * @return a new instance
     */
    public Rectangle detailedHelpBounds(int viewPortWidth, int viewPortHeight) {
        /*
         * Position help nodes along the top of the viewport.
         */
        float margin = 10f; // in pixels
        float height = viewPortHeight - (2f * margin);
        float width = viewPortWidth - (2f * margin);
        float leftX = margin;
        float topY = margin + height;
        Rectangle result = new Rectangle(leftX, topY, width, height);

        return result;
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
        setCgmHeight(sinbadModelRoot, 2f);
        centerCgm(sinbadModelRoot);
        /*
         * Add a skeleton visualizer.
         */
        SkinningControl sc = sinbadModelRoot.getControl(SkinningControl.class);
        sv = new SkeletonVisualizer(assetManager, sc);
        rootNode.addControl(sv);
        sv.setLineColor(ColorRGBA.Yellow);
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
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }
}
