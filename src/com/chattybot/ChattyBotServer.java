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

/*This connection is established as follows:
1) When the server receives a connection request on its specific server port, it creates a new socket for it and binds a port number to it.
2)It sends the new port number to the client to inform it that the connection is established.
3)The server goes on now by listening on two ports:
	a)it waits for new incoming connection requests on its specific port, and
	b)it reads and writes messages on established connection (on new port) with the accepted client*/

public class ChattyBotServer {

	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket clientSocket = null;

	/*
	 * This chat server can accept up to maxClientsCount clients' connections.
	 * private static final int maxClientsCount = 10; private static final
	 * clientThread[] threads = new clientThread[maxClientsCount];
	 */

	public static void main(String args[]) {
		// The default port number.
		int portNumber = 2222;
		if (args.length < 1) {
			System.out.println("ChattyBot server is up on " + portNumber);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			System.out.println("ChattyBot server is up on " + portNumber);
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
				/*
				 * Create a client socket for each connection and pass it to a
				 * new client thread. The server communicates with the client by
				 * reading from and writing to the new port. If other connection
				 * requests arrive, the server accepts them in the similar way
				 * creating a new port for each new connection. The
				 * communication with each client is done via the sockets
				 * created for each communication.
				 */

				/*
				 * When programming TCP and UDP based applications in Java,
				 * different types of sockets are used. These sockets are
				 * implemented in different classes. The classes ServerSocket
				 * and Socket implement TCP based sockets and the class
				 * DatagramSocket implements UDP based sockets as follows:
				 * 1)Stream socket to listen for client requests (TCP): the
				 * class ServerSocket. 2)Stream socket (TCP): the class Socket.
				 * 3)Datagram socket (UDP): the class DatagramSocket.
				 */
				clientSocket = serverSocket.accept();

				/*
				 * Now the server can send/receive data to/from the clients.
				 * Since the sockets are like the file descriptors the
				 * send/receive operations are implemented like read/write file
				 * operations on the input/output streams.
				 */
				/*
				 * it is not necessary to synchronize the portion 2 of code (see
				 * the table of the partitioned code). Even if this code
				 * modifies threads[] array, a better inspection of the code
				 * discovers that there is no risk this modification will create
				 * null pointer exceptions or other problems to the program.
				 */
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ClientThread(clientSocket, threads, chatRoomList, usersList)).start();
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
			System.out.println(
					"--------------------- Welcome to ChattyBot! ------------------\nEnter the number of clients supported");
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

	private static int setPortNumber() {
		int serverPort = 0;
		boolean isInputFormatWrong = false;
		Scanner sc;
		do {
			sc = new Scanner(System.in);
			System.out.println("Enter server port number \n");
			try {
				serverPort = sc.nextInt();
				// if(serverPort<0||(serverPort>=0&&serverPort<=1023)){
				// isInputFormatWrong = true;
				// }
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

/*
 * When a thread enters synchronized(this){}statement it blocks all other
 * threads from entering their synchronized(this){} statements. Thus, putting
 * all critical sections in synchronized(this){} statements we are guarantied
 * that the chat server will execute correctly without rising null pointer
 * exceptions caused by concurrent execution of other critical sections.
 */
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
			/*
			 * Usually, on the server side you need to close only the client
			 * socket after the client gets served. The server socket is kept
			 * open as long as the server is running. A new client can connect
			 * to the server on the server socket to establish a new connection,
			 * that is, a new client socket.
			 */
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
					createChatroom(command);
				} else if (command.startsWith(ChattyBotConstants.JOIN_CHATROOM_COMMAND)) {
					joinChatRoom(maxClientsCount, threads, userName, command);
				} else if (command.startsWith(ChattyBotConstants.QUIT_CHATBOT)) {
					os.println("=== Exiting ChattyBot. Come back soon, " + userName + " ===");
				} else if (command.startsWith(ChattyBotConstants.HELP)) {
					os.println(ChattyBotConstants.LIST_OF_COMMANDS_MAIN);
				} else {
					os.println(
							"** Invalid command. Please recheck the command or type \"help\" for list of commands. **");
				}
			}

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

	private void createChatroom(String command) {
		int startIndex = command.length() - ChattyBotConstants.CREATE_CHATROOM_COMMAND.length();
		String chatRoomName = command.substring(command.length() - startIndex).trim();
		if (chatRoomName.equals("") || chatRoomName.isEmpty()) {
			os.println("** Chat room name cannot be blank. Please enter valid name. **");
		} else {
			appChatRoomList.add(chatRoomName);
			os.println("== " + chatRoomName + " chatroom has been created. ==");
		}
	}

	private void joinChatRoom(int maxClientsCount, ClientThread[] threads, String name, String command)
			throws IOException {
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

			// Welcome the new the client to chatroom.
			os.println("##################################\nWelcome \"" + name + "\" to \"" + chatRoomName
					+ "\"###############################" + "\nTo exit chat room, type \"leave\"");
			synchronized (this) {
				// Public chat box
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this && threads[i].chatRoom != null) {
						if (threads[i].chatRoom.keySet().contains(chatRoomName))
							// if
							// (usersList.contains(threads[i].chatRoom.get(chatRoomName).get(0)))
							threads[i].os.println("=== " + name + " has entered the chat room. Say hi! ===");
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
								threads[i].os.println("=== The user " + name + " is leaving the chat room ===\n");
							}
						}
					}
					// Own client console
					os.println("=== Bye " + name
							+ " ===\n#################################################################");
					break;
				} else if (userInput.equalsIgnoreCase(ChattyBotConstants.HELP)) {
					os.println(ChattyBotConstants.LIST_OF_CHATROOM_COMMANDS);
				} else {
					broadcastMessage(maxClientsCount, threads, name, userInput);
				}
			}
		} else {
			os.println("** Chatroom doesnt exist. **");
		}
	}

	private void broadcastMessage(int maxClientsCount, ClientThread[] threads, String name, String userInput) {
		// The message is public, broadcast it to all other clients.
		synchronized (this) {
			for (int i = 0; i < maxClientsCount; i++) {
				if (threads[i] != null && threads[i].clientName != null) {
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
