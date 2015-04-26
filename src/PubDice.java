import java.net.Socket;
import java.util.Arrays;
import java.util.Observable;
import java.io.*;

/**
 * The Shut the Box Model. Manages all the data and the server connection.
 * Created by Isaac on 4/22/2015.
 */
public class PubDice extends Observable {

    private static final PubDice game = new PubDice();  // Let's use singleton.

    // Game state vars
    private String playerName;      // Name of the player assoc. with this client.
    private String opponent;        // Name of the enemy.
    private String playerNo;        // Player 1 or Player 2
    private boolean turn;           // Is it my turn?
    private boolean[] tiles;        // Live state of the tiles.
    private boolean[] lockedTiles;  // Tiles that have been locked in.
    private int tileSum;            // Sum of the visible tiles.
    private int[] dice;             // The two current dice values.
    private boolean rolled;         // Have the dice been rolled this round?
    private String[] players;       // Player names, in order.
    private String[] scores;        // Player scores, in order.
    private int roundScore;         // The current round (not turn) score
    private String winner;          // Winner winner.
    private boolean newGameTrigger; // Will rolling the dice be a new game?

    // Connection vars
    private Socket socket;          // Socket to the server
    BufferedReader in;              // Input buffer from server
    BufferedWriter out;             // Output buffer to server

    /**
     * Makes an instance of PubDice.
     * Since we're using singleton, this is private.
     */
    private PubDice() {
        tiles = new boolean[10];
        Arrays.fill(tiles, true);
        dice = new int[2];
        players = new String[2];
        scores = new String[2];
        tileSum = 45;
    }

    /**
     * @return the current tile state.
     */
    public static boolean[] getTiles() { return game.tiles; }

    /**
     * @return the current dice state.
     */
    public static int[] getDice() { return game.dice; }

    /**
     * @return if it's this client's turn.
     */
    public static boolean getTurn() { return game.turn; }

    /**
     * Processes the current game state and builds the message string
     * @return the message box string
     */
    public static String getPrintOut() {
        if(game.opponent != null) {
            String result = "";
            result += game.players[0];
            if(game.scores[0] != null)
                result += " " + game.scores[0];

            result += " -- ";
            result += game.players[1];
            if(game.scores[1] != null)
                result += " " + game.scores[1];

            if(game.winner != null) {
                result += " -- ";
                if ("0".equals(game.winner))
                    result += "Tie!";
                else if ("1".equals(game.winner))
                    result += game.players[0] + " wins!";
                else
                    result += game.players[1] + " wins!";
            }

            return result;
        } else return "Waiting for partner";
    }

    /**
     * Takes in the tokenized message from the server and updates the model.
     * @param mtokens - message from the server
     */
    private static void processServerMessage(String[] mtokens) {
        game.setChanged();

        if("joined".equals(mtokens[0])) {
            game.playerNo = mtokens[3];
            if("1".equals(game.playerNo)) {
                game.opponent = mtokens[2];
                game.players[0] = game.playerName;
                game.players[1] = game.opponent;
            } else {
                game.opponent = mtokens[1];
                game.players[0] = game.opponent;
                game.players[1] = game.playerName;
            }
            game.notifyObservers("joined");

        } else if ("turn".equals(mtokens[0])) {
            Arrays.fill(game.tiles, true);
            Arrays.fill(game.dice, 1);
            game.turn = mtokens[1].equals(game.playerNo);
            game.notifyObservers("turn tile dice");

        } else if ("tile".equals(mtokens[0])) {
            int tile = Integer.parseInt(mtokens[1]);
            boolean state = "up".equals(mtokens[2]);
            game.tiles[tile] = state;
            game.notifyObservers("tile");

        } else if ("dice".equals(mtokens[0])) {
            if(game.newGameTrigger) {
                resetGame();
                game.setChanged();
            }
            game.dice[0] = Integer.parseInt(mtokens[1]);
            game.dice[1] = Integer.parseInt(mtokens[2]);
            game.notifyObservers("dice");

        } else if ("score".equals(mtokens[0])) {
            int player = Integer.parseInt(mtokens[1]) - 1;
            game.scores[player] = mtokens[2];
            game.turn = !game.turn;
            game.notifyObservers("score");

        } else if ("win".equals(mtokens[0])) {
            game.winner = mtokens[1];
            game.newGameTrigger = true;
            game.notifyObservers("winner");

        } else if ("quit".equals(mtokens[0])) {
            try {
                game.out.close();
                game.socket.close();
                System.exit(0);
            } catch ( IOException ex ) {
                System.err.println(ex.toString());
                System.exit(1);
            }
        }
    }

    /**
     * Goes through the necessary steps to flip the tile.
     * If tile i is already locked in, this is effectively a no-op.
     * @param i - the number of the tile to flip.
     */
    public static void flipTile(int i) {
        if(game.lockedTiles[i]) return;

        String up = !game.tiles[i] ? "up" : "down";

        game.roundScore += (game.tiles[i] ? 1 : -1) * i;

        try {
            game.out.write("tile " + i + " " + up);
            game.out.newLine();
            game.out.flush();
        } catch(IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }

    /**
     * Rolls the dice. Locks in the currently flipped tiles.
     * Is effectively a no-op if this isn't a legal action.
     */
    public static void rollDice() {
        if(game.newGameTrigger) {
            resetGame();
        }
        if(!legalMove()) return;

        for(int i = 0; i < game.tiles.length; i++) {
            game.lockedTiles[i] = !game.tiles[i];
        }

        game.tileSum -= game.roundScore;
        game.roundScore = 0;
        game.rolled = true;

        try {
            game.out.write("roll");
            game.out.newLine();
            game.out.flush();
        } catch(IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }

    /**
     * Passes the turn and resets the board.
     * Is effectively a no-op if this isn't a legal action.
     */
    public static void passTurn() {
        if(!legalMove() && game.roundScore != 0) return;

        try {
            resetBoard();
            game.out.write("done");
            game.out.newLine();
            game.out.flush();
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }

    /**
     * Resets the board to a clean play state.
     */
    public static void resetBoard() {
        Arrays.fill(game.tiles, true);
        Arrays.fill(game.lockedTiles, false);
        Arrays.fill(game.dice, 1);
        game.roundScore = 0;
        game.tileSum = 45;
        game.rolled = false;
        game.setChanged();
        game.notifyObservers("tile dice");
    }

    /**
     * Resets the game to the very beginning.
     */
    public static void resetGame() {
        resetBoard();
        game.scores = new String[2];
        game.winner = null;
        game.setChanged();
        game.notifyObservers("score");
        game.newGameTrigger = false;
    }

    /**
     * @return whether a die roll or a turn pass is currently a legal action.
     */
    public static boolean legalMove() {
        if(game.tileSum > 6) {
            int dieTotal = game.dice[0] + game.dice[1];
            return game.roundScore == dieTotal || !game.rolled;
        } else {
            return game.roundScore == game.dice[0]
                    || game.roundScore == game.dice[1];
        }
    }

    /**
     * If I have time this will check if any legal moves exist
     * @return whether or not there are any other legal moves.
     */
    public static boolean allDone() {
        // TODO: Something. Anything.
        return false;
    }

    /**
     * Sends the appropriate quit message to the server.
     */
    public static void quit() {
        try {
            game.out.write("quit");
            game.out.newLine();
            game.out.flush();
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }


    /**
     * Connects to the server, starts the game, and listens for messages from the server.
     * @param args - the host, port, and player name - in that order.
     */
    public static void main(String[] args) {
        if(args.length != 3) {
            System.err.println("Usage: java PubDice <host> <port> <playername>");
            return;
        }

        game.playerName = args[2];

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            game.socket = new Socket(host, port);
            game.in = new BufferedReader(new InputStreamReader(game.socket.getInputStream()));
            game.out = new BufferedWriter(new OutputStreamWriter(game.socket.getOutputStream()));

        } catch (Exception e) {
            System.err.println("Unable to connect to host " + args[0] + " on port "
                    + args[1] + ":\n" + e.toString());
        }

        game.addObserver(new PubDiceController());

        try {
            game.out.write("join " + game.playerName);
            game.out.newLine();
            game.out.flush();

            while (true) {
                while(!game.in.ready());
                String s = game.in.readLine();
                System.out.println(s);

                // Spin off the processing into a new thread.
                final String[] mtokens = s.split(" ");
                processServerMessage(mtokens);
            }
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }
}
