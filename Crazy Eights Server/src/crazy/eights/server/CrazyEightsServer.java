/*
Author: Matthew Abney
Date: 1/30/20
Purpose: To make a game of crazy eights playable between two clients and a server
        using sockets. This will be the server the clients connect to.

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
package crazy.eights.server;

// Imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrazyEightsServer {
    
    // Variable Initialization
    private static final int PORT = 9090;
    
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(2);
    static int playerNumber = 0;
    static ArrayList<String> deck = new ArrayList<>();
    private static ArrayList<String> tempHand = new ArrayList<>();
    static ArrayList<String> playedDeck = new ArrayList<>();
    static int playerTurn = 0;
    
    public static void main(String[] args) throws IOException {
        
        // Deck Initialization
        deck = createDeck(deck);
        
        // Creates the server socket
        ServerSocket listener = new ServerSocket(PORT);
        
        // Allows the clients to connect
        while (true){
            if ( playerNumber < 2 ){
                
                tempHand.clear();
                int amountOfStartingCards = 5;
                for (int i = 0; i < amountOfStartingCards; i++) {
                    tempHand.add( deck.remove(0) );
                }
                for (int i = 0; i < tempHand.size(); i++) {
                    System.out.println(tempHand.get(i));
                }
                
                System.out.println("[SERVER] Waiting for client connection.");
                Socket client = listener.accept();
                System.out.println("[SERVER] Connected to client!");
                ClientHandler clientThread = new ClientHandler(client, playerNumber, tempHand, clients);
                playerNumber++;
                clients.add(clientThread);

                pool.execute(clientThread);
            }

        }
    }
    
    // Creates the cards in a deck
    public static ArrayList<String> createDeck(ArrayList<String> tempDeck){
        tempDeck.clear();
        String currentCard;
        String[] suits = {"C", "H", "S", "D"};
        int cardsInSuit = 13;
        
        
        for (int i = 0; i < suits.length; i++) {
            for (int j = 1; j <= cardsInSuit; j++) {
                currentCard = "";
                
                if ( j == 1 ){
                    currentCard += "A ";
                } else if ( j == 11 ){
                    currentCard += "J ";
                } else if ( j == 12 ){
                    currentCard += "Q ";
                } else if ( j == 13 ){
                    currentCard += "K ";
                } else {
                    currentCard += j +" ";
                }
                
                currentCard += suits[i];
                tempDeck.add(currentCard);
            }
        }
        
        tempDeck = randomizeDeck(tempDeck);
        return tempDeck;
    }
    
    public static ArrayList<String> randomizeDeck( ArrayList<String> tempDeck ){
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(tempDeck);
        }
        return tempDeck;
    }
}


// Thread for the clients to be seperate socket connections
class ClientHandler implements Runnable{
    
    // Variable Initialization
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private int playerNum;
    private ArrayList<String> playerHand; 
    private ArrayList<ClientHandler> clients;
    
    // Constructor
    public ClientHandler( Socket clientSocket, int playerNumb, ArrayList<String> hand, ArrayList<ClientHandler> clients ) throws IOException{
        this.client = clientSocket;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
        this.playerNum = playerNumb;
        this.playerHand = hand;
    }
    
    @Override
    public void run() {
        // So when the server closes it will break into the close everything properly
        try {
            
            if ( CrazyEightsServer.playerNumber == 2 ){
                outToAll("Both players connected.");
            }
            
            // Keeps the server going for the client to use
            while(true){
                
                // Gets the request from the client
                String request = in.readLine();
                
                // Makes the decision of what the client wants to do and tells them the response
                if ( CrazyEightsServer.playerNumber == 2 ){
                    String response = HandleRequest(request);
                    out.println(response);
                } else {
                    out.println("Second Player not connected yet.");
                }
                
            }
            
        } catch(IOException e) {
            // Prints an error message of what went wrong
            System.err.println("IO exception in client handler.");
            System.err.println(e.getStackTrace());
            
        } finally {
            // Closes everything
            out.close();
            try {
                client.close();
            } catch (IOException ex) {
                System.err.println(ex.getStackTrace());
            }
        }
    }
    
    private String HandleRequest( String request ){
        if ( request.contains("quit") ){
            outToAll("Player " + (playerNum+1) + " has left.");
            
        } else if ( request.startsWith("play") ){
            if ( request.indexOf(" ") != -1 ){
                String card = request.substring( request.indexOf(" ") ).trim();
                boolean played = playCard(card);
                if ( played ) {
                    outToAll("Player " + (playerNum+1) + " played " + card);
                    if ( CrazyEightsServer.playerTurn == 0 ){
                        CrazyEightsServer.playerTurn = 1;
                    } else {
                        CrazyEightsServer.playerTurn = 0;
                    }
                } else {
                    out.println("Can not play that card.");
                }
            }
            
        } else if ( request.startsWith("draw") ){
            if ( CrazyEightsServer.deck.size() < 1 ) {
                out.println("Can not draw from empty deck.");
                
                if ( CrazyEightsServer.playedDeck.size() < 1 ){
                    
                } else {
                    remakeDeckFromPlayed( CrazyEightsServer.playedDeck );
                }
            } else {
                playerHand.add( CrazyEightsServer.deck.remove(0));
            }
            
        } else if ( request.startsWith("hand") ){
            
        } else {
            out.println("Not a command.");
        }
        return "";
    }
    
    private boolean playCard( String card ){
        return true;    
    }
    
    private void remakeDeckFromPlayed( ArrayList<String> playedCards ){
        if ( playedCards.size() > 1 ){
            for (int i = 1; i < playedCards.size(); i++) {
                CrazyEightsServer.deck.add( playedCards.remove(i) );
            }
        }
    }
    
    private void outToAll( String msg ) {
        for ( ClientHandler aClient : clients ){
            aClient.out.println(msg);
        }
    }
}