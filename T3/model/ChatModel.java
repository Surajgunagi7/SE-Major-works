package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import controller.Server.ConnectionHandler;

public class ChatModel {
    private final List<String> chatHistory; // Stores all chat messages
    private final HashMap<String, ConnectionHandler> activeUsers; // Maps nicknames to connections

    public ChatModel() {
        chatHistory = new ArrayList<>();
        activeUsers = new HashMap<>();
    }

    public synchronized void addUser(String nickname, ConnectionHandler handler) {
        activeUsers.put(nickname, handler);
    }

    public synchronized void removeUser(String nickname) {
        activeUsers.remove(nickname);
    }

    public synchronized boolean isNicknameTaken(String nickname) {
        return activeUsers.containsKey(nickname);
    }

    public synchronized void addMessage(String message) {
        chatHistory.add(message);
    }

    public synchronized List<String> getChatHistory() {
        return new ArrayList<>(chatHistory);
    }

    public synchronized void broadcastMessage(String message) {
        for (ConnectionHandler handler : activeUsers.values()) {
            handler.sendMessage(message);
        }
    }

    public synchronized void renameUser(String oldNickname, String newNickname) {
        ConnectionHandler handler = activeUsers.remove(oldNickname);
        if (handler != null) {
            activeUsers.put(newNickname, handler);
        }
    }

    public synchronized void notifyServerShutdown() {
        for (ConnectionHandler handler : activeUsers.values()) {
            handler.sendMessage("Server is shutting down. You will be disconnected.");
            handler.shutdown();
        }
        activeUsers.clear();
    }
}
