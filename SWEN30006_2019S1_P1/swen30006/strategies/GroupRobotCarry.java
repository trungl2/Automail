package strategies;
import java.util.ArrayList;
import automail.Robot; //maybe
import automail.MailItem;

public class GroupRobotCarry {
	private ArrayList<Robot> robots;
	private MailItem mailItem;
	
	public GroupRobotCarry(ArrayList<Robot> robots, MailItem mailItem) {
		this.robots = robots;
		this.mailItem = mailItem;
	}
	
	public void addRobot(Robot robot) {
		boolean foundSimilar = false;
		for (Robot a_robot: robots) {
			if (a_robot.getID() == robot.getID()) {
				foundSimilar = true;
			}
		}
		if (!foundSimilar) {
			robots.add(robot);
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
	
	public void resetPriority() {
		for (Robot robot: robots) {
			robot.resetPriority();
		}
	}
}
