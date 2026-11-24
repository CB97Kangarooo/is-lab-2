package ru.itmo.is.lab.realtime;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint("/ws/changes")
public class ChangeEndpoint {

    static final long MAX_IDLE_MS = 90_000;

    private static final Logger LOGGER = Logger.getLogger(ChangeEndpoint.class.getName());

    @Inject
    private ChangeBroadcaster broadcaster;

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(MAX_IDLE_MS);
        broadcaster.register(session);
    }

    @OnMessage
    public void onMessage(Session session, String text) {
        if (ChangeMessage.PING.equals(text)) {
            broadcaster.sendTo(session, ChangeMessage.PONG);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        broadcaster.unregister(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOGGER.log(Level.FINE, "WebSocket: ошибка сессии " + (session == null ? "?" : session.getId()), error);
        if (session != null) {
            broadcaster.unregister(session);
        }
    }
}
