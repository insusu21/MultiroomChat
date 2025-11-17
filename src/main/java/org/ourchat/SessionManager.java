package org.ourchat;

import org.java_websocket.WebSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// WebSocket(연결)과 ClientSession(상태)을 연결해주는 관리자
public class SessionManager {

    private final Map<WebSocket, ClientSession> sessionMap = new ConcurrentHashMap<>();

    // 새 클라이언트 연결 시 세션 생성
    public ClientSession createSession(WebSocket conn) {
        ClientSession session = new ClientSession(conn);
        sessionMap.put(conn, session);
        return session;
    }

    // 클라이언트 연결 종료 시 세션 제거
    public ClientSession removeSession(WebSocket conn) {
        return sessionMap.remove(conn);
    }

    // 연결(conn)에 해당하는 세션(상태) 가져오기
    public ClientSession getSession(WebSocket conn) {
        return sessionMap.get(conn);
    }
}