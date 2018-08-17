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

public class MultiThreadChatServerSync {

	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket clientSocket = null;

	// This chat server can accept up to maxClientsCount clients' connections.
	// private static final int maxClientsCount = 10;
	// private static final clientThread[] threads = new
	// clientThread[maxClientsCount];

	public static void main(String args[]) {

		// Scanner in = new Scanner(System.in);
		
		int maxClientsCount = setNumOfClientsSupported();
		List<String> chatRoomList = new ArrayList<String>();
		List<String> usersList = new ArrayList<String>();
		clientThread[] threads = new clientThread[maxClientsCount];
		// The default port number.
		int portNumber = 2222;
		if (args.length < 1) {
			System.out.println("ChattyBot server is up on " + portNumber);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
		}

		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a client socket for each connection and pass it to a new
		 * client thread.
		 */
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new clientThread(clientSocket, threads, chatRoomList, usersList)).start();
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
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
			System.out.println("Enter the number of clients supported \n");
			try {
				maxClientsCount = sc.nextInt();
				isInputFormatWrong = false;
			} catch (NoSuchElementException | IllegalStateException ex) {
				isInputFormatWrong = true;
				System.err.println("Invalid input type");
			}
		} while (isInputFormatWrong);
		sc.close();
		return maxClientsCount;
	}
	
	private static int setPortNumber() {
		int serverPort = 0;
		boolean isInputFormatWrong = false;
		Scanner sc;
		do {
			sc = new Scanner(System.in);
			System.out.println("Enter server port number \n");
			try {
				serverPort = sc.nextInt();
//				if(serverPort<0||(serverPort>=0&&serverPort<=1023)){
//					isInputFormatWrong = true;
//				}
				isInputFormatWrong = false;
			} catch (NoSuchElementException | IllegalStateException ex) {
				isInputFormatWrong = true;
				System.err.println("Invalid port number");
			}
		} while (isInputFormatWrong);
		sc.close();
		return serverPort;
	}
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class clientThread extends Thread {

	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxClientsCount;
	private List<String> appChatRoomList;
	private List<String> appUserList;
	private Map<String, List<String>> chatRoom;

	public clientThread(Socket clientSocket, clientThread[] threads, List<String> chatRoomList,
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
		clientThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 */
			is = new DataInputStream(clientSocket.getInputStream());
			os = new PrintStream(clientSocket.getOutputStream());
			String name;
			while (true) {
				os.println("Enter your name.");
				name = is.readLine().trim();
				if (name.indexOf('@') == -1) {
					appUserList.add(name);
					break;
				} else {
					os.println("The name should not contain '@' character.");
				}
			}

			while (true) {
				os.println(">>");
				String command = is.readLine().trim();
				if (command.startsWith(ChattyBotConstants.LIST_CHATROOMS_COMMAND)) {
					if (appChatRoomList.size() < 1) {
						os.println("There are no chatrooms created.   " + System.identityHashCode(appChatRoomList));
					} else {
						for (int i = 0; i < appChatRoomList.size(); i++) {
							os.println(i + 1 + ") " + appChatRoomList.get(i));
						}
					}
				} else if (command.startsWith(ChattyBotConstants.CREATE_CHATROOM_COMMAND)) {
					int startIndex = command.length() - ChattyBotConstants.CREATE_CHATROOM_COMMAND.length();
					String chatRoomName = command.substring(command.length() - startIndex).trim();
					if (chatRoomName.equals("") || chatRoomName.isEmpty()) {
						os.println("Chatroom name cannot be blank. Please enter valid name.");
					} else {
						appChatRoomList.add(chatRoomName);
						os.println(chatRoomName + " chatroom has been created");
					}
				} else if (command.startsWith(ChattyBotConstants.JOIN_CHATROOM_COMMAND)) {
					int startIndex = command.length() - ChattyBotConstants.JOIN_CHATROOM_COMMAND.length();
					String chatRoomName = command.substring(command.length() - startIndex).trim();
					if (appChatRoomList.contains(chatRoomName)) {
						List<String> usersList = null;
						if (this.chatRoom == null) {
							this.chatRoom = new HashMap<String, List<String>>();
							usersList = new ArrayList<String>();
							usersList.add(name);
							this.chatRoom.put(chatRoomName, usersList);
						}

						// os.println("Users list " +
						// System.identityHashCode(usersList));
						// Setting the name of current client thread.
						this.setName(name);
						/* Welcome the new the client. */
						os.println("===============================================================\nWelcome " + name
								+ " to chat room " + chatRoomName
								+ "\nTo exit chat room enter \"leave\" in a new line.");
						synchronized (this) {
							for (int i = 0; i < maxClientsCount; i++) {
								if (threads[i] != null && threads[i] == this) {
									clientName = "@" + name;
									break;
								}
							}
							// Public chat box
							// for (int i = 0; i < maxClientsCount; i++) {
							// if (threads[i] != null && threads[i] != this &&
							// threads[i].chatRoom != null) {
							// if
							// (usersList.contains(threads[i].chatRoom.get(chatRoomName).get(0)))
							// threads[i].os
							// .println("*** A new user " + name + " entered the
							// chat room !!! ***");
							// }
							// }
						}

						/* Start the conversation. */
						while (true) {
							os.println("->");
							String userInput = is.readLine();
							if (userInput.startsWith(ChattyBotConstants.LIST_USERS)) {
								for (int i = 0; i < maxClientsCount; i++) {
									if (threads[i] != null && threads[i].chatRoom != null
											&& threads[i].chatRoom.containsKey(chatRoomName)) {
										List<String> chatRoomUserList = threads[i].chatRoom.get(chatRoomName);
										for (int j = 0; j < chatRoomUserList.size(); j++)
											os.println(chatRoomUserList.get(j));
									}
								}
							} else if (userInput.startsWith(ChattyBotConstants.ADD_USER)) {
								startIndex = userInput.length() - ChattyBotConstants.ADD_USER.length();
								String userName = userInput.substring(userInput.length() - startIndex).trim();
								if (appUserList.contains(userName)) {
									usersList.add(userName);
									this.chatRoom.put(chatRoomName, usersList);
								}else{
									os.println("User does not exist in ChattyBot");
								}
							} else if (userInput.equalsIgnoreCase(ChattyBotConstants.LEAVE_COMMAND)) {
								usersList.remove(name);
								synchronized (this) {
									// Public chat box
									for (int i = 0; i < maxClientsCount; i++) {
										// threads[i] != null && threads[i] !=
										// this means that thread shouldnt be
										// null and
										// broadcast message to every client
										// except the current client that has
										// sent a cmd/request
										if (threads[i] != null && threads[i] != this) {
											threads[i].os.println(
													"*** The user " + name + " is leaving the chat room !!! ***\n");
										}
									}
								}
								// Own client console
								os.println("*** Bye " + name
										+ " ***\n===============================================================");
								break;
							} else {

								/*
								 * If the message is private sent it to the
								 * given client.
								 */
								if (userInput.startsWith("@")) {
									String[] words = userInput.split("\\s", 2);
									if (words.length > 1 && words[1] != null) {
										words[1] = words[1].trim();
										if (!words[1].isEmpty()) {
											synchronized (this) {
												for (int i = 0; i < maxClientsCount; i++) {
													if (threads[i] != null && threads[i] != this
															&& threads[i].clientName != null
															&& threads[i].clientName.equals(words[0])) {
														threads[i].os.println("<" + name + "> " + words[1]);
														/*
														 * Echo this message to
														 * let the client know
														 * the private message
														 * was sent.
														 */
														this.os.println(">" + name + "> " + words[1]);
														break;
													}
												}
											}
										}
									}
								} else {
									/*
									 * The message is public, broadcast it to
									 * all other clients.
									 */
									synchronized (this) {
										for (int i = 0; i < maxClientsCount; i++) {
											if (threads[i] != null && threads[i].clientName != null) {
												threads[i].os.println("<" + name + "> " + userInput);
											}
										}
									}
								}
							}
						}
					} else {
						os.println("Chatroom doesnt exist.");
					}
				} else if (command.startsWith(ChattyBotConstants.QUIT_CHATBOT)) {
					os.println("*** Exiting ChattyBot. Come back soon, " + name + " ***");
				} else {
					os.println("Invalid command. Please enter a recheck the command");
				}
			}

		} catch (IOException e) {
		}
	}
}

/*
 * Clean up. Set the current thread variable to null so that a new client could
 * be accepted by the server.
 */
// synchronized (this) {
// for (int i = 0; i < maxClientsCount; i++) {
// if (threads[i] == this) {
// threads[i] = null;
// }
// }
// }
// /*
// * Close the output stream, close the input
// * stream, close the socket.
// */
// is.close();
// os.close();
// clientSocket.close();
