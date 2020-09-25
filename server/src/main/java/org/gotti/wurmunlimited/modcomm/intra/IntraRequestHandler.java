package org.gotti.wurmunlimited.modcomm.intra;

import com.wurmonline.communication.SocketConnection;

import java.nio.ByteBuffer;

/**
 * ModIntraServer request handler (receiving end)
 */
@FunctionalInterface
public interface IntraRequestHandler {

    /**
     * Handle the request
     *
     * @param connection Connection
     * @param recvBuffer Receive buffer
     */
    void handleRequest(SocketConnection connection,ByteBuffer recvBuffer);
}
