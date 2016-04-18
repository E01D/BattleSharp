package ultratest;
import java.util.Random;
import battlecode.common.*;

public class Archon {
	static RobotController rc;

	public static void archonCode(RobotController rcIn) {
		rc = rcIn;
	
		
		if(rc.isCoreReady()) {
			if (rc.getTeamParts()> 20) {
				
				if(rc.canBuild(Direction.EAST, RobotType.SCOUT)) {
					
					try {
						rc.build(Direction.EAST, RobotType.SCOUT);
						
						
						
	
						
						
						
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
							
			
			}
		}
	
	}
	
	
}

