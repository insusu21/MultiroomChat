package org.ourchat;

import org.java_websocket.WebSocket;

public class ClientSession {

    private final WebSocket connection;
    private String userId; // 닉네임
    private Room currentRoom; // 현재 접속한 방

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

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    public WebSocket getConnection() {
        return connection;
    }
}