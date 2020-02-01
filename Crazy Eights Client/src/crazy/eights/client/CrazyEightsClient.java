/*
Author: Matthew Abney
Date: 1/30/20
Purpose: To make a game of crazy eights playable between two clients and a server
        using sockets. This will be the client program to connect to server.

Due 2/4:

Create a client and server to allow two players to play a game of crazy eights
https://en.wikipedia.org/wiki/Crazy_Eights
You may assume that there will always be exactly two players.  
You will have to create an appropriate application-layer protocol as part of the assignment.  
The server should check that all actions are legal, and keeps track of the current score. Assume each player is a separate client.

Additional Rule(s):

If the start card turned up to start the game is an eight, put it back in the deck,  reshuffle deck, and draw a new start  card (repeat as necessary until a non-eight card starts the pile)
Submit the source code for both the client and server.

*/
package crazy.eights.client;

// Imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CrazyEightsClient {
    
    // Variable Initialization
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9090;
    
    public static void main(String[] args) throws IOException {
        
        // Connects the client to the server socket
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        
        // Allows the client to get the data from server
        BufferedReader input = new BufferedReader(new InputStreamReader( socket.getInputStream()));
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        String serverResponse;
        
        // Prints a menu for the client to follow
        System.out.println("Command Menu (Capitalization Sensitive): \n"
                       + "'quit' to exit \n"
                       + "'play K S' to play King of Spades or 'play 3 H' for 3 of hearts \n"
                       + "'draw' will draw a card from the deck \n"
                       + "'hand' to show what cards are in your hand \n"
                       + "Cards are their number or A, Q, J, K followed by a space and then suit ");
        System.out.println();
        
        // Keeps the client going as long as they don't type quit
        while (true){
            
            // Gets the message from server
            serverResponse = input.readLine();
            System.out.println("Server says: " + serverResponse);
            System.out.println();
            
            // Allows client to type a message to server
            System.out.print("> ");
            String command = keyboard.readLine();
            
            // Sends the server their message
            out.println(command);
            
            // If they want to quit
            if( command.equals("quit")){
                break;
            }
            
        }
        
        // Closes everything
        out.close();
        input.close();
        socket.close();
        System.exit(0);
    }
    
}
