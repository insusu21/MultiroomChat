package org.ourchat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class ChatServer extends WebSocketServer {

    private final SessionManager sessionManager;
    private final RoomManager roomManager;
    private final Gson gson;

    public ChatServer(int port, SessionManager sessionManager, RoomManager roomManager, Gson gson) {
        super(new InetSocketAddress(port));
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
        this.gson = gson;
    }

    @Override
    public void onStart() {
        System.out.println("서버가 포트 " + getPort() + "에서 성공적으로 시작되었습니다!");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sessionManager.createSession(conn); // '접수처'에서 세션 생성
        System.out.println("새 클라이언트 연결됨: " + conn.getRemoteSocketAddress());

        Message roomListMsg = new Message("ROOM_LIST", roomManager.getRoomNames());
        conn.send(roomListMsg.toJson(gson)); // 새로 로그인한 사람한테 리스트 전송
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientSession session = sessionManager.removeSession(conn); // '접수처'에서 세션 제거
        if (session != null) {
            boolean roomDeleted = roomManager.handleDisconnect(session); // '총괄 매니저'에게 연결 종료 알림

            if (roomDeleted) {
                broadcastRoomList();
            }
        }
        System.out.println("클라이언트 연결 끊김: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientSession session = sessionManager.getSession(conn);
        if (session == null) return;

        try {
            // 1. JSON 메시지를 Message 객체로 파싱
            Message msg = Message.fromJson(gson, message);

            // 2. 메시지 타입에 따라 '총괄 매니저'에게 작업 위임
            switch (msg.getType()) {
                case "SET_NICKNAME":
                    roomManager.setNickname(session, msg.getPayloadAsString());
                    break;
                case "JOIN_ROOM":
                    boolean newRoomCreated = roomManager.joinRoom(session, msg.getPayloadAsString());
                    if (newRoomCreated) {
                        broadcastRoomList();
                    }
                    break;
                case "CHAT_MESSAGE":
                    roomManager.handleChatMessage(session, msg.getPayloadAsString());
                    break;
                default:
                    Message error = new Message("ERROR", "알 수 없는 메시지 타입입니다.");
                    session.sendMessage(error.toJson(gson));
            }

        } catch (JsonSyntaxException e) {
            Message error = new Message("ERROR", "잘못된 JSON 형식입니다.");
            session.sendMessage(error.toJson(gson));
            System.err.println("잘못된 JSON 수신: " + message);
        } catch (Exception e) {
            Message error = new Message("ERROR", "서버 처리 중 오류가 발생했습니다.");
            session.sendMessage(error.toJson(gson));
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("오류 발생: " + ex.getMessage());
        ex.printStackTrace();

        // 오류 발생 시 해당 클라이언트 연결도 정리
        if (conn != null) {
            ClientSession session = sessionManager.removeSession(conn);
            if (session != null) {
                roomManager.handleDisconnect(session);
            }
        }
    }
    // 방 목록 갱신
    private void broadcastRoomList() {
        Message roomList = new Message("ROOM_LIST", roomManager.getRoomNames());
        sessionManager.broadcast(roomList.toJson(gson));
    }

    // --- 서버 실행 ---
    public static void main(String[] args) {
        int port = 8080;

        // 1. 모든 부품 생성
        Gson gson = new Gson();
        SessionManager sessionManager = new SessionManager();
        RoomManager roomManager = new RoomManager(gson);

        // 2. 서버 객체 생성
        ChatServer server = new ChatServer(port, sessionManager, roomManager, gson);

        // 3. 서버 시작
        server.start();
    }
}