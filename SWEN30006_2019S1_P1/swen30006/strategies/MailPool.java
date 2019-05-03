package strategies;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.ArrayList;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import strategies.GroupRobotCarry;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

	private class Item {
		int priority;
		int destination;
		MailItem mailItem;
		// Use stable sort to keep arrival time relative positions
		
		public Item(MailItem mailItem) {
			//--all no priority items have a priority == 1
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;
	private GroupRobotCarry groupRobotCarry;
	private ArrayList<Robot> freeRobots = null;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException {
		try{
			ListIterator<Robot> i = robots.listIterator();
			//continues to load until there are no more free robots
			do {
				while (i.hasNext()) loadRobot(i);
				
				//free the robots which were waiting to carry a heavy item
				if (freeRobots != null) {
					for(Robot freeRobot: freeRobots) {
						registerWaiting(freeRobot);
					}
					freeRobots = null;
				}
			} while(freeRobots != null);
			
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
		Robot robot = i.next();
		MailItem nextMailItem;
		assert(robot.isEmpty());
		boolean itemRemoved = false;
		
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			try {
			nextMailItem = j.next().mailItem;
			
			//case: when higher priority item comes in while robots are queuing to carry a heavy item
			//solution: disbands the queue and carries the new item
			if ((groupRobotCarry != null) && (nextMailItem != groupRobotCarry.getMailItem())) {
				groupRobotCarry.resetPriority();
				freeRobots = groupRobotCarry.getRobots();
				groupRobotCarry = null;
			}
			
			// hand first as we want higher priority delivered first
			robot.addToHand(nextMailItem); 

			//robot carries item if it is light enough
			if(nextMailItem.getWeight() <= Robot.INDIVIDUAL_MAX_WEIGHT)  {
				j.remove();
				itemRemoved = true;
				
				
			} else if (nextMailItem.getWeight() <= Robot.TRIPLE_MAX_WEIGHT) {
				
				//add robot to carry a heavy item if a group already exists
				if ((groupRobotCarry != null) && !groupRobotCarry.foundRobot(robot)) {
					groupRobotCarry.addRobot(robot);
					
				} else {
				//create a group to carry a heavy item if one does not already exists
					robot.setRobotDelivering();
					groupRobotCarry = new GroupRobotCarry(new ArrayList<Robot>(Arrays.asList(robot)), nextMailItem);
				}
				
				robot.setInGroup();

				
				//remove from the pool when there is enough robots can carry it
				if (((nextMailItem.getWeight() <= Robot.PAIR_MAX_WEIGHT) && (groupRobotCarry.getNumRobots() == 2))
						|| (groupRobotCarry.getNumRobots() == 3)) {
					j.remove();
					itemRemoved = true;
				} 
				
			} else {
				throw new ItemTooHeavyException();
			}
			
			if (pool.size() > 0) {
				
				//traverses through the mail pool to find an item that the tube can carry
				while(j.hasNext() && robot.getTube() == null) {
					nextMailItem = j.next().mailItem;
					if (nextMailItem.getWeight() <= Robot.INDIVIDUAL_MAX_WEIGHT) {
						robot.addToTube(nextMailItem);
						j.remove();
						break;
					}
				}
			}
			
			
			//dispatches the robots when there is enough robots to carry the item
			if ((groupRobotCarry != null) && itemRemoved) {
				for (Robot a_robot: groupRobotCarry.getRobots()) {
					groupRobotCarry = null;
					a_robot.dispatch();
				}
			} else if (groupRobotCarry == null && itemRemoved) {
				robot.dispatch();
			}		
			i.remove();

			} catch (Exception e) { 
	            throw e; 
	        } 
		}
		
	}

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}


