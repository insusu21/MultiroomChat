package org.ourchat;

import com.google.gson.Gson;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 모든 '방'을 관리하고, 실제 비즈니스 로직을 처리하는 '두뇌'
public class RoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Gson gson;

    public RoomManager(Gson gson) {
        this.gson = gson;
    }

    // 1. 닉네임 설정
    public void setNickname(ClientSession session, String nickname) {
        session.setUserId(nickname);
        // 클라이언트에게 성공 알림
        Message notice = new Message("SYSTEM_NOTICE", "닉네임이 " + nickname + "으로 설정되었습니다.");
        session.sendMessage(notice.toJson(gson));
    }

    // 2. 방 입장/생성
    public void joinRoom(ClientSession session, String roomName) {
        // 닉네임이 없으면 방에 참가할 수 없음
        if (session.getUserId() == null || session.getUserId().isEmpty()) {
            Message error = new Message("ERROR", "닉네임을 먼저 설정해야 합니다.");
            session.sendMessage(error.toJson(gson));
            return;
        }

        // 이미 다른 방에 있다면, 그 방에서 먼저 나옴
        leaveRoom(session);

        // 방을 찾거나, 없으면 새로 생성 (Thread-safe)
        Room room = rooms.computeIfAbsent(roomName, k -> new Room(roomName));

        // 방에 참가
        room.join(session);
        session.setCurrentRoom(room);

        // 1. 방 참가자 본인에게 알림
        Message welcome = new Message("SYSTEM_NOTICE", "'" + roomName + "' 방에 입장했습니다.");
        session.sendMessage(welcome.toJson(gson));

        // 2. 방에 있는 다른 사람들에게 알림
        Message joinNotice = new Message("SYSTEM_NOTICE", session.getUserId() + "님이 입장했습니다.");
        broadcastToRoom(session, joinNotice, false); // '나'를 제외하고 전송
    }

    // 3. 채팅 메시지 처리
    public void handleChatMessage(ClientSession session, String chatMessage) {
        Room room = session.getCurrentRoom();
        if (room == null) {
            Message error = new Message("ERROR", "방에 먼저 참가해야 합니다.");
            session.sendMessage(error.toJson(gson));
            return;
        }

        // 방에 있는 모든 사람에게 채팅 메시지 전송
        Message chat = new Message("NEW_MESSAGE",
                Map.of("sender", session.getUserId(), "message", chatMessage));

        broadcastToRoom(session, chat, true); // '나'를 포함하여 모두에게 전송
    }

    // 4. 연결 종료 처리
    public void handleDisconnect(ClientSession session) {
        if (session == null) return;

        // 방에서 나감
        leaveRoom(session);
    }

    // (내부 헬퍼) 방에서 나가는 로직
    private void leaveRoom(ClientSession session) {
        Room room = session.getCurrentRoom();
        if (room != null) {
            room.leave(session);
            session.setCurrentRoom(null);

            // 방에 남아있는 사람들에게 퇴장 알림
            Message leaveNotice = new Message("SYSTEM_NOTICE", session.getUserId() + "님이 퇴장했습니다.");
            broadcastToRoom(session, leaveNotice, false);

            // 방이 비었으면 방을 제거
            if (room.isEmpty()) {
                rooms.remove(room.getRoomName());
            }
        }
    }

    // (내부 헬퍼) 방에 메시지 브로드캐스트
    private void broadcastToRoom(ClientSession sender, Message message, boolean includeSelf) {
        Room room = sender.getCurrentRoom();
        if (room == null) return;

        String jsonMessage = message.toJson(gson);

        if (includeSelf) {
            room.broadcast(jsonMessage);
        } else {
            // 'sender'를 제외하고 모두에게 전송
            for (ClientSession client : room.getClients()) {
                if (client != sender) {
                    client.sendMessage(jsonMessage);
                }
            }
        }
    }
}