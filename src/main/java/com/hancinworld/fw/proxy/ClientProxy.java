//Copyright (c) 2015, David Larochelle-Pratte
//All rights reserved.
//
//        Redistribution and use in source and binary forms, with or without
//        modification, are permitted provided that the following conditions are met:
//
//        1. Redistributions of source code must retain the above copyright notice, this
//        list of conditions and the following disclaimer.
//        2. Redistributions in binary form must reproduce the above copyright notice,
//        this list of conditions and the following disclaimer in the documentation
//        and/or other materials provided with the distribution.
//
//        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
//        ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
//        WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
//        ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
//        (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
//        LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
//        ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
//        SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package com.hancinworld.fw.proxy;

import com.hancinworld.fw.handler.ConfigurationHandler;
import com.hancinworld.fw.handler.DrawScreenEventHandler;
import com.hancinworld.fw.reference.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.awt.Rectangle;
import java.awt.Point;
import java.nio.IntBuffer;

public class ClientProxy implements IProxy {
    private final Minecraft client = Minecraft.getInstance();
    private Rectangle _savedWindowedBounds;
    public static boolean fullscreen;
    public static KeyBinding fullscreenKeyBinding;
    public DrawScreenEventHandler dsHandler;

    /** This keybind replaces the default MC fullscreen keybind in their logic handler. Without it, the game crashes.
     *  If this is set to any valid key, problems may occur. */
    public static KeyBinding ignoreKeyBinding = new KeyBinding("key.fullscreenwindowed.unused", -1, "key.categories.misc");

    @Override
    public void registerKeyBindings()
    {
        /* FIXME: Overrides the minecraft hotkey for fullscreen, as there are no hooks */
        if(fullscreenKeyBinding == null && ConfigurationHandler.ENABLED.get())
        {
            fullscreenKeyBinding = client.gameSettings.keyBindFullscreen;
            client.gameSettings.keyBindFullscreen = ignoreKeyBinding;

        }
        else if(fullscreenKeyBinding != null && !ConfigurationHandler.ENABLED.get())
        {
            Minecraft mc = Minecraft.getInstance();
            mc.gameSettings.keyBindFullscreen = fullscreenKeyBinding;
            fullscreenKeyBinding = null;

            if(fullscreen){
                mc.mainWindow.toggleFullscreen();
                //mc.fullscreen = false;
                //mc.toggleFullscreen();
                mc.mainWindow.update();
            }
        }
    }

    @Override
    public void subscribeEvents() {
        dsHandler = new DrawScreenEventHandler();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(dsHandler::handleDrawScreenEvent);
    }

    private Rectangle findCurrentScreenDimensionsAndPosition(int x, int y)
    {
        //attempt to use GLFW to get the screen's video mode
        GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        if (mode == null)
            return null;
        return new Rectangle(0, 0, mode.width(), mode.height());
	}

    private Rectangle findScreenDimensionsByID(int monitorID)
    {
        if(monitorID < 1)
            return null;

        PointerBuffer monitors = GLFW.glfwGetMonitors();

        if(monitors == null || monitors.limit() == 0 || monitors.limit() < monitorID){
            return null;
        }

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitors.get(monitorID - 1));
        if (mode == null)
            return null;
        IntBuffer xPos = BufferUtils.createIntBuffer(1);
        IntBuffer yPos = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetMonitorPos(monitors.get(monitorID - 1), xPos, yPos);
        return new Rectangle(xPos.get(0), yPos.get(0), mode.width(), mode.height());
    }

    private Rectangle getAppropriateScreenBounds(Rectangle currentCoordinates, int desiredMonitor)
    {
        Rectangle screenBounds;
        Point centerCoordinates = new Point((int) (currentCoordinates.getMinX() + currentCoordinates.getWidth() / 2), (int) (currentCoordinates.getMinY() + currentCoordinates.getHeight() / 2));

        //First feature mode: Only remove decorations. No need to calculate screen positions, we're not changing size or location.
        if(ConfigurationHandler.ADVANCED_ENABLED.get() && ConfigurationHandler.ONLY_REMOVE_DECORATIONS.get()){
            screenBounds = currentCoordinates;
        }
        //Custom dimensions enabled: follow requested settings if we can work with them.
        else if(ConfigurationHandler.ADVANCED_ENABLED.get() && ConfigurationHandler.CUSTOM_FULLSCREEN.get())
        {
            screenBounds = new Rectangle(ConfigurationHandler.CUSTOM_FULLSCREEN_X.get(),ConfigurationHandler.CUSTOM_FULLSCREEN_Y.get(),ConfigurationHandler.CUSTOM_FULLSCREEN_W.get(),ConfigurationHandler.CUSTOM_FULLSCREEN_H.get());

            //If you've selected a monitor, then X & Y are offsets - easier to do math.
            if(desiredMonitor > 0) {
                Rectangle actualScreenBounds = findScreenDimensionsByID(desiredMonitor);
                if(actualScreenBounds != null){
                    screenBounds.setLocation(actualScreenBounds.x + screenBounds.x, actualScreenBounds.y + screenBounds.y);
                }
            }
        }
        // No specified monitor for fullscreen -> find the one the window is on right now
        else if(desiredMonitor < 0 || desiredMonitor == Reference.AUTOMATIC_MONITOR_SELECTION) {
            //find which monitor we should be using based on the center of the MC window
            screenBounds = findCurrentScreenDimensionsAndPosition((int) centerCoordinates.getX(), (int) centerCoordinates.getY());
        // specified monitor for fullscreen -> get dimensions.
        }else{
            screenBounds = findScreenDimensionsByID(desiredMonitor);
            // you've specified a monitor but it doesn't look connected. Revert to automatic mode.
            if(screenBounds == null){
                screenBounds = findCurrentScreenDimensionsAndPosition((int) centerCoordinates.getX(), (int) centerCoordinates.getY());
            }
        }

        return screenBounds;
    }
    @Override
    public void toggleFullScreen(boolean goFullScreen) {
        toggleFullScreen(goFullScreen, ConfigurationHandler.FULLSCREEN_MONITOR.get());
    }

    private boolean isFullscreen() {
        return GLFW.glfwGetWindowMonitor(client.mainWindow.getHandle()) != 0L;
    }

    @Override
    public void toggleFullScreen(boolean goFullScreen, int desiredMonitor) {

        //If we're in actual fullscreen right now, then we need to fix that.
        if(isFullscreen()) {
            fullscreen = true;
        }

        // If all state is valid, there is nothing to do and we just exit.
        if(fullscreen == goFullScreen
                && !isFullscreen() //Display in fullscreen mode: Change required
                && GLFW.glfwGetWindowAttrib(client.mainWindow.getHandle(), GLFW.GLFW_DECORATED) == GLFW.GLFW_FALSE // Window not in expected state
        )
            return;

        //Save our current display parameters
        IntBuffer currX = BufferUtils.createIntBuffer(1);
        IntBuffer currY = BufferUtils.createIntBuffer(1);
        IntBuffer currW = BufferUtils.createIntBuffer(1);
        IntBuffer currH = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetWindowPos(client.mainWindow.getHandle(), currX, currY);
        GLFW.glfwGetWindowSize(client.mainWindow.getHandle(), currW, currH);
        Rectangle currentCoordinates = new Rectangle(currX.get(0), currY.get(0), currW.get(0), currH.get(0));

        if(goFullScreen && !isFullscreen())
            _savedWindowedBounds = currentCoordinates;

        GLFW.glfwSetWindowAttrib(client.mainWindow.getHandle(), GLFW.GLFW_DECORATED, goFullScreen ? GLFW.GLFW_FALSE : GLFW.GLFW_TRUE);

        //Get the fullscreen dimensions for the appropriate screen.
        Rectangle screenBounds = getAppropriateScreenBounds(currentCoordinates, desiredMonitor);

        //This is the new bounds we have to apply.
        Rectangle newBounds = goFullScreen ? screenBounds : _savedWindowedBounds;
        if(newBounds == null)
            newBounds = screenBounds;

        if(!goFullScreen && !fullscreen) {
            newBounds = currentCoordinates;
            _savedWindowedBounds = currentCoordinates;
        }

        fullscreen = goFullScreen;
        // TODO: set the client's fullscreen thing somehow
        //client.fullscreen = fullscreen;
        if( client.gameSettings.fullscreen != fullscreen) {
            client.gameSettings.fullscreen = fullscreen;
            client.gameSettings.saveOptions();
        }
        if (fullscreen) {
            GLFW.glfwSetWindowMonitor(client.mainWindow.getHandle(), 0L, newBounds.x, newBounds.y, newBounds.width, newBounds.height, -1);
        }
        GLFW.glfwSetWindowSize(client.mainWindow.getHandle(), newBounds.width, newBounds.height);
        GLFW.glfwSetWindowPos(client.mainWindow.getHandle(), newBounds.x, newBounds.y);
        //Display.setFullscreen(false);
        //Display.setDisplayMode(new DisplayMode((int) newBounds.getWidth(), (int) newBounds.getHeight()));
        //Display.setLocation(newBounds.x, newBounds.y);

        // TODO: mc.func_213226_a()?
        //client.resize((int) newBounds.getWidth(), (int) newBounds.getHeight());
        //Display.setResizable(!goFullScreen);
        //Display.setVSyncEnabled(client.gameSettings.enableVsync);
        //client.updateDisplay();

    }

    @Override
    @SuppressWarnings("deprecated")
    public void performStartupChecks()
    {
        //If the mod is disabled by configuration, just put back the initial value.
        if(!ConfigurationHandler.ENABLED.get()) {
            return;
        }

        if(ConfigurationHandler.MAXIMUM_COMPATIBILITY.get()){
            dsHandler.setInitialFullscreen(client.gameSettings.fullscreen, ConfigurationHandler.FULLSCREEN_MONITOR.get());
        // This is the correct way to set fullscreen at launch, but LWJGL limitations means we might crash the game if
        // another mod tries to do a similar Display changing operation. Doesn't help the API says "don't use this"
        }else{
            throw new RuntimeException("not implemented yet");
//            try {
//                //FIXME: Living dangerously here... Is there a better way of doing this?
//                SplashProgress.pause();
//                toggleFullScreen(client.gameSettings.fullScreen, ConfigurationHandler.FULLSCREEN_MONITOR.get());
//                SplashProgress.resume();
//            }catch(NoClassDefFoundError e) {
//                LogHelper.warn("Error while doing startup checks, are you using an old version of Forge ? " + e);
//                toggleFullScreen(client.gameSettings.fullScreen, ConfigurationHandler.FULLSCREEN_MONITOR.get());
//            }
        }
    }
}
