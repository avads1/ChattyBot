package com.chattybot;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ChattyBotServer {

	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket clientSocket = null;

	public static void main(String args[]) {
		// The default port number.
		int portNumber = 3333;

		if (args.length < 1) {
			System.out.println(
					"--------------------- Welcome to ChattyBot! ------------------\nChattyBot server is up on "
							+ portNumber + ". Clients can connect now ...");
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			System.out.println(
					"--------------------- Welcome to ChattyBot! ------------------\nChattyBot server is up on "
							+ portNumber + ". Clients can connect now ...");
		}
		int maxClientsCount = setNumOfClientsSupported();
		List<String> chatRoomList = new ArrayList<String>();
		List<String> usersList = new ArrayList<String>();
		ClientThread[] threads = new ClientThread[maxClientsCount];

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		// After server socket is created, run a infinite while loop
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						threads[i] = new ClientThread(clientSocket, threads, chatRoomList, usersList);
						threads[i].start();
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("** Server is busy. Please try later. **");
					os.close();
					clientSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}

	private static int setNumOfClientsSupported() {
		int maxClientsCount = 0;
		boolean isInputFormatWrong = false;
		Scanner sc;
		do {
			sc = new Scanner(System.in);
			System.out.println("Enter the number of clients supported");
			try {
				maxClientsCount = sc.nextInt();
				isInputFormatWrong = false;
			} catch (NoSuchElementException | IllegalStateException ex) {
				isInputFormatWrong = true;
				System.err.println("** Invalid input type. **");
			}
		} while (isInputFormatWrong);
		sc.close();
		return maxClientsCount;
	}
}

class ClientThread extends Thread {

	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final ClientThread[] threads;
	private int maxClientsCount;
	private List<String> appChatRoomList;
	private List<String> appUserList;
	private Map<String, List<String>> chatRoom;

	public ClientThread(Socket clientSocket, ClientThread[] threads, List<String> chatRoomList,
			List<String> usersList) {
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxClientsCount = threads.length;
		appChatRoomList = chatRoomList;
		appUserList = usersList;
	}

	@SuppressWarnings("deprecation")
	public void run() {
		int maxClientsCount = this.maxClientsCount;
		ClientThread[] threads = this.threads;

		try {
			// Create input and output streams for this client. DataInputStream
			// is used to receive inputs from the client.
			is = new DataInputStream(clientSocket.getInputStream());
			// Using the class PrintStream to send data to the client
			os = new PrintStream(clientSocket.getOutputStream());
			String userName;
			os.println("Enter your name.");
			userName = is.readLine().trim();
			appUserList.add(userName);

			while (true) {
				os.println(">>");
				String command = is.readLine().trim();
				if (command.startsWith(ChattyBotConstants.LIST_CHATROOMS_COMMAND)) {
					listChatrooms();
				} else if (command.startsWith(ChattyBotConstants.CREATE_CHATROOM_COMMAND)) {
					int startIndex = command.length() - ChattyBotConstants.CREATE_CHATROOM_COMMAND.length();
					String chatRoomName = command.substring(command.length() - startIndex).trim();
					createChatroom(chatRoomName, userName);
					command = "join " + chatRoomName;
					joinChatRoom(maxClientsCount, threads, userName, command);
				} else if (command.startsWith(ChattyBotConstants.JOIN_CHATROOM_COMMAND)) {
					joinChatRoom(maxClientsCount, threads, userName, command);
				} else if (command.startsWith(ChattyBotConstants.QUIT_CHATBOT)) {
					os.println("=== Exiting ChattyBot. Come back soon, " + userName + " ===");
					break;
				} else if (command.startsWith(ChattyBotConstants.HELP)) {
					os.println(ChattyBotConstants.LIST_OF_COMMANDS_MAIN);
				} else {
					os.println(
							"** Invalid command. Please recheck the command or type \"help\" for list of commands. **");
				}
			}
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the
			 * socket.
			 */
			is.close();
			os.close();
			clientSocket.close();

		} catch (IOException e) {
		}
	}

	private void listChatrooms() {
		if (appChatRoomList.size() < 1) {
			os.println("** There are no chat rooms created. **");
		} else {
			for (int i = 0; i < appChatRoomList.size(); i++) {
				os.println(i + 1 + ") " + appChatRoomList.get(i));
			}
		}
	}

	private void createChatroom(String chatRoomName, String userName) {
		if (chatRoomName.equals("") || chatRoomName.isEmpty()) {
			os.println("** Chat room name cannot be blank. Please enter valid name. **");
		} else {
			appChatRoomList.add(chatRoomName);
			synchronized (this) {
				if (this.chatRoom == null) {
					this.chatRoom = new HashMap<String, List<String>>();
					List<String> usersList = new ArrayList<String>();
					usersList.add(userName);
					this.chatRoom.put(chatRoomName, usersList);
				}
			}
			os.println("== " + chatRoomName + " chatroom has been created. ==");
		}
	}

	private void joinChatRoom(int maxClientsCount, ClientThread[] threads, String userName, String command)
			throws IOException {
		int startIndex = command.length() - ChattyBotConstants.JOIN_CHATROOM_COMMAND.length();
		String chatRoomName = command.substring(command.length() - startIndex).trim();
		if (appChatRoomList.contains(chatRoomName)) {
			List<String> usersList = null;
			synchronized (this) {
				if (this.chatRoom == null) {
					this.chatRoom = new HashMap<String, List<String>>();
					usersList = new ArrayList<String>();
					usersList.add(userName);
					this.chatRoom.put(chatRoomName, usersList);
				}
			}

			// Welcome the new the client to chatroom.
			os.println("################################## Welcome \"" + userName + "\" to \"" + chatRoomName
					+ "\" ###############################" + "\nTo exit chat room, type \"leave\"");
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = userName;
						break;
					}
				}
				// Public chat box
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].chatRoom != null) {
						if (threads[i].chatRoom.keySet().contains(chatRoomName)) {
							threads[i].os.println("=== " + userName + " has entered the chat room. Say hi! ===");
						}
					}
				}
			}

			// Start the conversation.
			while (true) {
				os.println("->");
				String userInput = is.readLine();
				if (userInput.startsWith(ChattyBotConstants.LIST_USERS)) {
					listUsers(maxClientsCount, threads, chatRoomName);
				} else if (userInput.startsWith(ChattyBotConstants.ADD_USER)) {
					addUser(chatRoomName, usersList, userInput);
				} else if (userInput.equalsIgnoreCase(ChattyBotConstants.LEAVE_COMMAND)) {
					usersList = this.chatRoom.get(chatRoomName);
					usersList.remove(userName);
					synchronized (this) {
						// Public chat box
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null && threads[i] != this && threads[i].chatRoom.keySet().contains(chatRoomName)) {
								threads[i].os.println("=== The user " + userName + " is leaving the chat room ===\n");
							}
						}
					}
					// Own client console
					os.println("####################################### Bye " + userName
							+ " ############################");
					this.chatRoom.remove(chatRoomName);
					break;
				} else if (userInput.equalsIgnoreCase(ChattyBotConstants.HELP)) {
					os.println(ChattyBotConstants.LIST_OF_CHATROOM_COMMANDS);
				} else {
					broadcastMessage(maxClientsCount, threads, userName, userInput, chatRoomName);
				}
			}
		} else {
			os.println("** Chatroom doesnt exist. **");
		}
	}

	private void broadcastMessage(int maxClientsCount, ClientThread[] threads, String name, String userInput, String chatRoomName) {
		// The message is public, broadcast it to all other clients.
		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i].clientName != null && threads[i].chatRoom.keySet().contains(chatRoomName)) {
					threads[i].os.println("{" + name + "} " + userInput);
				}
			}
		}
	}

	private void addUser(String chatRoomName, List<String> usersList, String userInput) {
		int startIndex;
		startIndex = userInput.length() - ChattyBotConstants.ADD_USER.length();
		String userName = userInput.substring(userInput.length() - startIndex).trim();
		if (appUserList.contains(userName)) {
			usersList.add(userName);
			this.chatRoom.put(chatRoomName, usersList);
		} else {
			os.println("** User does not exist in ChattyBot. **");
		}
	}

	private void listUsers(int maxClientsCount, ClientThread[] threads, String chatRoomName) {
		for (int i = 0; i < maxClientsCount; i++) {
			if (threads[i] != null && threads[i].chatRoom != null && threads[i].chatRoom.containsKey(chatRoomName)) {
				List<String> chatRoomUserList = threads[i].chatRoom.get(chatRoomName);
				for (int j = 0; j < chatRoomUserList.size(); j++)
					os.println(chatRoomUserList.get(j));
			}
		}
	}
}
