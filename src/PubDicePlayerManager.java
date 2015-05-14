import java.util.HashMap;

public class PubDicePlayerManager {
	private static final PubDicePlayerManager manager = new PubDicePlayerManager();
	private HashMap<PubDiceClientThread, PubDiceClientThread> pairings;
	
	private PubDiceClientThread waitingPlayer;

	/**
	 * Makes the singleton player manager.
	 */
	private PubDicePlayerManager() {
		pairings = new HashMap<PubDiceClientThread, PubDiceClientThread>();
	}

	/**
	 * Calls the manager's join.
	 * @param player - the player joining
	 */
	public static void join(PubDiceClientThread player) {
		manager.join_(player);
	}

	/**
	 * Calls the manager's quit.
	 * @param player - the player quitting
	 */
	public static void quit(PubDiceClientThread player) {
		manager.quit_(player);
	}

	/**
	 * Calls the manager's getPartner.
	 * @param player - the player whose partner is required.
	 */
	public static PubDiceClientThread getPartner(PubDiceClientThread player) {
		return manager.getPartner_(player);
	}

	/**
	 * Pairs up partners.
	 * @player - a player waiting for a partner
	 */
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

	/**
	 * Pulls a player out of the pairings.
	 * @player - the quitter
	 */
	private synchronized void quit_(PubDiceClientThread player) {
		if(pairings.containsKey(player)) {
			pairings.remove(pairings.get(player));
			pairings.remove(player);
		} else if(waitingPlayer == player) {
			waitingPlayer = null;
		}
	}

	/**
	 * @param player - the player whose partner is being requested
	 * @return the passed player's partner
	 */
	private synchronized PubDiceClientThread getPartner_(PubDiceClientThread player) {
		return pairings.get(player);
	}

}
