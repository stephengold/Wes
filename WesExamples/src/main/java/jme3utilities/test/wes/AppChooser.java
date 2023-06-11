/*
 Copyright (c) 2022-2023, Stephen Gold
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

import com.jme3.app.state.AppState;
import com.jme3.input.KeyInput;
import com.jme3.math.ColorRGBA;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import jme3utilities.ui.AcorusDemo;
import jme3utilities.ui.Combo;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.LocationPolicy;
import jme3utilities.ui.Overlay;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

/**
 * Choose an application from a list, then execute it.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final class AppChooser extends AcorusDemo {
    // *************************************************************************
    // constants and loggers

    /**
     * main classes of the apps
     */
    final private static Class<?>[] mainClasses = {
        ConvertToInPlace.class,
        FlashMobDemo.class,
        ReverseAnimation.class,
        TrimAnimation.class
    };
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AppChooser.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = AppChooser.class.getSimpleName();
    /**
     * action string to delete any persistent settings of the selected app
     */
    final private static String asDeleteSettings = "delete settings";
    /**
     * action string to execute the selected app
     */
    final private static String asExecute = "execute";
    /**
     * action string to select the next app
     */
    final private static String asNext = "down";
    /**
     * action string to select the previous app
     */
    final private static String asPrevious = "up";
    // *************************************************************************
    // fields

    /**
     * script to execute
     */
    private static File script;
    /**
     * index of the selected app in the {@code mainClasses} array
     */
    private static int selectedAppIndex = 0;
    /**
     * environment variables passed to the executor
     */
    private static Map<String, String> env = new TreeMap<>();
    /**
     * menu overlay, displayed in the upper-left corner of the GUI node
     */
    private static Overlay menuOverlay;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an AcorusDemo without any initial appstates.
     */
    private AppChooser() {
        super((AppState[]) null);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the AppChooser application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        String title = applicationName + " " + MyString.join(arguments);
        AppChooser application = new AppChooser();
        Heart.parseAppArgs(application, arguments);

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setAudioRenderer(null);
        settings.setResizable(true);
        settings.setSamples(4); // anti-aliasing
        settings.setTitle(title); // Customize the window's title bar.
        application.setSettings(settings);
        /*
         * The AWT settings dialog interferes with LWJGL v3
         * on macOS and Raspbian, so don't show it!
         */
        application.setShowSettings(false);
        application.start();
    }
    // *************************************************************************
    // AcorusDemo methods

    /**
     * Initialize this application.
     */
    @Override
    public void acorusInit() {
        script = findScriptToExecute();

        // environment variables that will be passed to the executor
        env.putAll(System.getenv());
        Platform platform = JmeSystem.getPlatform();
        if (platform.getOs() == Platform.Os.MacOS) {
            env.put("JAVA_OPTS", "-XstartOnFirstThread");
        }

        getHelpBuilder().setBackgroundColor(new ColorRGBA(0f, 0.05f, 0f, 1f));
        addMenuOverlay();
        super.acorusInit();
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bindSignal("ctrl", KeyInput.KEY_LCONTROL, KeyInput.KEY_RCONTROL);
        Combo ctrlX = new Combo(KeyInput.KEY_X, "ctrl", true);
        dim.bind(asDeleteSettings, ctrlX);
        dim.bind(asDeleteSettings, KeyInput.KEY_DELETE);

        dim.bind(asExecute,
                KeyInput.KEY_RETURN, KeyInput.KEY_NUMPAD6, KeyInput.KEY_RIGHT);
        dim.bind(asNext,
                KeyInput.KEY_S, KeyInput.KEY_NUMPAD2, KeyInput.KEY_DOWN);
        dim.bind(asPrevious,
                KeyInput.KEY_W, KeyInput.KEY_NUMPAD8, KeyInput.KEY_UP);
    }

    /**
     * Process an action that wasn't handled by the active InputMode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            switch (actionString) {
                case asDeleteSettings:
                    Class<?> mainClass = mainClasses[selectedAppIndex];
                    String appName = mainClass.getSimpleName();
                    Heart.deleteStoredSettings(appName);
                    updateMenuOverlay();
                    return;

                case asExecute:
                    Thread executor = new Thread("Executor") {
                        @Override
                        public void run() {
                            executeSelectedApp();
                        }
                    };
                    executor.start();
                    return;

                case asNext:
                    // Select the next app.
                    if (selectedAppIndex < mainClasses.length - 1) {
                        ++selectedAppIndex;
                        updateMenuOverlay();
                    }
                    return;

                case asPrevious:
                    // Select the previous app.
                    if (selectedAppIndex > 0) {
                        --selectedAppIndex;
                        updateMenuOverlay();
                    }
                    return;

                default:
            }
        }

        // The action has not been handled: forward it to the superclass.
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Update the GUI layout after the ViewPort gets resized.
     *
     * @param newWidth the new width of the ViewPort (in pixels, &gt;0)
     * @param newHeight the new height of the ViewPort (in pixels, &gt;0)
     */
    @Override
    public void onViewPortResize(int newWidth, int newHeight) {
        super.onViewPortResize(newWidth, newHeight);
        menuOverlay.onViewPortResize(newWidth, newHeight);
    }
    // *************************************************************************
    // private methods

    /**
     * Add an enabled menu overlay to the GUI scene.
     */
    private void addMenuOverlay() {
        float width = 40f; // in pixels
        int numLines = 25;
        menuOverlay = new Overlay("menu", width, numLines);
        menuOverlay.setLocationPolicy(LocationPolicy.CenterLeft);

        boolean success = stateManager.attach(menuOverlay);
        assert success;

        menuOverlay.setEnabled(true);
        updateMenuOverlay();
    }

    /**
     * Execute the selected app.
     */
    private static void executeSelectedApp() {
        CommandLine commandLine = new CommandLine(script);

        Class<?> mainClass = mainClasses[selectedAppIndex];
        String mainClassName = mainClass.getName();
        commandLine.addArgument(mainClassName);

        DefaultExecutor executor = new DefaultExecutor();
        try {
            executor.execute(commandLine, env);
            // ignore the return code
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Look for the shell script (or batch file) to execute in
     * "./build/install/WesExamples/bin".
     *
     * @return a new instance
     */
    private static File findScriptToExecute() {
        File buildDir = new File("build");
        File installDir = new File(buildDir, "install");
        File examplesDir = new File(installDir, "WesExamples");
        File binDir = new File(examplesDir, "bin");

        Platform platform = JmeSystem.getPlatform();
        String fileName = "WesExamples";
        if (platform.getOs() == Platform.Os.Windows) {
            fileName += ".bat";
        }
        File result = new File(binDir, fileName);

        String path = Heart.fixedPath(result);
        if (!result.exists()) {
            String message = MyString.quote(path) + " not found!";
            throw new RuntimeException(message);
        }

        return result;
    }

    /**
     * Update the menu overlay for the current selectedAppIndex.
     */
    private static void updateMenuOverlay() {
        int numLines = menuOverlay.countLines();
        int selectedLineIndex = numLines / 2;
        int topLineAppIndex = selectedAppIndex - selectedLineIndex;

        for (int lineIndex = 0; lineIndex < numLines; ++lineIndex) {
            int appIndex = topLineAppIndex + lineIndex;
            String text = "";
            if (appIndex >= 0 && appIndex < mainClasses.length) {
                Class<?> mainClass = mainClasses[appIndex];
                text = mainClass.getSimpleName();
                if (Heart.hasStoredSettings(text)) {
                    text = text + " +";
                }
            }
            if (lineIndex == selectedLineIndex) {
                text = "--> " + text;
                menuOverlay.setText(lineIndex, text, ColorRGBA.Yellow);
            } else {
                menuOverlay.setText(lineIndex, text, ColorRGBA.White);
            }
        }
    }
}
