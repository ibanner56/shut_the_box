import java.net.Socket;
import java.util.Arrays;
import java.util.Observable;
import java.io.*;

/**
 * The Shut the Box Model. Manages all the data and the server connection.
 * Created by Isaac on 4/22/2015.
 */
public class PubDice extends Observable {

    private static final PubDice game = new PubDice();

    private String playerName;
    private String opponent;
    private String playerNo;
    private boolean turn;
    private boolean[] tiles;
    private int[] dice;
    private String[] players;
    private String[] scores;
    private int roundScore;
    private String winner;
    private boolean newGameTrigger;

    private Socket socket;
    BufferedReader in;
    BufferedWriter out;

    private PubDice() {
        tiles = new boolean[10];
        Arrays.fill(tiles, true);
        dice = new int[2];
        players = new String[2];
        scores = new String[2];
    }

    public static boolean[] getTiles() { return game.tiles; }
    public static int[] getDice() { return game.dice; }
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

    public static void flipTile(int i) {
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

    public static void rollDice() {
        int dieTotal = game.dice[0] + game.dice[1];
        System.out.println(dieTotal);
        if(game.roundScore != dieTotal) return;

        game.roundScore = 0;

        if(game.newGameTrigger) {
            resetGame();
        }
        try {
            game.out.write("roll");
            game.out.newLine();
            game.out.flush();
        } catch(IOException ex) {
            System.err.println(ex.toString());
            System.exit(1);
        }
    }

    public static void passTurn() {
        int dieTotal = game.dice[0] + game.dice[1];
        if(game.roundScore != dieTotal) return;

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

    public static void resetBoard() {
        Arrays.fill(game.tiles, true);
        Arrays.fill(game.dice, 0);
        game.roundScore = 0;
        game.setChanged();
        game.notifyObservers("tile dice");
    }

    public static void resetGame() {
        resetBoard();
        game.scores = new String[2];
        game.winner = null;
        game.setChanged();
        game.notifyObservers("score");
        game.newGameTrigger = false;
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
