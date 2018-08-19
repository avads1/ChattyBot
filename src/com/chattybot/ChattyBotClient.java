package com.chattybot;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 * The code below is the multi-threaded chat client. 
 * It uses two threads: 
 * one to read the data from the standard input and to sent it to the server, 
 * the other to read the data from the server and to print it on the standard output.
 * 
 */
public class ChattyBotClient implements Runnable {

	// The client socket
	private static Socket clientSocket = null;
	// The output stream
	private static PrintStream os = null;
	// The input stream
	private static DataInputStream is = null;

	private static BufferedReader inputLine = null;
	private static boolean closed = false;

	public static void main(String[] args) {
		// The default port.
		int portNumber = 2222;
		int MAX_SIZE = 1048;
		int count = 0;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out.println("--------------------- Welcome to ChattyBot! ------------------\nServer name: " + host
					+ ", portNumber: " + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
			System.out.println("ChattyBot server is up on args" + portNumber);
		}

		// Open a socket on a given host and port. Open input and output
		// streams.

		try {
			clientSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			/*
			 * On the client side, an output stream must be created to send the
			 * data to the server socket using the classPrintStream or
			 * DataOutputStream
			 */
			os = new PrintStream(clientSocket.getOutputStream());
			/*
			 * Using the DataInputStream class to create an input stream to
			 * receive responses from the server
			 */
			is = new DataInputStream(clientSocket.getInputStream());
		} catch (UnknownHostException e) {
			System.err.println("** Host " + host + " is either unavailable or doesnt exist **");
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host " + host);
		}

		// If everything has been initialized then we want to write some data to
		// the socket we have opened a connection to on the port portNumber.

		if (clientSocket != null && os != null && is != null) {
			try {

				// Create a thread to read from the server.
				new Thread(new ChattyBotClient()).start();
				while (!closed) {
					os.println(inputLine.readLine().trim());
				}

				// Close the output stream, close the input stream, close the
				// socket.
				os.close();
				is.close();
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}

	private static String setHostName() {
		String hostName = "";
		boolean isInputFormatWrong = false;
		Scanner in;
		do {
			in = new Scanner(System.in);
			System.out.println("Enter hostname\n");
			try {
				hostName = in.nextLine();
				isInputFormatWrong = false;
			} catch (NoSuchElementException | IllegalStateException ex) {
				isInputFormatWrong = true;
				System.err.println("Invalid input type");
			}
		} while (isInputFormatWrong);
		in.close();
		return hostName;
	}

	private static int setPortNumber() {
		int serverPort = 0;
		boolean isInputFormatWrong = false;
		Scanner in;
		do {
			in = new Scanner(System.in);
			System.out.println("Enter server port number \n");
			try {
				serverPort = in.nextInt();
				if (serverPort < 0 || (serverPort >= 0 && serverPort <= 1023)) {
					isInputFormatWrong = true;
				}
				isInputFormatWrong = false;
			} catch (NoSuchElementException | IllegalStateException ex) {
				isInputFormatWrong = true;
				System.err.println("Invalid port number");
			}
		} while (isInputFormatWrong);
		in.close();
		return serverPort;
	}

	// Create a thread to read from the server.

	@SuppressWarnings("deprecation")
	public void run() {
		// Keep on reading from the socket till we receive "Bye" from the
		// server. Once we received that then we want to break.

		String responseLine;
		try {
			while ((responseLine = is.readLine()) != null) {
				System.out.println(responseLine);
				if (responseLine.indexOf("*** Exiting") != -1)
					break;
			}
			closed = true;
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}