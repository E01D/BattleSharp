package ultratest;
import battlecode.common.*;

import java.util.*;

public class Viper
{
	public static Random rand;
	public static RobotController rc;

	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;

	public static double probMove = 0.4;

	public static int closeEnoughSquared = 4; 

	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	
	public static double startTooMuchRubble = 500;

	public static int foeSignalRadiusSquared = 1000; 
	public static double probSignal = 0.15;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID());

		double tooMuchRubble = startTooMuchRubble;

		double rubbleToleranceGrowthFactor = 2; 

		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;

		int roundsLeft = 50;

		boolean offCourse = false;

		boolean anyFoesToAttack = true;
		MapLocation myLoc = rc.getLocation();
		RobotInfo makerArchon = null;

		
		int movesAwayFromArchon = 2;
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(movesAwayFromArchon*movesAwayFromArchon);
		for (RobotInfo robot : nearbyRobots)
		{
			if(robot.type == RobotType.ARCHON)
			{
				makerArchon = robot;
				break;
			}
		}
		if(makerArchon != null)
		{
			dirToMove = makerArchon.location.directionTo(myLoc); //away from archon
			while(movesAwayFromArchon > 0)
			{
				if(rc.isCoreReady())
				{
					int timesRotated = 0;
					boolean done = false;
					boolean turnLeft = rand.nextBoolean(); 
					while((timesRotated < numDirections) && (! done))
					{
						double rubble = rc.senseRubble(myLoc.add(dirToMove));
						if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH)
						{
							if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
							{
								tooMuchRubble *= rubbleToleranceGrowthFactor;
								dirToMove = turn(dirToMove, turnLeft);
								timesRotated ++;
							}
							else
							{
								rc.clearRubble(dirToMove);
								done = true;
							}
						}
						else
						{
							if(rc.canMove(dirToMove))
							{
								rc.move(dirToMove);
								done = true;
								myLoc = rc.getLocation();
							}
							else
							{
								dirToMove = turn(dirToMove, turnLeft);
								timesRotated ++;
							}
						}
					}
					movesAwayFromArchon --;
				}
				Clock.yield();
			}
		}

		while(true)
		{
		
			if(anyFoesToAttack)
			{
				if(rc.isWeaponReady()) 
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotType.VIPER.attackRadiusSquared);

					if(foes.length > 0)
					{
						RobotInfo targetFoe = null;
						double lowestHealth = 0;
						for(RobotInfo foe : foes)
						{
							if(foe.type == RobotType.ARCHON)
							{
								targetFoe = foe;
								break;
							}
							if((lowestHealth == 0) || (foe.health < lowestHealth))
							{
								targetFoe = foe;
								lowestHealth = foe.health;
							}
						}

					
						if(targetFoe.type.attackRadiusSquared < RobotType.VIPER.attackRadiusSquared
								&& myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
						{

							if(goalLoc != null) 
							{
								offCourse = true; 
							}

							while(myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
							{
								try
								{
								
									targetFoe = rc.senseRobot(targetFoe.ID);
									dirToMove = targetFoe.location.directionTo(myLoc); //away from foe

									int timesRotated = 0;
									boolean done = false; 
									boolean turnLeft = rand.nextBoolean(); 
									while((timesRotated < numDirections) && (! done))
									{
										if(rc.canMove(dirToMove))
										{
											rc.move(dirToMove);
											done = true;
											myLoc = rc.getLocation();
										}
										else
										{
										

											dirToMove = turn(dirToMove, turnLeft);
											timesRotated ++;
										}
									}

									if(done) //you moved
									{
										Clock.yield();
									}
									else 
									{
										break;
									}
								}
								catch (Exception GameActionException)
								{
									dirToMove = dirToMove.opposite();
									if(rc.canMove(dirToMove))
									{
										if(rc.isCoreReady())
										{
											rc.move(dirToMove);
											myLoc = rc.getLocation();
											Clock.yield();
										}
									}
									else
									{
									
										break;
									}

									break;
								}
							}

							if(rc.isWeaponReady())
							{
								try
								{
									targetFoe = rc.senseRobot(targetFoe.ID);
									rc.attackLocation(targetFoe.location);
									if(Math.random() < probSignal)
									{
										rc.broadcastSignal(foeSignalRadiusSquared);
									}
								}
								catch (Exception GameActionException)
								{
								
								}
							}
						}
						else
						{
							rc.attackLocation(targetFoe.location);
							if(Math.random() < probSignal)
							{
								rc.broadcastSignal(foeSignalRadiusSquared);
							}
						}
					}
					else 
					{
						anyFoesToAttack = false;
						RobotInfo[] foesYouCanOnlySee = rc.senseHostileRobots(myLoc, RobotType.VIPER.sensorRadiusSquared);

	
						if(foesYouCanOnlySee.length > 0)
						{
							RobotInfo targetFoe = foesYouCanOnlySee[0];
							goalLoc = targetFoe.location;
							roundsLeft = myLoc.distanceSquaredTo(targetFoe.location);
						}

						continue; 
					}
				}
			}
			else
			{
				if(goalLoc == null)
				{
				
					Signal[] signals = rc.emptySignalQueue();
					MapLocation chosenSignalLoc = null;
					double smallestCloseness = 0;
					for(Signal signal : signals)
					{
					
						if((signal.getMessage() == null) && (signal.getTeam() == myTeam))
						{
							MapLocation signalLoc = signal.getLocation();
							double signalCloseness = myLoc.distanceSquaredTo(signalLoc);

							if((smallestCloseness == 0) || (signalCloseness < smallestCloseness))
							{
								chosenSignalLoc = signalLoc;
								smallestCloseness = signalCloseness;
							}
						}
					}
					if(chosenSignalLoc != null)
					{
						goalLoc = chosenSignalLoc;
						dirToMove =  myLoc.directionTo(goalLoc);
						roundsLeft = (int) smallestCloseness; 
						continue;
					}

				
				}
				else 
				{
					if(rc.isCoreReady())
					{
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (roundsLeft <= 0)) //done
						{
							goalLoc = null;
							dirToMove = Direction.NONE;
							continue; 
						}
						else
						{
							if(offCourse)
							{
								dirToMove =  myLoc.directionTo(goalLoc);
								offCourse = false;
							}
							int timesRotated = 0;
							boolean done = false; 
							boolean turnLeft = rand.nextBoolean(); 
							while((timesRotated < numDirections) && (! done))
							{
								double rubble = rc.senseRubble(myLoc.add(dirToMove));
								if(rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH)
								{
									if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
										offCourse = true; 
									}
									else
									{
										rc.clearRubble(dirToMove);
										done = true;
									}
								}
								else
								{
									if(rc.canMove(dirToMove))
									{
										rc.move(dirToMove);
										roundsLeft --;
										done = true;
										myLoc = rc.getLocation();
									}
									else
									{
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
										offCourse = true;
									}
								}
							}
						}
					}
				}
				anyFoesToAttack = true;
			}
			Clock.yield();
		}
	}

	public static void simpleTryMove(Direction dirToMove) throws GameActionException
	{
		if(rc.isCoreReady() && rc.canMove(dirToMove))
		{
			rc.move(dirToMove);
			Clock.yield();
		}
	}

	public static Direction turn(Direction dir, boolean turnLeft)
	{
		if(turnLeft)
		{
			return dir.rotateLeft();
		}
		else
		{
			return dir.rotateRight();
		}
	}
}
