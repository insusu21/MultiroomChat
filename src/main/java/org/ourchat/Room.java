package org.ourchat;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Room {

    private final String roomName;

    // 방에 참가한 클라이언트 목록 (동시성 문제 방지를 위해 Set 사용)
    private final Set<ClientSession> clients = new CopyOnWriteArraySet<>();

    public Room(String roomName) {
        this.roomName = roomName;
    }

    // 방에 클라이언트 추가
    public void join(ClientSession client) {
        clients.add(client);
    }

    // 방에서 클라이언트 제거
    public void leave(ClientSession client) {
        clients.remove(client);
    }

    // 방에 있는 모든 클라이언트에게 메시지 전송 (브로드캐스트)
    public void broadcast(String jsonMessage) {
        for (ClientSession client : clients) {
            client.sendMessage(jsonMessage);
        }
    }

    public Set<ClientSession> getClients() {
        // 읽기 전용으로 반환하거나(Collections.unmodifiableSet)
        // 그냥 반환할 수 있습니다.
        return clients;
    }

    public String getRoomName() {
        return roomName;
    }

    public boolean isEmpty() {
        return clients.isEmpty();
    }
}