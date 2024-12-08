package controller;

import model.ChatModel;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
	private ServerSocket server;
	private boolean done;
	private final ExecutorService pool;
	private final ChatModel model;

	public Server() {
		done = false;
		pool = Executors.newCachedThreadPool();
		model = new ChatModel();
	}

	@Override
	public void run() {
		try {
			server = new ServerSocket(1234);
			System.out.println("[SERVER LOG] Server started on port 1234.");

			while (!done) {
				Socket client = server.accept();
				ConnectionHandler handler = new ConnectionHandler(client);
				pool.execute(handler);
			}
		} catch (IOException e) {
			System.err.println("[SERVER ERROR] " + e.getMessage());
		} finally {
			shutdown();
		}
	}

	public void shutdown() {
		done = true;
		try {
			System.out.println("[SERVER LOG] Server shutting down...");
			model.notifyServerShutdown();
			if (!server.isClosed()) {
				server.close();
			}
			pool.shutdown();
		} catch (IOException e) {
			System.err.println("[SERVER ERROR] " + e.getMessage());
		}
	}

	public class ConnectionHandler implements Runnable {
		private final Socket client;
		private BufferedReader in;
		private PrintWriter out;
		private String nickname;

		public ConnectionHandler(Socket client) {
			this.client = client;
		}

	@Override
	public void run() {
		try {
		out = new PrintWriter(client.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out.println("Enter a nickname:");
		nickname = in.readLine();
		while (nickname == null || nickname.isBlank() || model.isNicknameTaken(nickname)) {
			out.println("Invalid or taken nickname. Enter another:");
			nickname = in.readLine();
		}
		model.addUser(nickname, this);
		model.broadcastMessage(nickname + " joined the chat.");
		model.addMessage("[JOIN] " + nickname);
		System.out.println("[SERVER LOG] " + nickname + " joined.");
		String message;
		while ((message = in.readLine()) != null) {
			if (message.startsWith("/nick ")) {
				String[] split = message.split(" ", 2);
				if (split.length == 2 && !model.isNicknameTaken(split[1])) {
					model.renameUser(nickname, split[1]);
					model.broadcastMessage(nickname + " renamed to " + split[1]);
					model.addMessage("[RENAME] " + nickname + " -> " + split[1]);
					System.out.println("[SERVER LOG] " + nickname + " renamed to " + split[1]);
					nickname = split[1];
					out.println("Nickname changed to " + nickname);
				} else {
					out.println("Invalid or taken nickname.");
					}
			} else if (message.equalsIgnoreCase("/quit")) {
				model.broadcastMessage(nickname + " left the chat.");
				model.addMessage("[LEAVE] " + nickname);
				System.out.println("[SERVER LOG] " + nickname + " left.");
				break;
			} else if (message.equalsIgnoreCase("/history")) {
				out.println("Chat History:");
				for (String chat : model.getChatHistory()) {
					out.println(chat);
				}
			} else {
				model.addMessage(nickname + ": " + message);
				model.broadcastMessage(nickname + ": " + message);
			}
		}
		} catch (IOException e) {
				System.err.println("[SERVER ERROR] " + e.getMessage());
		} finally {
				model.removeUser(nickname);
				shutdown();
			}
		}

		public void sendMessage(String message) {
			if (out != null) {
				out.println(message);
			}
		}

		public void shutdown() {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				if (client != null && !client.isClosed())
					client.close();
			} catch (IOException e) {
				System.out.println("[SERVER ERROR] Server disconnected.");
			}
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}
}
