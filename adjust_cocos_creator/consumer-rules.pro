# Keep the Cocos/Adjust bridge classes because they are used by
# JSB dispatch and Android reflection from the TypeScript layer.
-keep class com.game.magic.** { *; }

# Keep the Android reflection entry point explicitly.
-keep class com.game.magic.AdjustCocosInit {
    public static void initBridge();
}
