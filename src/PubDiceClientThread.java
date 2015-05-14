import java.io.IOException;
import java.io.EOFException;
import java.util.Random;
import java.util.Arrays;

/**
 * Thread to handle all game state and connection information for a client.
 * @author Isaac Banner
 */
public class PubDiceClientThread extends Thread {
	private PubDiceClientConnection client;
	private String name;
	private int playerNo;
	private boolean quit;
	private boolean[] tiles;
	private int[] scores;

	/**
	 *	Creates a new player thread wrapped around a client connection.
	 *	@param client - the client connection for the player
	 */
	public PubDiceClientThread(PubDiceClientConnection client) {
		super();
		this.client = client;
		this.name = "";
		this.playerNo = -1;
		this.quit = false;
		this.tiles = new boolean[10];
		Arrays.fill(tiles, true);
		this.scores = new int[2];
	}

	/**
	 * Parse messages forever.
	 */
	public void run() {
		try{
			while(!quit) {
				String message = client.read();
				processMessage(message);
			}
		} catch (EOFException ex) {
			// A client disconnected incorrectly. TODO: Consider printing error.	
			quit();
		} catch(IOException ex) {
			if(!quit)
				System.err.println(ex);
		}
	}

	/**
	 *	Takes a message and executes the proper actions for that message.
	 *	@param message - The message from the client.
	 */
	public void processMessage(String message) {
		String[] mtokens = message.split(" ");
		if("join".equals(mtokens[0])) {
			name = mtokens[1];
			PubDicePlayerManager.join(this);
		} else if("tile".equals(mtokens[0])) {
			try {
				int tile = Integer.parseInt(mtokens[1]);
				setTile(tile, mtokens[2]);
			} catch (Exception e) {

			}
		} else if("roll".equals(mtokens[0])) {
			rollDice();
		} else if("done".equals(mtokens[0])) {
			endTurn();
		} else if("quit".equals(mtokens[0])) {
			quit();
		}
	}

	/**
	 *	Sends a message through the client connection.
	 *	@param message - the message to send
	 */
	private void sendMessage(String message) {
		try {
			client.write(message);
		} catch (IOException ex) {
			if(!quit)
				System.err.println(ex);
		}
	}

	/**
	 * Connects this player to an opponent.
	 * @param playerNo - This player's player number.
	 */
	public void pairUp(int playerNo) {
		this.playerNo = playerNo;
		String player1 = this.name;
		String player2 = PubDicePlayerManager.getPartner(this).getPlayerName();
		if(playerNo == 2) {
			String temp = player1;
			player1 = player2;
			player2 = temp;
		} 
		sendMessage("joined " + player1 + " " + player2 + " " + playerNo);
	}
	
	/**
	 * Sets the turn
	 * @param turnNo - the turn number
	 */
	public void turn(int turnNo) {
		turn(turnNo, this);
	}

	/**
	 * Sets the turn
	 * @param turnNo - the turn number
	 * @param sender - recursive stop gap - first thread to call this.
	 */
	public void turn(int turnNo, PubDiceClientThread sender) {
		sendMessage("turn " + turnNo);
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		if(partner != sender) partner.turn(turnNo, sender);
	}

	/**
	 * Quits the game. Tells its partner to quit too.
	 */
	public void quit() {
		this.quit = true;
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		PubDicePlayerManager.quit(this);
		if(partner != null) partner.quit();
		sendMessage("quit");
	}
	
	/**
	 * Sets a tile.
	 * @param tile - the time number to set
	 * @param dir - the direction to set it - up or down.
	 */
	public void setTile(int tile, String dir) {
		setTile(tile, dir, this);
	}

	/**
	 * Sets a tile.
	 * @param tile - the time number to set
	 * @param dir - the direction to set it - up or down.
	 * @param sender - recursive stop gap - first thread to call this.
	 */
	public void setTile(int tile, String dir, 
			PubDiceClientThread sender) {
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		if(partner != sender) partner.setTile(tile, dir, sender);
		sendMessage("tile " + tile + " " + dir);	
		tiles[tile] = "up".equals(dir);	
	}

	/**
	 * Rolls the dice and then tells its partner what the dice are.
	 */
	public void rollDice() {
		Random roll = new Random(System.currentTimeMillis());
		int _1d6 = roll.nextInt(5) + 1;
		int _2d6 = roll.nextInt(5) + 1;
		sendRoll(_1d6, _2d6);
		
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		partner.sendRoll(_1d6, _2d6);
	}

	/**
	 * Takes in the dice and tells the client the roll.
	 */
	public void sendRoll(int _1d6, int _2d6) {
		sendMessage("dice " + _1d6 + " " + _2d6);
	}

	/**
	 * Ends the turn. Scores and resets the board.
	 */
	public void endTurn() {
		endTurn(playerNo, this);
	}
	
	/**
	 * Ends the turn. Scores and resets the board. Tells its partner the turn 
	 * ended.
	 *
	 * @param playerNo - the player who just finished their turn
	 * @param sender - recursive stop gap - the thread that first called this
	 */
	public void endTurn(int playerNo, PubDiceClientThread sender) {
		int score = 0;
		for(int i = 0; i < 10; i++) {
			if(tiles[i]) score += i;
			tiles[i] = true;
		}
		scores[playerNo - 1] = score;

		sendMessage("score " + playerNo + " " + score);
		if(playerNo == 2) {
			int winner = (scores[0] < scores[1])? 1 : (scores[0] > scores[1])? 2 : 0;
			sendMessage("win " + winner);
		}
		
		sendMessage("turn " + ((playerNo == 1)? 2 : 1));
		
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		if(partner != sender) partner.endTurn(playerNo, sender);
	}
	
	/**
	 * @return the name of the player associated with this object.
	 */
	public String getPlayerName() { 
		return name;
	}
}
