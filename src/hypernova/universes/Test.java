package hypernova.universes;

import hypernova.NewUniverse;
import hypernova.Activity;
import hypernova.Faction;
import hypernova.Realization;
import hypernova.SaveGame;
import hypernova.UniNames;
import hypernova.activities.CountDown;
import hypernova.gui.Viewer;
import hypernova.gui.Wormhole;
import hypernova.gui.Transition;
import hypernova.activities.WarpZoneTest;
import hypernova.gui.backgrounds.EqualizerBackground;

public class Test extends NewUniverse {
   public static Test INSTANCE = new Test();
   static final long serialVersionUID = 7533472837495L;

   private boolean countDone = false;
   
   public static void setCountDone(){INSTANCE.countDone = true;}
   
   public void begin()
   {
        Faction.clear();
        Faction.create("Humans", Faction.ColorType.TEST_HUMAN);
        Faction.create("Invaders", Faction.ColorType.TEST_INVADER);
        SaveGame.setCheckpoint(0, 0, UniNames.TEST);
        Viewer.setBackground(new EqualizerBackground());
        Activity battle = new hypernova.activities.FactoryBattle();
        u.addActivity(battle, -500, -500);
        if( !INSTANCE.countDone ) u.addActivity(new CountDown(), 500, -500);
        Wormhole.add(0,-1500,400,400,UniNames.START,Transition.Types.DIAGONAL);
        u.queueMessage("You are there");
   }
} 
