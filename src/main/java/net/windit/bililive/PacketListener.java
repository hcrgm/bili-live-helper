package net.windit.bililive;

public interface PacketListener {
    void onPacket(LivePacket packet, LiveRoom room);
}