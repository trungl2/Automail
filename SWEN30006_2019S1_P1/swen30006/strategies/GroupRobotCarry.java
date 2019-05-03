package strategies;
import java.util.ArrayList;
import automail.Robot;
import automail.MailItem;

/**
 * this class represents the group of robots that is waiting to carry
 * a heavy item
 */

public class GroupRobotCarry {
	private ArrayList<Robot> robots;
	private MailItem mailItem;
	
	public GroupRobotCarry(ArrayList<Robot> robots, MailItem mailItem) {
		this.robots = robots;
		this.mailItem = mailItem;
	}
	
	//adds a robot to the group if not already in it
	public void addRobot(Robot otherRobot) {
		boolean foundSimilar = false;
		for (Robot robot: robots) {
			if (robot.getID() == otherRobot.getID()) {
				foundSimilar = true;
			}
		}
		if (!foundSimilar) {
			robots.add(otherRobot);
		}
		
	}
	
	public boolean isEmpty() {
		return robots.isEmpty();
	}
	
	public int getNumRobots() {
		return robots.size();
	}
	
	public ArrayList<Robot> getRobots() {
		return robots;
	}
	
	//checks if the robot is already in the group
	public boolean foundRobot(Robot otherRobot) {
		for (Robot robot: robots) {
			if (otherRobot.getID() == robot.getID()) {
				return true;
			}
		}
		return false;
	}
	
	public MailItem getMailItem() {
		return mailItem;
	}
	
	//resets information on all robots about delivering a mail item
	//that now is not highest priority
	public void resetPriority() {
		for (Robot robot: robots) {
			robot.resetPriority();
		}
	}
}
