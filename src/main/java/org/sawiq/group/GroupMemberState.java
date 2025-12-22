package org.sawiq.group;

import java.util.UUID;

public class GroupMemberState {
    public final UUID uuid;
    public String name;

    public int overworldX;
    public int overworldY;
    public int overworldZ;

    public int netherX;
    public int netherY;
    public int netherZ;

    public String lastDimensionKey;
    public long lastUpdateMs;

    public GroupMemberState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
