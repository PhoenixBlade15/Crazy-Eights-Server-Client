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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Collections;

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
                
                // Creates a hand to allow the client to start the game with 5, or designated amount, cards
                tempHand.clear();
                int amountOfStartingCards = 5;
                for (int i = 0; i < amountOfStartingCards; i++) {
                    tempHand.add( deck.remove(0) );
                }
                for (int i = 0; i < tempHand.size(); i++) {
                    System.out.println(tempHand.get(i));
                }
                
                // Allows feedback on server side that client has connected, and starts their thread and waits for a second person to enter
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
        
        // Clears the deck to make sure nothing is in it then sets up the suits and amount of cards per suit
        tempDeck.clear();
        String currentCard;
        String[] suits = {"C", "H", "S", "D"};
        int cardsInSuit = 13;
        
        // Will go through all suits and all numbers assigning aces, jacks, queens, and kings as needed otherwise face value
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
        
        // Shuffles the deck then returns it
        tempDeck = randomizeDeck(tempDeck);
        return tempDeck;
    }
    
    // Shuffles the deck 10 times to make sure is randomized properly
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
    private static ArrayList<String> playerHand; 
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
            
            // Tells the clients two players are now in the game
            if ( CrazyEightsServer.playerNumber == 2 ){
                outToAll("Both players connected.");
            }
            
            // Keeps the server going for the client to use
            while(true){
                
                // Gets the request from the client
                String request = in.readLine();
                
                // Makes the decision of what the client wants to do and tells them the response
                if ( CrazyEightsServer.playerNumber == 2 ){
                    HandleRequest(request);
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
    
    // Will handle any requests the clients put in
    private void HandleRequest( String request ){
        
        // If the client wants to quit, will inform all other players
        if ( request.contains("quit") ){
            outToAll("Player " + (playerNum+1) + " has left.");
            
        // If they want to play a card
        } else if ( request.startsWith("play") ){
            
            // Make sure there is at least a space after the play
            if ( request.indexOf(" ") != -1 ){
                
                // Seperates the card from the request and removes any trailing or leading spaces
                String card = request.substring( request.indexOf(" ") ).trim();
                
                // Attempts to play the card returns true or false
                boolean played = playCard(card, this.playerHand);
                if ( played ) {
                    
                    // Checks if they have any cards remaining and if not inform all players
                    if ( this.playerHand.size() < 1 ){
                        outToAll("Player " + (playerNum+1) + " is out of cards!");
                        
                    } else {
                        
                        // tells all players who played what card and changes the player turn
                        outToAll("Player " + (playerNum+1) + " played " + card);
                        if ( CrazyEightsServer.playerTurn == 0 ){
                            CrazyEightsServer.playerTurn = 1;
                        } else {
                            CrazyEightsServer.playerTurn = 0;
                        }    
                    }
                    
                } else {
                    out.println("Can not play that card.");
                }
            }
            
        // If the client wants to draw a card
        } else if ( request.startsWith("draw") ){
            
            // Makes sure drawing a card is possible
            if ( CrazyEightsServer.deck.size() < 1 ) {
                out.println("Can not draw from empty deck.");
                
                if ( CrazyEightsServer.playedDeck.size() < 1 ){
                    out.println("No extra cards to draw.");
                } else {
                    remakeDeckFromPlayed( CrazyEightsServer.playedDeck );
                }
                
            // Adds a card from the deck to players hand
            } else {
                out.println( "You drew a " + CrazyEightsServer.deck.get(0) );
                playerHand.add( CrazyEightsServer.deck.remove(0));
            }
            
        // In case the client wants to see what cards are in their hand
        } else if ( request.startsWith("hand") ){
            String hand = createHandString( this.playerHand );
            out.println(hand);
            
        // If the client enters anything not properly
        } else {
            out.println("Not a command.");
        }
    }
    
    // Creates a string to show the user what cards are in their hand
    private String createHandString( ArrayList<String> playerHand ){
        String tempHand = "";
        for (int i = 0; i < playerHand.size(); i++) {
            tempHand += playerHand.get(i) + ", ";
        }
        tempHand = tempHand.substring(0,tempHand.length()-2);
        return tempHand;
    }
    
    // Plays checks if the card can be played and play it if so
    private boolean playCard( String card, ArrayList<String> playerHand ){
        
        // Is the card in their hand
        if ( !playerHand.contains(card) ){
            return false;
        }
        
        // Remove the card from their hand and add to the played pile
        playerHand.remove( playerHand.indexOf(card) );
        CrazyEightsServer.playedDeck.add(0, card);
        return true;    
    }
    
    // Remakes the deck from all played cards incase the deck runs out and someone needs to draw
    private void remakeDeckFromPlayed( ArrayList<String> playedCards ){
        if ( playedCards.size() > 1 ){
            for (int i = 1; i < playedCards.size(); i++) {
                CrazyEightsServer.deck.add( playedCards.remove(i) );
            }
        }
    }
    
    // Send a message to all clients in sockets
    private void outToAll( String msg ) {
        for ( ClientHandler aClient : clients ){
            aClient.out.println(msg);
        }
    }
}