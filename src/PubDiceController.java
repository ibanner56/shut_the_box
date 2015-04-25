import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Controller for Shut the Box. Deals with communication between model and view.
 * Created by Isaac on 4/22/2015.
 */
public class PubDiceController implements Observer{
    private PubDiceUI pdui;

    public PubDiceController () {
        pdui = new PubDiceUI(this);


        pdui.setMessage ("Waiting for partner");
        pdui.enableButtons(false);
        pdui.setVisible (true);

        pdui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PubDice.quit();
            }
        });
    }

    public void tileClick(int i) {
        PubDice.flipTile(i);
    }

    public void dieClick() {
        PubDice.rollDice();
    }

    public void passTurn() {
        PubDice.passTurn();
    }

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof String) {
            String change = (String) arg;
            if(change.contains("joined")) {
                pdui.setMessage(PubDice.getPrintOut());
            }
            if(change.contains("turn")) {
                pdui.enableButtons(PubDice.getTurn());
            }
            if(change.contains("tile")) {
                for(int tile = 1; tile < PubDice.getTiles().length; tile++)
                    pdui.setTile(tile, PubDice.getTiles()[tile]);
            }
            if(change.contains("dice")) {
                int[] dice = PubDice.getDice();
                pdui.setDie(0, dice[0]);
                pdui.setDie(1, dice[1]);
            }
            if(change.contains("score")) {
                pdui.setMessage(PubDice.getPrintOut());
                pdui.enableButtons(PubDice.getTurn());
            }
            if(change.contains("win")) {
                pdui.setMessage(PubDice.getPrintOut());
            }
        }
    }

    // TODO:
    // Turn progression:
    // All Tiles Up
    // Let them press dice.
    // Roll dice
    // loop:
    // Let them flip tiles.
    // On Die press:
    //   check if legal tile choices:
    //   if not legal - alert them, don't submit.
    //   if legal
    //     send tile choices
    //     roll dice
    //     check for legal moves (maybe)
    //     if no legal moves, goto end
    //     else goto loop
}
