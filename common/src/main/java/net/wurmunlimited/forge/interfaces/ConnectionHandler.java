package net.wurmunlimited.forge.interfaces;

import org.gotti.wurmunlimited.modcomm.PacketWriter;

public interface ConnectionHandler {

    void sendPacket(final PacketWriter writer);
}
