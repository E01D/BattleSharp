package ultratest;
import java.util.ArrayList;

import battlecode.common.*;

public class Utility 
{
	public static final int MINIMUM_TIME_TO_TURTLE = 100;

	
	public static final int STRATEGY_CODE = 77525;
	public static final int ARCHON_IN_TROUBLE = 062666;
	public static final String SCOUT_TURRET_VISION_PREFACE_CODE = "17";
	public static final int CLUMP_OF_FOES_CODE = 54031;
	public static final int ENEMY_TURTLE = 32856;
	public static final int ZOMBIE_DEN = 23666;
	

	public static ArrayList<Direction> arrayListOfDirections()
	{
		ArrayList<Direction> directions = new ArrayList<Direction>();
		Direction[] arrayDirections = Direction.values();
		for(Direction dir : arrayDirections)
		{
			directions.add(dir);
		}
		return directions;
	}
}
