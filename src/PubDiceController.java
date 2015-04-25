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

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof String) {
            String change = (String) arg;
            if("joined".equals(change)) {
                pdui.setMessage(PubDice.getPrintOut());
            } else if("turn".equals(change)) {
                pdui.enableButtons(PubDice.getTurn());
            } else if("tile".equals(change.substring(0, 4))) {
                int tile = Integer.parseInt(change.split(" ")[1]);
                pdui.setTile(tile, PubDice.getTiles()[tile]);
            } else if("dice".equals(change)) {
                int[] dice = PubDice.getDice();
                pdui.setDie(0, dice[0]);
                pdui.setDie(1, dice[1]);
            } else if("score".equals(change)) {
                pdui.setMessage(PubDice.getPrintOut());
                pdui.enableButtons(PubDice.getTurn());
            } else if("win".equals(change)) {
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
