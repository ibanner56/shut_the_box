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

	private void sendMessage(String message) {
		try {
			client.write(message);
		} catch (IOException ex) {
			if(!quit)
				System.err.println(ex);
		}
	}

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
	
	public void turn(int turnNo) {
		turn(turnNo, this);
	}

	public void turn(int turnNo, PubDiceClientThread sender) {
		sendMessage("turn " + turnNo);
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		if(partner != sender) partner.turn(turnNo, sender);
	}

	public void quit() {
		this.quit = true;
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		PubDicePlayerManager.quit(this);
		if(partner != null) partner.quit();
		sendMessage("quit");
	}
	
	public void setTile(int tile, String dir) {
		setTile(tile, dir, this);
	}

	public void setTile(int tile, String dir, 
			PubDiceClientThread sender) {
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		if(partner != sender) partner.setTile(tile, dir, sender);
		sendMessage("tile " + tile + " " + dir);	
		tiles[tile] = "up".equals(dir);	
	}

	public void rollDice() {
		Random roll = new Random(System.currentTimeMillis());
		int _1d6 = roll.nextInt(5) + 1;
		int _2d6 = roll.nextInt(5) + 1;
		sendMessage("dice " + _1d6 + " " + _2d6);
		
		PubDiceClientThread partner = PubDicePlayerManager.getPartner(this);
		partner.sendRoll(_1d6, _2d6);
	}

	public void sendRoll(int _1d6, int _2d6) {
		sendMessage("dice " + _1d6 + " " + _2d6);
	}

	public void endTurn() {
		endTurn(playerNo, this);
	}
	
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

	public String getPlayerName() { 
		return name;
	}
}
