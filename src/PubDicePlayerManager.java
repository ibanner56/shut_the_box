import java.util.HashMap;

public class PubDicePlayerManager {
	private static final PubDicePlayerManager manager = new PubDicePlayerManager();
	private HashMap<PubDiceClientThread, PubDiceClientThread> pairings;
	
	private PubDiceClientThread waitingPlayer;

	private PubDicePlayerManager() {
		pairings = new HashMap<PubDiceClientThread, PubDiceClientThread>();
	}

	public static void join(PubDiceClientThread player) {
		manager.join_(player);
	}

	public static void quit(PubDiceClientThread player) {
		manager.quit_(player);
	}

	public static PubDiceClientThread getPartner(PubDiceClientThread player) {
		return manager.getPartner_(player);
	}
	
	private synchronized void join_(PubDiceClientThread player) {
		if(waitingPlayer == null) {
			waitingPlayer = player;
		} else {
			pairings.put(waitingPlayer, player);
			pairings.put(player, waitingPlayer);
			
			waitingPlayer.pairUp(1);
			player.pairUp(2);

			waitingPlayer.turn(1);
			
			waitingPlayer = null;
		}
	}

	private synchronized void quit_(PubDiceClientThread player) {
		if(pairings.containsKey(player)) {
			pairings.remove(pairings.get(player));
			pairings.remove(player);
		} else if(waitingPlayer == player) {
			waitingPlayer = null;
		}
	}

	private synchronized PubDiceClientThread getPartner_(PubDiceClientThread player) {
		return pairings.get(player);
	}

}
