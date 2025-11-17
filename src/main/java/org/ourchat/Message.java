package org.ourchat;

import com.google.gson.Gson;
import java.util.Map;

// 서버와 클라이언트가 주고받을 메시지의 형식
public class Message {
    // 메시지 타입 (예: "SET_NICKNAME", "JOIN_ROOM", "CHAT_MESSAGE")
    String type;

    // 메시지 내용 (간단한 텍스트일 수도, 복잡한 객체일 수도 있음)
    Object payload;

    // (Gson 라이브러리가 사용하기 위한 기본 생성자)
    public Message() { }

    public Message(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // 객체를 JSON 문자열로 변환 (서버가 클라이언트에게 보낼 때)
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    // JSON 문자열을 객체로 변환 (서버가 클라이언트로부터 받을 때)
    public static Message fromJson(Gson gson, String json) {
        return gson.fromJson(json, Message.class);
    }

    // --- Getter (ProtocolHandler에서 사용) ---
    public String getType() {
        return type;
    }

    // 페이로드를 문자열로 간단히 가져오기
    public String getPayloadAsString() {
        return (String) payload;
    }

    // 페이로드를 Map(JSON 객체)으로 가져오기
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPayloadAsMap() {
        return (Map<String, Object>) payload;
    }
}