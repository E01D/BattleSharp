package ultratest;
import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {


	public static void run(RobotController rcIn) {
		
		Utility.rc = rcIn;
		Utility.rnd = new Random(Utility.rc.getID());

		while (true) {
			try {
				if (Utility.rc.getType() == RobotType.ARCHON) {
					Archon.archonCode(rcIn);
				} else if (Utility.rc.getType() == RobotType.SCOUT) {
					Scout.scoutCode(rcIn);
				} else if (Utility.rc.getType() == RobotType.SOLDIER) {
					Soldier.soldierCode(rcIn);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Clock.yield();
		}
	}

}