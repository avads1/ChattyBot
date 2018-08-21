ChattyBot is a chat application which handles multiple clients and enables clients to create chatrooms.

HOW TO RUN THE SERVER AND CLIENT .sh FILES - 

1) After extracting the zip, navigate to ChattBot->bin

2) Run the server.sh first. 
		a) Enter the port number. Ensure it is not in the range 0-1023.
		b) Enter the number of clients that can connect to the server
		
3) Run the client.sh next.
		a) Enter the hostname as "localhost"
		b) Enter the server port you wish to connect to. (Same port as entered in Step (2.a))
		
For a new client, run the client.sh again.


BASIC OVERVIEW -

The following features have been implemented:

1) Client connects to server and can type in commands listed in the commands section.

2) End user is prompted for user name.

3) User can create a chatroom. If the user creates a chatroom, it automatically joins the chatroom.

4) Client can see the list of chatrooms available.

5) User can join the chatrooms available

6) Users in the same chatroom can chat with each other. The message is broadcasted to all users in the same chatroom.

7) Users in a chat room are notified when a user joins or leaves the chatroom.

8) User can add another existing user to the chatroom.




COMMANDS -

1) create chatroom <chatRoomName> - To create a chatroom of given name

2) join <chatRoomName> - To join a chatroom of given name

3) list chatrooms - To see list of chatrooms

4) list users - To see list of users in a chatroom

5) add <userName> - To add a user to the chatroom. The users which have joined the chatroom can only add other users to the chatroom

6) leave - To leave a chatroom

7) quit - To exit ChattyBot

8) help - To see list of commands




