package org.ourchat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 멀티룸(동시 접속)을 지원하는 RoomManager
public class RoomManager {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Gson gson;

    public RoomManager(Gson gson) {
        this.gson = gson;
    }

    // 1. 닉네임 설정
    public void setNickname(ClientSession session, String nickname) {
        session.setUserId(nickname);
        Message notice = new Message("SYSTEM_NOTICE", "닉네임이 " + nickname + "으로 설정되었습니다.");
        session.sendMessage(notice.toJson(gson));
    }

    // 2. 방 입장/생성
    public boolean joinRoom(ClientSession session, String roomName) {
        if (session.getUserId() == null || session.getUserId().isEmpty()) {
            // 에러 처리 (생략 가능)
            return false;
        }

        boolean isNewRoom = !rooms.containsKey(roomName);
        Room room = rooms.computeIfAbsent(roomName, k -> new Room(roomName));

        // 이미 들어간 방인지 체크
        if (session.getJoinedRooms().contains(room)) {
            return false; // 이미 접속 중이면 무시
        }

        // 방 입장 처리
        room.join(session);
        session.joinRoom(room);

        // 알림 전송
        Message welcome = new Message("SYSTEM_NOTICE", "'" + roomName + "' 방에 입장했습니다.");
        session.sendMessage(welcome.toJson(gson));

        Message joinNotice = new Message("SYSTEM_NOTICE", "[" + roomName + "] " + session.getUserId() + "님이 입장했습니다.");
        broadcastToRoom(room, joinNotice);

        return isNewRoom;
    }

    // 3. 특정 방에서 나가기
    public boolean leaveSpecificRoom(ClientSession session, String roomName) {
        Room room = rooms.get(roomName);

        // 방이 존재하고, 내가 그 방에 있을 때만
        if (room != null && session.getJoinedRooms().contains(room)) {
            room.leave(session);
            session.leaveRoom(room); // 세션에서도 제거

            // 퇴장 알림
            Message leaveNotice = new Message("SYSTEM_NOTICE", "[" + roomName + "] " + session.getUserId() + "님이 퇴장했습니다.");
            broadcastToRoom(room, leaveNotice);

            // 방이 비었으면 삭제
            if (room.isEmpty()) {
                rooms.remove(roomName);
                return true; // 방 삭제됨
            }
        }
        return false;
    }

    // 4. 채팅 메시지 처리
    public void handleChatMessage(ClientSession session, String payloadJson) {
        try {
            // 예: { "targetRoom": "1번방", "message": "안녕하세요" }
            JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();
            String targetRoomName = json.get("targetRoom").getAsString();
            String msgContent = json.get("message").getAsString();

            Room room = rooms.get(targetRoomName);

            // 방이 존재하고, 내가 그 방에 멤버여야 메시지 전송 가능
            if (room != null && session.getJoinedRooms().contains(room)) {
                // 클라이언트가 화면에 그릴 수 있게 데이터 구조화
                // { "room": "1번방", "sender": "나", "message": "내용" }
                Map<String, String> chatData = Map.of(
                        "room", targetRoomName,
                        "sender", session.getUserId(),
                        "message", msgContent
                );

                Message chat = new Message("NEW_MESSAGE", chatData);
                room.broadcast(chat.toJson(gson));
            }
        } catch (Exception e) {
            System.err.println("메시지 전송 에러: " + e.getMessage());
            // 필요하다면 클라이언트에게 에러 메시지 전송
        }
    }

    // 5. 연결 종료 처리
    public boolean handleDisconnect(ClientSession session) {
        if (session == null) return false;

        boolean anyRoomDeleted = false;

        // 내가 들어간 '모든' 방을 돌면서 하나씩 퇴장 처리
        // (리스트 복사본을 만들어서 돌리는 것이 안전함)
        for (Room room : new java.util.ArrayList<>(session.getJoinedRooms())) {
            room.leave(session);
            session.leaveRoom(room);

            // 방이 비었으면 삭제
            if (room.isEmpty()) {
                rooms.remove(room.getRoomName());
                anyRoomDeleted = true;
            }

            // TODO: 연결 끊김 알림을 각 방에 보낼 수도 있음
        }
        return anyRoomDeleted; // 하나라도 방이 삭제되었으면 true
    }

    // 특정 Room 객체에 방송
    private void broadcastToRoom(Room room, Message message) {
        if (room != null) {
            room.broadcast(message.toJson(gson));
        }
    }

    // 방 목록 반환
    public java.util.Set<String> getRoomNames() {
        return rooms.keySet();
    }
}