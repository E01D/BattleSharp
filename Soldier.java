package ultratest;
import battlecode.common.*;
import ultratest.RobotPlayer;

import java.util.*;


public class Soldier
{
	public static Random rand;
	public static RobotController rc;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static int numDirections = directions.length;

	public static double probMove = 0.1; 

	public static int maxMomentum = 0; 

	public static int closeEnoughSquared = 4; 

	public static double probProtector = 0;

	public static double probIgnoreRubbleIfNotTooMuch = 0.2;
	
	
	public static double startTooMuchRubble = 500;

	public static int foeSignalRadiusSquared = 1000; 
	public static double probSignal = 0.15;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID());

		boolean isProtector = Math.random() < probProtector;

		double tooMuchRubble = startTooMuchRubble;
		
		double rubbleToleranceGrowthFactor = 2; 
		
		MapLocation goalLoc = null;
		Direction dirToMove = Direction.NONE;
		
		
		int turnsLeft = 50;
		int momentum = maxMomentum;
		
		
		boolean offCourse = false;
		
		boolean anyFoesToAttack = true;
		MapLocation myLoc = rc.getLocation();
		int makerArchonID = 0;
		RobotInfo makerArchon = null;

		
		int movesAwayFromArchon = 2;
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(movesAwayFromArchon*movesAwayFromArchon);
		for (RobotInfo robot : nearbyRobots)
		{
			if(robot.type == RobotType.ARCHON)
			{
				makerArchonID = robot.ID;
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
					//movement code copied below (a little specialized)
					int timesRotated = 0;
					boolean done = false;
					boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
					//start in a direction, choose a random way to turn, turn that way until you've tried all the directions
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
							else //clear the rubble
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
								if(timesRotated > 0)
								{
									momentum = maxMomentum; //so tries to go around the wall?
								}
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
			if(isProtector)
			{
				try
				{
					makerArchon = rc.senseRobot(makerArchonID);
					goalLoc = makerArchon.location;
					turnsLeft = myLoc.distanceSquaredTo(goalLoc);
				}
				catch (Exception GameActionException)
				{
					//nothing
				}
			}
			
			//try to attack weakest foe, if successful then finish turn
			if(anyFoesToAttack)
			{
				if(rc.isWeaponReady())
				{
					RobotInfo[] foes = rc.senseHostileRobots(myLoc, RobotType.SOLDIER.attackRadiusSquared);

					if(foes.length > 0)
					{
						RobotInfo targetFoe = null;
						double lowestHealth = 0;
						for(RobotInfo foe : foes)
						{
							if(foe.type == RobotType.ARCHON) //highest priority
							{
								targetFoe = foe;
								break;
							}
							if(lowestHealth == 0 || foe.health < lowestHealth)
							{
								targetFoe = foe;
								lowestHealth = foe.health;
							}
						}
						
						if(targetFoe.type.attackRadiusSquared < RobotType.SOLDIER.attackRadiusSquared
						   && myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
						{
							Direction away = targetFoe.location.directionTo(myLoc);
							simpleTryMove(away);
							boolean enemyStillThere = true;
							while(myLoc.distanceSquaredTo(targetFoe.location) <= targetFoe.type.attackRadiusSquared)
							{
								try
								{
									targetFoe = rc.senseRobot(targetFoe.ID);
									away = targetFoe.location.directionTo(myLoc);
									simpleTryMove(away);
								}
								catch (Exception GameActionException)
								{
									enemyStillThere = false;
									simpleTryMove(away.opposite());
									break;
								}
								myLoc = rc.getLocation();

							}
							if(enemyStillThere && rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
							{
								rc.attackLocation(targetFoe.location);
							}
						}
						
						
						else
						{
							if(rc.isWeaponReady() && rc.canAttackLocation(targetFoe.location))
							{
								rc.attackLocation(targetFoe.location);
							}
						}
												
						if(Math.random() < probSignal)
						{
							rc.broadcastSignal(foeSignalRadiusSquared);
						}
					}
					else //no foes in attack range
					{
						anyFoesToAttack = false;
						RobotInfo[] foesYouCanOnlySee = rc.senseHostileRobots(myLoc, RobotType.SOLDIER.sensorRadiusSquared);
						//could do min thing here too, but $$$?
						if(foesYouCanOnlySee.length > 0)
						{
							goalLoc = foesYouCanOnlySee[0].location;

							if(Math.random() < probSignal)
							{
								rc.broadcastSignal(foeSignalRadiusSquared);
							}
						}
						continue;
					}
				}
			}
			else
			{
				if(goalLoc == null)
				{
					//follow signal closest to you
					Signal[] signals = rc.emptySignalQueue();
					MapLocation closestSignalLoc = null;
					double smallestCloseness = 0;
					for(Signal signal : signals)
					{
						MapLocation signalLoc = signal.getLocation();
						double signalCloseness = myLoc.distanceSquaredTo(signalLoc);
						//follow enemy signals to kill messengers or your own team's signals to group up
						if((smallestCloseness == 0 || signalCloseness < smallestCloseness) && (signal.getMessage() == null || signal.getTeam() != myTeam))
						{
							closestSignalLoc = signalLoc;
							smallestCloseness = signalCloseness;
						}
					}
					if(closestSignalLoc != null)
					{
						goalLoc = closestSignalLoc;
						dirToMove =  myLoc.directionTo(goalLoc);
						turnsLeft = (int) smallestCloseness; //not sure what would be better
						continue;
					}
				
					
					else //move randomly
						 //this code is copied some below
						 //maybe change to move towards friends
					{
						if(rc.isCoreReady() && Math.random() < probMove)
						{
							int timesRotated = 0;
							boolean done = false;
							boolean turnLeft = rand.nextBoolean(); //if true keep turning left, if false keep turning right
							if(momentum <= 0 || dirToMove == Direction.NONE)
							{
								dirToMove = directions[rand.nextInt(directions.length)]; //random dir
								momentum = maxMomentum; //reset momentum
							}
							else
							{
								momentum --; //should do this here or only when have moved?
							}
							while((timesRotated < numDirections) && (! done))
							{
								double rubble = rc.senseRubble(myLoc.add(dirToMove));
								if(rubble > GameConstants.RUBBLE_OBSTRUCTION_THRESH) //can't get through it
								{
									if(rubble >= tooMuchRubble && Math.random() < probIgnoreRubbleIfNotTooMuch) //try another direction
									{
										tooMuchRubble *= rubbleToleranceGrowthFactor;
										dirToMove = turn(dirToMove, turnLeft);
										timesRotated ++;
									}
									else //clear the rubble
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
										turnsLeft --;
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
						}
					}
				}
				else
				{
					if(rc.isCoreReady())
					{
						if((myLoc.distanceSquaredTo(goalLoc) <= closeEnoughSquared) || (turnsLeft <= 0)) //done
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
										offCourse = true; //means you have to recompute direction to goalLoc
									}
									else //clear the rubble
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
										turnsLeft --;
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

	//turnLeft says whether or not to turnLeft
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
