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
package com.hancinworld.fw;

import com.hancinworld.fw.handler.*;
import com.hancinworld.fw.proxy.ClientProxy;
import com.hancinworld.fw.proxy.IProxy;
import com.hancinworld.fw.reference.Reference;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Reference.MOD_ID)
public class FullscreenWindowed {

    public static IProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> () -> null);

    public FullscreenWindowed() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigurationHandler.CLIENT_CONFIG);

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        ConfigurationHandler.loadConfig(ConfigurationHandler.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("fw-client.toml"));
    }

    private void setup(final FMLCommonSetupEvent event) {
        proxy.init();
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        //Items and blocks
        if(proxy != null)
            proxy.subscribeEvents(event.getSuggestedConfigurationFile());

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if(proxy != null)
            proxy.registerKeyBindings();

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if(proxy != null)
            proxy.performStartupChecks();
    }

}
