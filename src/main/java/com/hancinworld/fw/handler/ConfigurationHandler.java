package com.hancinworld.fw.handler;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.hancinworld.fw.reference.Reference;
import net.minecraftforge.common.ForgeConfigSpec;
import java.nio.file.Path;

public class ConfigurationHandler {

	public static final String CATEGORY_GENERAL = "general";
	public static final String CATEGORY_ADVANCED = "advanced";

	private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec CLIENT_CONFIG;

	// General settings
	public static ForgeConfigSpec.BooleanValue ENABLED;
	public static ForgeConfigSpec.BooleanValue MAXIMUM_COMPATIBILITY;
	public static ForgeConfigSpec.IntValue FULLSCREEN_MONITOR;

	// Advanced settings
	public static ForgeConfigSpec.BooleanValue ADVANCED_ENABLED;
	public static ForgeConfigSpec.BooleanValue CUSTOM_FULLSCREEN;
	public static ForgeConfigSpec.IntValue CUSTOM_FULLSCREEN_X;
	public static ForgeConfigSpec.IntValue CUSTOM_FULLSCREEN_Y;
	public static ForgeConfigSpec.IntValue CUSTOM_FULLSCREEN_W;
	public static ForgeConfigSpec.IntValue CUSTOM_FULLSCREEN_H;
	public static ForgeConfigSpec.BooleanValue ONLY_REMOVE_DECORATIONS;

	static {
		CLIENT_BUILDER.comment("General settings").push(CATEGORY_GENERAL);
		ENABLED = CLIENT_BUILDER
				.comment("Enable Fullscreen Windowed (replaces Minecraft Fullscreen)")
				.translation("comment.fullscreenwindowed.enableFullscreenWindowed")
				.define("enableFullscreenWindowed", true);
		MAXIMUM_COMPATIBILITY = CLIENT_BUILDER
				.comment("TRUE: Use a different startup strategy to make this mod play nicer with Optifine & other GL modifying mods. CHANGE AT YOUR OWN RISK.")
				.translation("comment.fullscreenwindowed.enableMaximumCompatibility")
				.define("enableMaximumCompatibility", Reference.ENABLE_MAXIMUM_COMPATIBILITY);
		FULLSCREEN_MONITOR = CLIENT_BUILDER
				.comment("Indicates which monitor (1-based) to use for fullscreen windowed mode. Use 0 for the default behavior of maximizing on the active monitor.")
				.translation("comment.fullscreenwindowed.fullscreenmonitor")
				.defineInRange("fullscreenMonitor", Reference.AUTOMATIC_MONITOR_SELECTION, 0, 50);
		CLIENT_BUILDER.pop();

		CLIENT_BUILDER.comment("Advanced settings").push(CATEGORY_ADVANCED);
		ADVANCED_ENABLED = CLIENT_BUILDER
				.comment("To use any of the features in the \"advanced\" section, set this to true.")
				.translation("comment.fullscreenwindowed.enableAdvancedFeatures")
				.define("enableAdvancedFeatures", Reference.ADVANCED_FEATURES_ENABLED);
		CUSTOM_FULLSCREEN = CLIENT_BUILDER
				.comment("Set this to true to customize what size the window is when fullscreen.")
				.translation("comment.fullscreenwindowed.customFullscreenDimensions")
				.define("customFullscreenDimensions", false);
		CUSTOM_FULLSCREEN_X = CLIENT_BUILDER
				.comment("X coordinate where to put the window when in fullscreen. 0 is the left pixel column of the primary monitor if fullscreenMonitor is 0, or the left pixel column of the selected monitor. customFullscreenDimensions must be true for this to have any effect.")
				.translation("comment.fullscreenwindowed.customFullscreenDimensionsX")
				.defineInRange("customFullscreenDimensionsX", 0, 0, Integer.MAX_VALUE);
		CUSTOM_FULLSCREEN_Y = CLIENT_BUILDER
				.comment("Y coordinate where to put the window when in fullscreen. 0 is the top pixel row of the primary monitor if fullscreenMonitor is 0, or the top pixel column of the selected monitor. customFullscreenDimensions must be true for this to have any effect.")
				.translation("comment.fullscreenwindowed.customFullscreenDimensionsY")
				.defineInRange("customFullscreenDimensionsY", 0, 0, Integer.MAX_VALUE);
		CUSTOM_FULLSCREEN_W = CLIENT_BUILDER
				.comment("Width of the window when fullscreen. customFullscreenDimensions must be true for this to have any effect.")
				.translation("comment.fullscreenwindowed.customFullscreenDimensionsW")
				.defineInRange("customFullscreenDimensionsW", 256, 256, Integer.MAX_VALUE);
		CUSTOM_FULLSCREEN_H = CLIENT_BUILDER
				.comment("Height of the window when fullscreen. customFullscreenDimensions must be true for this to have any effect.")
				.translation("comment.fullscreenwindowed.customFullscreenDimensionsH")
				.defineInRange("customFullscreenDimensionsH", 256, 256, Integer.MAX_VALUE);
		//TODO: due to how LWJGL draws windows, it's not a good idea to have this... disabled until I can fix the bugs with X,Y being off due to decoration shadows.
//		ONLY_REMOVE_DECORATIONS = CLIENT_BUILDER
//				.comment("If set to true, hitting the fullscreen button will only remove the title bar and not resize the window.")
//				.translation("comment.fullscreenwindowed.onlyRemoveDecorations")
//				.define("onlyRemoveDecorations", Reference.ONLY_REMOVE_DECORATIONS);
		CLIENT_BUILDER.pop();

		CLIENT_CONFIG = CLIENT_BUILDER.build();
	}

	public static void loadConfig(ForgeConfigSpec spec, Path path) {
		final CommentedFileConfig configData = CommentedFileConfig.builder(path)
				.sync()
				.autosave()
				.writingMode(WritingMode.REPLACE)
				.build();

		configData.load();
		spec.setConfig(configData);
	}

}