package org.ourchat;

import org.java_websocket.WebSocket;

import java.util.HashSet;
import java.util.Set;

public class ClientSession {

    private final WebSocket connection;
    private String userId; // 닉네임
    private final Set<Room> joinedRooms = new HashSet<>();// 현재 접속한 방들

    public ClientSession(WebSocket connection) {
        this.connection = connection;
    }

    // 이 세션을 통해 클라이언트에게 JSON 메시지를 전송
    public void sendMessage(String jsonMessage) {
        if (connection.isOpen()) {
            connection.send(jsonMessage);
        }
    }

    // --- Getter/Setter ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void joinRoom(Room room) {
        joinedRooms.add(room);
    }

    public void leaveRoom(Room room) {
        joinedRooms.remove(room);
    }

    public Set<Room> getJoinedRooms() {
        return joinedRooms;
    }

    public WebSocket getConnection() {
        return connection;
    }
}