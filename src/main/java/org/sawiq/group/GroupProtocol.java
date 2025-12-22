package org.sawiq.group;

import java.util.Base64;
import java.util.UUID;

public final class GroupProtocol {
    public static final String PREFIX = "CPG|";

    private GroupProtocol() {
    }

    public static String encodeInvite(String groupId, String token, String targetName, UUID inviterUuid, String inviterName) {
        return PREFIX + "INV|" + safe(groupId) + "|" + safe(token) + "|" + safe(targetName) + "|" + inviterUuid + "|" + safe(inviterName);
    }

    public static String encodeJoin(String groupId, String token, UUID uuid, String name) {
        return PREFIX + "JOIN|" + safe(groupId) + "|" + safe(token) + "|" + uuid + "|" + safe(name);
    }

    public static String encodeLeave(String groupId, String token, UUID uuid) {
        return PREFIX + "LEAV|" + safe(groupId) + "|" + safe(token) + "|" + uuid;
    }

    public static String encodeKick(String groupId, String token, UUID targetUuid) {
        return PREFIX + "KICK|" + safe(groupId) + "|" + safe(token) + "|" + targetUuid;
    }

    public static String encodePos(String groupId, String token, UUID uuid, String name,
                                  int owX, int owY, int owZ,
                                  int neX, int neY, int neZ,
                                  String dimKey, long timeMs) {
        return PREFIX + "POS|" + safe(groupId) + "|" + safe(token) + "|" + uuid + "|" + safe(name) + "|" +
                owX + "|" + owY + "|" + owZ + "|" + neX + "|" + neY + "|" + neZ + "|" + safe(dimKey) + "|" + timeMs;
    }

    public static boolean isGroupMessage(String msg) {
        return msg != null && msg.startsWith(PREFIX);
    }

    public static String safe(String s) {
        if (s == null) return "";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String unsafe(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(s);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
