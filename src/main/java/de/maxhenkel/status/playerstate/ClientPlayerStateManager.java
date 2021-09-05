package de.maxhenkel.status.playerstate;

import de.maxhenkel.status.Status;
import de.maxhenkel.status.StatusClient;
import de.maxhenkel.status.events.ClientWorldEvents;
import de.maxhenkel.status.net.NetManager;
import de.maxhenkel.status.net.PlayerStatePacket;
import de.maxhenkel.status.net.PlayerStatesPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public class ClientPlayerStateManager {

    private PlayerState state;
    private Map<UUID, PlayerState> states;

    public ClientPlayerStateManager() {
        state = getDefaultState();
        states = new HashMap<>();
        NetManager.registerClientReceiver(PlayerStatePacket.class, (client, handler, responseSender, packet) -> {
            states.put(packet.getPlayerState().getPlayer(), packet.getPlayerState());
        });
        NetManager.registerClientReceiver(PlayerStatesPacket.class, (client, handler, responseSender, packet) -> {
            states = packet.getPlayerStates();
        });
        ClientWorldEvents.DISCONNECT.register(this::onDisconnect);
        ClientWorldEvents.JOIN_SERVER.register(this::onConnect);
    }

    public String getState() {
        return state.getState();
    }

    public void setState(String s) {
        state.setState(s);
        syncOwnState();
        StatusClient.CLIENT_CONFIG.status.set(s);
        StatusClient.CLIENT_CONFIG.status.save();
    }

    public void setAvailability(Availability availability) {
        state.setAvailability(availability);
        syncOwnState();
        StatusClient.CLIENT_CONFIG.availability.set(availability);
        StatusClient.CLIENT_CONFIG.availability.save();
    }

    public Availability getAvailability() {
        return state.getAvailability();
    }

    private PlayerState getDefaultState() {
        if (StatusClient.CLIENT_CONFIG.persistState.get()) {
            return new PlayerState(Minecraft.getInstance().getUser().getGameProfile().getId(), StatusClient.CLIENT_CONFIG.availability.get(), StatusClient.CLIENT_CONFIG.status.get());
        } else {
            return new PlayerState(Minecraft.getInstance().getUser().getGameProfile().getId());
        }
    }

    private void onDisconnect() {
        clearStates();
    }

    private void onConnect() {
        syncOwnState();
        if (StatusClient.CLIENT_CONFIG.showJoinMessage.get()) {
            showChangeStatusMessage();
        }
    }

    private void showChangeStatusMessage() {
        Minecraft.getInstance().player.sendMessage(ComponentUtils.wrapInSquareBrackets(new TranslatableComponent("message.status.mod_name"))
                        .withStyle(ChatFormatting.GREEN)
                        .append(" ")
                        .append(new TranslatableComponent("message.status.change_status")
                                .withStyle(style -> style
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent("message.status.set_status")))
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/status gui"))
                                ).withStyle(ChatFormatting.WHITE)
                        )
                , Util.NIL_UUID);
    }

    public void syncOwnState() {
        NetManager.sendToServer(new PlayerStatePacket(state));
    }

    @Nullable
    public PlayerState getState(UUID player) {
        return states.get(player);
    }

    private static final ResourceLocation DND = new ResourceLocation(Status.MODID, "textures/icons/dnd.png");
    private static final ResourceLocation OPEN = new ResourceLocation(Status.MODID, "textures/icons/open.png");
    private static final ResourceLocation NO_AVAILABILITY = new ResourceLocation(Status.MODID, "textures/icons/no_availability.png");
    private static final ResourceLocation RECORDING = new ResourceLocation(Status.MODID, "textures/icons/recording.png");
    private static final ResourceLocation STREAMING = new ResourceLocation(Status.MODID, "textures/icons/streaming.png");
    private static final ResourceLocation NO_SLEEP = new ResourceLocation(Status.MODID, "textures/icons/no_sleep.png");
    private static final ResourceLocation NEUTRAL = new ResourceLocation(Status.MODID, "textures/icons/neutral.png");

    @Nullable
    public ResourceLocation getIcon(UUID player) {
        PlayerState state = getState(player);
        if (state == null) {
            return null;
        }
        if (state.getState().equals("recording")) {
            return RECORDING;
        } else if (state.getState().equals("streaming")) {
            return STREAMING;
        } else if (state.getState().equals("no_sleep")) {
            return NO_SLEEP;
        } else {
            return NEUTRAL;
        }
    }

    @Nullable
    public ResourceLocation getOverlay(UUID player) {
        PlayerState state = getState(player);
        if (state == null) {
            return null;
        }
        if (state.getAvailability().equals(Availability.DO_NOT_DISTURB)) {
            return DND;
        } else if (state.getAvailability().equals(Availability.OPEN)) {
            return OPEN;
        }
        return NO_AVAILABILITY;
    }

    public void clearStates() {
        states.clear();
    }
}
