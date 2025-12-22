package org.sawiq.group;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.security.SecureRandom;
import java.util.*;

public class GroupManager {
    private static final SecureRandom RNG = new SecureRandom();

    private final MinecraftClient mc;

    private String groupId;
    private String groupToken;
    private UUID leaderUuid;
    private final Map<UUID, GroupMemberState> members = new HashMap<>();
    private final Set<String> pendingInvites = new HashSet<>();
    private final Map<String, String> inviteTokenByGroupId = new HashMap<>();
    private final Map<String, UUID> inviteSenderByGroupId = new HashMap<>();

    private long lastBroadcastMs;

    public GroupManager(MinecraftClient mc) {
        this.mc = mc;
    }

    public boolean isInGroup() {
        return groupId != null && !groupId.isEmpty();
    }

    public String getGroupId() {
        return groupId;
    }

    public Collection<GroupMemberState> getMembers() {
        return members.values();
    }

    public void createGroup() {
        if (isInGroup()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.already_in_group"), false);
            }
            return;
        }
        this.groupId = randomId();
        this.groupToken = randomToken();
        this.leaderUuid = mc.player != null ? mc.player.getUuid() : null;
        this.members.clear();
        this.pendingInvites.clear();
        this.inviteTokenByGroupId.clear();
        this.inviteSenderByGroupId.clear();
        if (mc.player != null) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.created", groupId), false);
        }
    }

    public void leaveGroup() {
        if (!isInGroup()) return;
        UUID self = mc.player != null ? mc.player.getUuid() : null;
        String gid = this.groupId;
        String tok = this.groupToken;
        this.groupId = null;
        this.groupToken = null;
        this.leaderUuid = null;
        this.members.clear();
        this.pendingInvites.clear();
        this.inviteTokenByGroupId.clear();
        this.inviteSenderByGroupId.clear();
        if (self != null) {
            broadcastRaw(GroupProtocol.encodeLeave(gid, tok, self));
        }
        if (mc.player != null) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.left"), false);
        }
    }

    private void clearGroupLocal() {
        this.groupId = null;
        this.groupToken = null;
        this.leaderUuid = null;
        this.members.clear();
        this.pendingInvites.clear();
        this.inviteTokenByGroupId.clear();
        this.inviteSenderByGroupId.clear();
    }

    public void invite(String playerName) {
        if (!isInGroup()) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.not_in_group"), false);
            }
            return;
        }

        if (mc.player != null && leaderUuid != null && !leaderUuid.equals(mc.player.getUuid())) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.not_leader"), false);
            return;
        }
        if (mc.player != null) {
            sendTo(playerName, GroupProtocol.encodeInvite(groupId, groupToken, playerName, mc.player.getUuid(), mc.player.getName().getString()));
        }
        if (mc.player != null) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.invited", playerName), false);
        }
    }

    public void acceptInvite(String gid) {
        if (mc.player == null) return;

        if (isInGroup()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.already_in_group"), false);
            return;
        }

        if (!pendingInvites.contains(gid)) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.no_invite", gid), false);
            return;
        }

        String token = inviteTokenByGroupId.get(gid);
        if (token == null || token.isEmpty()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.no_invite", gid), false);
            return;
        }

        UUID inviter = inviteSenderByGroupId.get(gid);

        // One-time invite: consume only this groupId.
        pendingInvites.remove(gid);
        inviteTokenByGroupId.remove(gid);
        inviteSenderByGroupId.remove(gid);

        this.groupId = gid;
        this.groupToken = token;
        this.leaderUuid = inviter;
        this.members.clear();

        if (inviter != null) {
            String inviterName = resolveName(inviter);
            if (inviterName != null && !inviterName.isEmpty()) {
                members.put(inviter, new GroupMemberState(inviter, inviterName));
            }
        }

        UUID self = mc.player.getUuid();
        String name = mc.player.getName().getString();
        if (inviter != null) {
            String payload = GroupProtocol.encodeJoin(gid, groupToken, self, name);
            String inviterName = resolveName(inviter);
            if (inviterName != null && !inviterName.isEmpty()) {
                sendTo(inviterName, payload);
            } else {
                broadcastRaw(payload);
            }
        } else {
            broadcastRaw(GroupProtocol.encodeJoin(gid, groupToken, self, name));
        }
        mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.joined", gid), false);
    }

    public void kickByName(String playerName) {
        if (mc.player == null) return;
        if (!isInGroup()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.not_in_group"), false);
            return;
        }
        if (leaderUuid == null || !leaderUuid.equals(mc.player.getUuid())) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.not_leader"), false);
            return;
        }
        if (playerName == null || playerName.isEmpty()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.usage.kick"), false);
            return;
        }

        UUID target = null;
        for (GroupMemberState m : members.values()) {
            if (m == null || m.uuid == null || m.name == null) continue;
            if (m.name.equalsIgnoreCase(playerName)) {
                target = m.uuid;
                break;
            }
        }
        if (target == null) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.member_not_found", playerName), false);
            return;
        }

        String payload = GroupProtocol.encodeKick(groupId, groupToken, target);
        String targetName = resolveName(target);
        if (targetName != null && !targetName.isEmpty()) {
            sendTo(targetName, payload);
        } else {
            broadcastRaw(payload);
        }

        // Make sure everyone removes them too.
        broadcastRaw(GroupProtocol.encodeLeave(groupId, groupToken, target));
        mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.kicked", playerName), false);
    }

    public void onIncomingGroupMessage(String rawMessage) {
        onIncomingGroupMessage(rawMessage, null);
    }

    public void onIncomingGroupMessage(String rawMessage, UUID senderUuid) {
        if (rawMessage == null || !GroupProtocol.isGroupMessage(rawMessage)) return;
        String[] parts = rawMessage.split("\\|", -1);
        if (parts.length < 2) return;

        String type = parts[1];
        if ("INV".equals(type)) {
            if (parts.length < 5) return;
            String gid = GroupProtocol.unsafe(parts[2]);
            String tok = GroupProtocol.unsafe(parts[3]);
            String targetName = GroupProtocol.unsafe(parts[4]);
            if (gid.isEmpty()) return;
            if (tok.isEmpty()) return;

            if (mc.player == null) return;
            String selfName = mc.player.getName().getString();
            if (targetName != null && !targetName.isEmpty() && !targetName.equalsIgnoreCase(selfName)) {
                return;
            }

            if (isInGroup()) {
                // Already in a group: keep the invite stored but don't auto-join.
                pendingInvites.add(gid);
                inviteTokenByGroupId.put(gid, tok);
            } else {
                pendingInvites.add(gid);
                inviteTokenByGroupId.put(gid, tok);
            }

            UUID inviterUuid = null;
            String inviterName = null;

            if (parts.length >= 6) {
                inviterUuid = parseUuid(parts[5]);
            }
            if (parts.length >= 7) {
                inviterName = GroupProtocol.unsafe(parts[6]);
            }

            if (inviterUuid == null) {
                inviterUuid = senderUuid;
            }
            if ((inviterName == null || inviterName.isEmpty()) && inviterUuid != null) {
                inviterName = resolveName(inviterUuid);
            }

            if (inviterUuid != null) {
                inviteSenderByGroupId.put(gid, inviterUuid);
                if (inviterName != null && !inviterName.isEmpty()) {
                    final String nameFinal = inviterName;
                    members.computeIfAbsent(inviterUuid, u -> new GroupMemberState(u, nameFinal));
                }
            }

            if (mc.player != null) {
                Text clickableId = Text.literal(gid).styled(style -> style
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand("/cpgroup accept " + gid))
                        .withHoverEvent(new HoverEvent.ShowText(Text.translatable("text.coordinatesplus.group.invite_click_hover"))));
                String inviterDisplay = inviterName;
                if (inviterDisplay == null || inviterDisplay.isEmpty()) {
                    if (inviterUuid != null) {
                        inviterDisplay = resolveName(inviterUuid);
                    }
                }
                if (inviterDisplay == null || inviterDisplay.isEmpty()) {
                    inviterDisplay = "?";
                }

                mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.invite_received_click", inviterDisplay, clickableId), false);
            }
            return;
        }

        if ("JOIN".equals(type)) {
            if (parts.length < 6) return;
            String gid = GroupProtocol.unsafe(parts[2]);
            String tok = GroupProtocol.unsafe(parts[3]);
            if (!Objects.equals(gid, groupId)) return;
            if (groupToken == null || !Objects.equals(tok, groupToken)) return;
            UUID uuid = parseUuid(parts[4]);
            if (uuid == null) return;
            String name = GroupProtocol.unsafe(parts[5]);
            if (mc.player != null && uuid.equals(mc.player.getUuid())) return;
            members.compute(uuid, (k, v) -> v != null ? v : new GroupMemberState(uuid, name));

            if (mc.player != null && isInGroup()) {
                String payload = GroupProtocol.encodeJoin(groupId, groupToken, mc.player.getUuid(), mc.player.getName().getString());
                if (senderUuid != null) {
                    String senderName = resolveName(senderUuid);
                    if (senderName != null && !senderName.isEmpty()) {
                        sendTo(senderName, payload);
                    }
                }
            }
            return;
        }

        if ("LEAV".equals(type)) {
            if (parts.length < 5) return;
            String gid = GroupProtocol.unsafe(parts[2]);
            String tok = GroupProtocol.unsafe(parts[3]);
            if (!Objects.equals(gid, groupId)) return;
            if (groupToken == null || !Objects.equals(tok, groupToken)) return;
            UUID uuid = parseUuid(parts[4]);
            if (uuid == null) return;
            members.remove(uuid);
            return;
        }

        if ("KICK".equals(type)) {
            if (parts.length < 5) return;
            String gid = GroupProtocol.unsafe(parts[2]);
            String tok = GroupProtocol.unsafe(parts[3]);
            if (!Objects.equals(gid, groupId)) return;
            if (groupToken == null || !Objects.equals(tok, groupToken)) return;
            UUID target = parseUuid(parts[4]);
            if (target == null) return;

            if (mc.player != null && target.equals(mc.player.getUuid())) {
                // Remove any stored invites to the same group so the player can't re-accept an old invite.
                pendingInvites.remove(gid);
                inviteTokenByGroupId.remove(gid);
                inviteSenderByGroupId.remove(gid);
                clearGroupLocal();
                mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.kicked_you"), false);
            } else {
                members.remove(target);
            }
            return;
        }

        if ("POS".equals(type)) {
            if (parts.length < 14) return;
            String gid = GroupProtocol.unsafe(parts[2]);
            String tok = GroupProtocol.unsafe(parts[3]);
            if (!Objects.equals(gid, groupId)) return;
            if (groupToken == null || !Objects.equals(tok, groupToken)) return;
            UUID uuid = parseUuid(parts[4]);
            if (uuid == null) return;
            if (mc.player != null && uuid.equals(mc.player.getUuid())) return;
            String name = GroupProtocol.unsafe(parts[5]);
            GroupMemberState state = members.computeIfAbsent(uuid, u -> new GroupMemberState(u, name));
            state.name = name;
            state.overworldX = parseInt(parts[6]);
            state.overworldY = parseInt(parts[7]);
            state.overworldZ = parseInt(parts[8]);
            state.netherX = parseInt(parts[9]);
            state.netherY = parseInt(parts[10]);
            state.netherZ = parseInt(parts[11]);
            state.lastDimensionKey = GroupProtocol.unsafe(parts[12]);
            state.lastUpdateMs = parseLong(parts[13]);
        }
    }

    public void tick() {
        if (!isInGroup() || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBroadcastMs < 250) return;
        lastBroadcastMs = now;

        int owX;
        int owY;
        int owZ;
        int neX;
        int neY;
        int neZ;

        double rawX = mc.player.getX();
        double rawY = mc.player.getY();
        double rawZ = mc.player.getZ();
        String dimKey = mc.player.getWorld().getRegistryKey().getValue().toString();
        boolean inNether = "minecraft:the_nether".equals(dimKey);

        if (inNether) {
            neX = (int) Math.round(rawX);
            neY = (int) Math.round(rawY);
            neZ = (int) Math.round(rawZ);
            owX = (int) Math.round(rawX * 8.0);
            owY = neY;
            owZ = (int) Math.round(rawZ * 8.0);
        } else {
            owX = (int) Math.round(rawX);
            owY = (int) Math.round(rawY);
            owZ = (int) Math.round(rawZ);
            neX = (int) Math.round(rawX * 0.125);
            neY = owY;
            neZ = (int) Math.round(rawZ * 0.125);
        }

        String msg = GroupProtocol.encodePos(groupId, groupToken, mc.player.getUuid(), mc.player.getName().getString(),
                owX, owY, owZ,
                neX, neY, neZ,
                dimKey, now);

        broadcastRaw(msg);
    }

    public boolean handleOutgoingCommand(String message) {
        if (message == null) return false;
        String m = message.trim();
        if (!(m.startsWith("!group") || m.startsWith("!g"))) return false;

        String[] args = m.split("\\s+");
        if (args.length < 2) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.help.commands"), false);
            }
            return true;
        }

        String sub = args[1];
        String arg = args.length >= 3 ? args[2] : null;
        executeSubcommand(sub, arg);
        return true;
    }

    public void executeSubcommand(String sub, String arg) {
        if (sub == null) return;
        executeSubcommand(sub.toLowerCase(Locale.ROOT), arg, true);
    }

    private void executeSubcommand(String sub, String arg, boolean showHelpOnError) {
        if ("create".equals(sub)) {
            createGroup();
            return;
        }
        if ("leave".equals(sub)) {
            leaveGroup();
            return;
        }
        if ("list".equals(sub)) {
            listMembers();
            return;
        }
        if ("invite".equals(sub)) {
            if (arg == null || arg.isEmpty()) {
                if (showHelpOnError && mc.player != null) {
                    mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.usage.invite"), false);
                }
                return;
            }
            invite(arg);
            return;
        }
        if ("accept".equals(sub)) {
            if (arg == null || arg.isEmpty()) {
                if (showHelpOnError && mc.player != null) {
                    mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.usage.accept"), false);
                }
                return;
            }
            acceptInvite(arg);
            return;
        }

        if ("kick".equals(sub)) {
            if (arg == null || arg.isEmpty()) {
                if (showHelpOnError && mc.player != null) {
                    mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.usage.kick"), false);
                }
                return;
            }
            kickByName(arg);
            return;
        }

        if (showHelpOnError && mc.player != null) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.help.commands"), false);
        }
    }

    private void broadcastRaw(String payload) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        for (UUID uuid : members.keySet()) {
            if (uuid == null) continue;
            if (mc.player.getUuid().equals(uuid)) continue;

            String name = resolveName(uuid);
            if (name == null || name.isEmpty()) continue;
            sendTo(name, payload);
        }
    }

    private void sendTo(String playerName, String payload) {
        if (mc.player == null) return;
        mc.player.networkHandler.sendChatMessage("/msg " + playerName + " " + payload);
    }

    private void listMembers() {
        if (mc.player == null) return;
        if (!isInGroup()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.error.not_in_group"), false);
            return;
        }

        mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.info.header", groupId), false);

        String selfLine = mc.player.getName().getString() + " (you)" +
                " | OW: " + (int) Math.round(mc.player.getX()) + " " + (int) Math.round(mc.player.getY()) + " " + (int) Math.round(mc.player.getZ()) +
                " | N: " + (int) Math.round(mc.player.getX() * 0.125) + " " + (int) Math.round(mc.player.getY()) + " " + (int) Math.round(mc.player.getZ() * 0.125);
        mc.player.sendMessage(Text.literal(selfLine), false);

        if (members.isEmpty()) {
            mc.player.sendMessage(Text.translatable("text.coordinatesplus.group.info.members_empty"), false);
            return;
        }

        for (GroupMemberState m : members.values()) {
            if (m == null) continue;
            String name = m.name == null ? "?" : m.name;
            String line = name + " | OW: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ + " | N: " + m.netherX + " " + m.netherY + " " + m.netherZ;
            mc.player.sendMessage(Text.literal(line), false);
        }
    }

    private String resolveName(UUID uuid) {
        if (mc.getNetworkHandler() == null) return null;
        var entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null || entry.getProfile() == null) return null;
        return entry.getProfile().getName();
    }

    private static String randomId() {
        byte[] buf = new byte[4];
        RNG.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(Integer.toString((b & 0xFF), 36));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private static String randomToken() {
        byte[] buf = new byte[16];
        RNG.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String s = Integer.toString((b & 0xFF), 16);
            if (s.length() == 1) sb.append('0');
            sb.append(s);
        }
        return sb.toString();
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
