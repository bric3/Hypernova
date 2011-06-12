package hypernova.pilots;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.apache.log4j.Logger;

import hypernova.Universe;
import hypernova.Ship;

public class KeyboardPilot extends Pilot implements KeyListener {
    private static Logger log = Logger.getLogger("KeyboardPilot");
    private static KeyboardPilot INSTANCE = new KeyboardPilot();

    private KeyboardPilot() {
        super(null);
    }

    public static KeyboardPilot get() {
	return INSTANCE;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        log.trace("keyPressed() " + e);
	Ship ship = getShip();
	if(ship == null) {
	    log.trace("no ship");
	    return;
	}

        switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            ship.turnLeft(true);
            break;
        case KeyEvent.VK_RIGHT:
            ship.turnRight(true);
            break;
        case KeyEvent.VK_UP:
            ship.setEngines(1.0);
            break;
        case KeyEvent.VK_DOWN:
            ship.setEngines(-1.0);
            break;
        case KeyEvent.VK_SPACE:
            ship.setFire(0, true);
            break;
        default:
            log.trace("Unkown key " + e.getKeyCode());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        log.trace("keyReleased() " + e);
	Ship ship = getShip();
	if(ship == null) {
	    log.trace("no ship");
	    return;
	}

        switch (e.getKeyCode()) {
        case KeyEvent.VK_LEFT:
            ship.turnLeft(false);
            break;
        case KeyEvent.VK_RIGHT:
            ship.turnRight(false);
            break;
        case KeyEvent.VK_UP:
            ship.setEngines(false);
            break;
        case KeyEvent.VK_DOWN:
            ship.setEngines(false);
            break;
        case KeyEvent.VK_SPACE:
            ship.setFire(0, false);
            break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void drive(double dt) {
        /* Driving is handled entirely by keyboard events. */
    }
}
