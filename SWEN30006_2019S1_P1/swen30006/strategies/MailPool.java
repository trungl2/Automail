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
				
				if (freeRobots != null) {
					for(Robot freeRobot: freeRobots) {
						registerWaiting(freeRobot);
						System.out.println("freed robot" + freeRobot.getID());
					}
					freeRobots = null;
				}
			} while(freeRobots != null);
			
		} catch (Exception e) { 
            throw e; 
        } 
	}
	
	
	//PROBLEM: if you have robots about to carry an item, but items with higher priority comes in, what should be done?
	//note higher priority items becomes the next mail item in the list -->carry the item
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
		Robot robot = i.next();
		MailItem nextMailItem;
		assert(robot.isEmpty());
		boolean itemRemoved = false;
		
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			try {
			nextMailItem = j.next().mailItem;
			System.out.println("curr robot: " + robot.getID());
			System.out.println("next mail item: " + nextMailItem.getWeight());
			
			//deals with case where higher priority item comes in while robots are queuing to carry a heavy item
			//--> disbands the queue and carries the new item
			if ((groupRobotCarry != null) && (nextMailItem != groupRobotCarry.getMailItem())) {
				groupRobotCarry.resetPriority();
				freeRobots = groupRobotCarry.getRobots();
				groupRobotCarry = null;
				System.out.println("reset priority");
				
			}
			
			robot.addToHand(nextMailItem); // hand first as we want higher priority delivered first
			

			
			//if robot can carry by itself, do it
			if(nextMailItem.getWeight() < Robot.INDIVIDUAL_MAX_WEIGHT)  { //--added
				j.remove();
				itemRemoved = true;
				System.out.println("light removed");
				
			
			} else if (nextMailItem.getWeight() <= Robot.TRIPLE_MAX_WEIGHT) { /*maybe remove <= --> <*/
				
				//if there is already a group of robots about to carry the heavy item
				//add the robot to the list robots to carry the heavy item
				if ((groupRobotCarry != null) && !groupRobotCarry.foundRobot(robot)) {
					groupRobotCarry.addRobot(robot);
					System.out.println("group robot id: " + robot.getID());
					
				} else {
				//or create a new list of robots to carry the heavy item
					robot.setRobotDelivering(); //set the first robot to be the one to deliver the mail item
					groupRobotCarry = new GroupRobotCarry(new ArrayList<Robot>(Arrays.asList(robot)), nextMailItem);
					System.out.println("create group robot id: " + robot.getID());
				}

				
				//remove the item at the start of the list when enough robots can carry it
				if (((nextMailItem.getWeight() < Robot.PAIR_MAX_WEIGHT) && (groupRobotCarry.getNumRobots() == 2))
						|| (groupRobotCarry.getNumRobots() == 3)) {
					j.remove();
					itemRemoved = true;
					System.out.println("heavy removed");
					
					
					
				} 
				
				if (groupRobotCarry != null) {
					System.out.println("group num robots" + groupRobotCarry.getNumRobots());
				}
				
			} else {
				throw new ItemTooHeavyException();
			}
			
			if (pool.size() > 0) {
				
				/*ListIterator<Item> dupj = (ListIterator<Item>) j.clone();
				Item nextj = j.next();*/
				
				/*get the new item at the start of the list if prior item was added to hand*/
				if (itemRemoved) {
					nextMailItem = j.next().mailItem;
					System.out.println("next item for tube");
				}
				
				/*if robot can carry by itself and there is are still items in the pool, add the item to the tube*/
				if (nextMailItem.getWeight() < Robot.INDIVIDUAL_MAX_WEIGHT) {
					robot.addToTube(nextMailItem);
					j.remove();
					System.out.println("tube remove");
				}
				
			}
			
			
			

			
			/*if there is enough robots to carry, starting going*/
			if ((groupRobotCarry != null) && itemRemoved) {
				for (Robot a_robot: groupRobotCarry.getRobots()) {
					groupRobotCarry = null;
					a_robot.dispatch();
					System.out.println("heavy dispatch");
				}
			/*or if only one robot is required to carry the item, start going*/
			} else if (groupRobotCarry == null && itemRemoved) {
				robot.dispatch(); // send the robot off if it has any items to deliver
				System.out.println("light dispatch");
			}
			System.out.println(i.hasNext());
				
			i.remove();       // remove from mailPool queue
			

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


