package org.ourchat;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class ChatServer extends WebSocketServer {

    // 1. 생성자: 서버가 실행될 포트를 지정합니다.
    public ChatServer(int port) {
        super(new InetSocketAddress(port));
        System.out.println("채팅 서버가 " + port + " 포트에서 시작되었습니다.");
    }

    // 2. onOpen: 클라이언트가 접속했을 때
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("새 클라이언트 연결됨: " + conn.getRemoteSocketAddress());
    }

    // 3. onClose: 클라이언트 접속이 끊겼을 때
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("클라이언트 연결 끊김: " + conn.getRemoteSocketAddress());
    }

    // 4. onMessage: 클라이언트가 메시지를 보냈을 때 (핵심)
    @Override
    public void onMessage(WebSocket conn, String message) {
        // 지금은 받은 메시지를 그대로 콘솔에 출력만 합니다.
        System.out.println(conn.getRemoteSocketAddress() + " 로부터 온 메시지: " + message);

        // (선택 사항) 받은 메시지를 그대로 클라이언트에게 돌려보내기 (Echo)
        conn.send("서버가 메시지를 받았습니다: " + message);
    }

    // 5. onError: 오류 발생 시
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("오류 발생: " + ex.getMessage());
        ex.printStackTrace();
    }

    // 6. main 메서드: 서버를 실행합니다.
    public static void main(String[] args) {
        int port = 8080; // 8080 포트 사용
        ChatServer server = new ChatServer(port);
        server.start(); // 서버 스레드 시작
    }

    // 7. onStart: 서버가 성공적으로 시작되었을 때
    @Override
    public void onStart() {
        System.out.println("서버가 성공적으로 시작되었습니다!");
        // (필요시) 서버 시작 시 초기화 로직을 여기에 추가할 수 있습니다.
    }
}