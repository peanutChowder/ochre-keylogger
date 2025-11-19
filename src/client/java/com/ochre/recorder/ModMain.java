package com.ochre.recorder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModMain implements ClientModInitializer {

    private static boolean recording = false;
    private static FileWriter writer = null;
    private static long tickCounter = 0;

    private KeyBinding startKey;
    private KeyBinding stopKey;

    @Override
    public void onInitializeClient() {

        startKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ochre.start_recording",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                "category.ochre.recorder"
        ));

        stopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ochre.stop_recording",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "category.ochre.recorder"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (startKey.wasPressed()) startRecording();
            if (stopKey.wasPressed()) stopRecording();

            if (recording) captureTick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!recording) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null || client.textRenderer == null || client.getWindow() == null) return;

                String msg = "Not collecting data";
                int x = 4;
                int y = client.getWindow().getScaledHeight() - 4 - client.textRenderer.fontHeight;

                drawContext.drawText(client.textRenderer, msg, x, y, 0xFFFF5555, true);
            }
        });
    }

    private void startRecording() {
        if (recording) return;

        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File dir = new File(System.getProperty("user.home") + "/.minecraft/action-recordings/" + ts);
            dir.mkdirs();

            writer = new FileWriter(new File(dir, "actions.jsonl"), true);
            recording = true;
            tickCounter = 0;

            System.out.println("[OchreRecorder] Recording started.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (!recording) return;

        try {
            writer.close();
        } catch (Exception ignored) {}

        recording = false;
        writer = null;

        System.out.println("[OchreRecorder] Recording stopped.");
    }

    private void captureTick(MinecraftClient client) {
        tickCounter++;

        // 20Hz sampling (1 sample per game tick)
        long worldTick = client.world.getTime();

        var p = client.player;
        var options = client.options;

        boolean w = options.forwardKey.isPressed();
        boolean s = options.backKey.isPressed();
        boolean a = options.leftKey.isPressed();
        boolean d = options.rightKey.isPressed();
        boolean jump = options.jumpKey.isPressed();

        float yaw = p.getYaw();
        float pitch = p.getPitch();

        String json = String.format(
                "{\"tick\":%d,\"frame\":%d,\"yaw\":%.4f,\"pitch\":%.4f,\"w\":%b,\"a\":%b,\"s\":%b,\"d\":%b,\"jump\":%b}\n",
                worldTick, tickCounter, yaw, pitch, w, a, s, d, jump
        );

        try {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
