package CAH;

import java.util.*;

/**
 * The model is responsible for setting up the simulation
 * @author Ross Kitsis
 *
 */
public class Model 
{
	//Simulation parameters
	private int numNodes;
	private int numLanes;
	private int width;
	private double PoS;
	private double density;
	private int range;
	private int numSlots;

	//AdHoc parameters
	private double b;
	private int maxReservationDuration;
	
	//Clock
	Clock c = Clock.CLOCK;
	
	//Random number generator
	private Random r = new Random();
	
	//Lanes
	List<ArrayList<Node>> lanes = new ArrayList<ArrayList<Node>>();
	
	//All nodes
	List<Node> nodes = new ArrayList<Node>();
	
	//Node Positions
	Node[][] field;
	
	//Max time a message may be stored in memory; ensures no stale messages sent
	private int maxMemory;
	
	//List of nodes without reservation for PoS reset to ensure same nodes
	private List<Node> noRes = new ArrayList<Node>();

	public Model(int numNodes, int numLanes, int width, double poS,
			double density, int range, double b, int reservationDuration, int numSlots, int maxMemory) {
		this.numNodes = numNodes;
		this.numLanes = numLanes;
		this.width = width;
		PoS = poS;
		this.density = density;
		this.range = range;
		this.b = b;
		this.maxReservationDuration = reservationDuration;
		this.numSlots = numSlots;
		
		this.maxMemory = maxMemory;
		
		//Instantiate all lane lists
		for(int i = 0; i < numLanes; i++)
		{
			lanes.add(new ArrayList<Node>());
		}
		
	}
	/**
	 * Creates the simulation area
	 * Creates nodes with the required fields
	 * Places nodes onto the simulation area
	 * Initializes nodes for simulation
	 * 
	 */
	public void buildModel()
	{
		//Arranges all cars in the simulation field
		this.arrangeCars();
		//Finds the OHS of all nodes
		this.findOHS();
		//Finds the THS of all nodes
		this.findTHS();
		//Reserve Time slots
		this.reserveTimeSlots();
		//Set nodes without reservations
		this.setInitialNodesWithoutReservation(b);
	}
	/**
	 * Arranges cars based on the number of lanes
	 */
	private void arrangeCars()
	{
		//Number of cars placed so far
		int numCar = 0;
		
		//Position in lane where cars placed
		int lanePosition = 0;
		
		//Place cars until reach simulation parameters
		while(numCar < this.numNodes)
		{
			for(int i = 0; i < this.numLanes; i++)
			{
				double rand = r.nextDouble();
				if(rand < this.density)
				{
					Node toAdd = new Node(numCar, this.numSlots,
							this.PoS, this.range,i,lanePosition, this.maxReservationDuration);
					nodes.add(toAdd);
					numCar++;
				}
			}
			lanePosition++;
		}
		field = new Node[numLanes][lanePosition];
		for(int i = 0; i < this.numNodes; i++)
		{
			Node active = nodes.get(i);
			field[active.getXLanePosition()][active.getyLanePosition()] = active;
		}
	}
	public void findOHS()
	{
		for(int i = 0; i < numNodes; i++)
		{
			Node active = nodes.get(i);
			int activeLane = active.getLane();
			//int activeLanePosition = active.getLanePosition();
			//System.out.println(field.length+ "   x     " + field[0].length);
			
			//Left most lane with which communication may occur
			int leftLimit = ((activeLane * this.width) - this.range)/this.width;
			if(leftLimit < 0)
				leftLimit = 0;
			
			
			//Right most lane with which communication may occur
			int rightLimit = ((activeLane * this.width) + this.range)/this.width;
			if(rightLimit > field.length)
				rightLimit = field.length - 1;
			
			int upperLimit = active.getyLanePosition() + this.range;
			if(upperLimit >= field[0].length)
				upperLimit = field[0].length -1;
			
			int lowerLimit = active.getyLanePosition() - this.range;
			if(lowerLimit < 0)
				lowerLimit = 0;
			
			int activeX = active.getXLanePosition();
			int activeY = active.getyLanePosition();
			
			for(int j = leftLimit; j <= rightLimit; j++)
			{
				for(int k = lowerLimit; k <= upperLimit; k++)
				{
					Node toCheck = field[j][k];
					if(toCheck != null && toCheck.getID() != active.getID())
					{
						
						int toCheckX = toCheck.getXLanePosition() * this.width;
						int toCheckY = toCheck.getyLanePosition();
						double distance = Math.sqrt(Math.pow(toCheckX - activeX, 2) + 
								Math.pow(toCheckY - activeY, 2));
						//System.out.println(distance);
						if(distance < this.range)
							active.addToOHS(toCheck.getID());
					}
				}
			}
		}
	}
	
	public void findTHS()
	{
		for(int i = 0; i < numNodes; i++)
		{
			int doubleRange = this.range * 2;
			
			Node active = nodes.get(i);
			int activeLane = active.getLane();
			//int activeLanePosition = active.getLanePosition();
			//System.out.println(field.length+ "   x     " + field[0].length);
			
			//Left most lane with which communication may occur
			int leftLimit = ((activeLane * this.width) - doubleRange)/this.width;
			if(leftLimit < 0)
				leftLimit = 0;
			
			
			//Right most lane with which communication may occur
			int rightLimit = ((activeLane * this.width) + doubleRange)/this.width;
			if(rightLimit > field.length )
				rightLimit = field.length - 1;
			
			int upperLimit = active.getyLanePosition() + doubleRange;
			if(upperLimit >= field[0].length)
				upperLimit = field[0].length -1;
			
			int lowerLimit = active.getyLanePosition() - doubleRange;
			if(lowerLimit < 0)
				lowerLimit = 0;
			
			int activeX = active.getXLanePosition();
			int activeY = active.getyLanePosition();
			
			
			//System.out.println(leftLimit + "   " + rightLimit);
			
			for(int j = leftLimit; j <= rightLimit; j++)
			{
				for(int k = lowerLimit; k <= upperLimit; k++)
				{
					//System.out.println(j + "    "+ k);
					Node toCheck = field[j][k];
					if(toCheck != null && toCheck.getID() != active.getID())
					{
						
						int toCheckX = toCheck.getXLanePosition() * this.width;
						int toCheckY = toCheck.getyLanePosition();
						double distance = Math.sqrt(Math.pow(toCheckX - activeX, 2) + 
								Math.pow(toCheckY - activeY, 2));
						//System.out.println(distance);
						if(distance < doubleRange)
							active.addToTHS(toCheck.getID());
					}
				}
			}
		}
	}
	/**
	 * All nodes reserve time slots
	 */
	public void reserveTimeSlots()
	{
		List<Integer> hasSlot = new ArrayList<Integer>();
		while (hasSlot.size() < this.numNodes)
		{
			int nodeToAttempt = r.nextInt(this.numNodes);
			while(hasSlot.contains(nodeToAttempt))
			{
				nodeToAttempt = r.nextInt(this.numNodes);
			}
			
			Node toAttemptReservation = nodes.get(nodeToAttempt);
			if(toAttemptReservation.getReservation() == -1) //Node doesn't have a slot yet
			{
				boolean canAdd = true;
				int slotToReserve = r.nextInt(this.numSlots);
				List<Integer> THSToCheck = toAttemptReservation.getTHS();
				
				for(int n: THSToCheck)
				{
					Node toCheck = nodes.get(n);
					ReservationBean[] toCheckSlots = toCheck.getTimeSlots();
					if(toCheckSlots[slotToReserve].getHolder() != -1)
					{
						canAdd = false;
						break;
					}
				}
				if(canAdd)
				{
					toAttemptReservation.setReservation(slotToReserve);
					
					for(int n: THSToCheck)
					{
						Node toUpdate = nodes.get(n);
						//set reservation in all nodes in THS
						toUpdate.setTimeSlotReservation(slotToReserve, 
								toAttemptReservation.getID(), toAttemptReservation.getRemainingReservationDuration());
						//Set initial FI in all nodes in THS
						toUpdate.setFI(slotToReserve, 
								toAttemptReservation.getID());
					}
					hasSlot.add(toAttemptReservation.getID());
				}				
			}
		}
	}
	public List<Node> getAllNodes()
	{
		return nodes;
	}
	/**
	 * Returns the simulation field
	 */
	public Node[][] getField()
	{
		return field;
	}
	
	public void processFrame()
	{
		for(int i = 0; i < this.numSlots; i++)
		{
			List<Message> allMessage = new ArrayList<Message>();
			List<ACKMessage> allACKMessage = new ArrayList<ACKMessage>();
			List<JamMessage> allJamMessage = new ArrayList<JamMessage>();
			
			//Gather Ack and new messages
			for(Node sending: nodes)
			{
				allMessage.add(sending.sendMsg());
				allACKMessage.add(sending.sendACK());
			}
			//Send out all Ack
			for(ACKMessage a: allACKMessage)
			{
				if(a != null)
				{
					int senderID = a.getAckID();
					sendACKInRange(senderID, a);
				}
			}
			//Send out all new messages and retransmits
			for(Message m: allMessage)
			{
				if(m != null)
				{
					int senderID = m.getSenderID();
					sendInRange(senderID, m);
					
					/*
					Node sender = nodes.get(senderID);
					
					List<Integer> senderOHS = sender.getOHS();
					for(int r: senderOHS)
					{
						Node toRecieve = nodes.get(r);
						toRecieve.receieveMsg(m);
					}
					Works but uses a known OHS, rework it similar to the find OHS methods
					Wont rely on knowing the OHS
					*/
					
				}
			}
			for(Node n: nodes)
			{
				n.processBuffer();
			}
			
			//Send out jam if needed
			for(Node sendingJam: nodes)
			{
				allJamMessage.add(sendingJam.sendJam());
			}
			
			for(JamMessage j: allJamMessage)
			{
				if(j != null)
				{
					int senderID = j.getSenderID();
					sendJamInRange(senderID, j);
				}
			}
			
			c.tick();
		}
		for(Node n: nodes)
		{
			n.updateReservationDurations();
			n.clearExpiredDurations();
			n.clearStaleCache(this.maxMemory);
		}
		//System.out.println(c.getFrame());
	}
	public void sendInRange(int senderID, Message m)
	{
		Node active = nodes.get(senderID);
		int activeLane = active.getLane();
		//int activeLanePosition = active.getLanePosition();
		//System.out.println(field.length+ "   x     " + field[0].length);
		
		//Left most lane with which communication may occur
		int leftLimit = ((activeLane * this.width) - this.range)/this.width;
		if(leftLimit < 0)
			leftLimit = 0;
		
		
		//Right most lane with which communication may occur
		int rightLimit = ((activeLane * this.width) + this.range)/this.width;
		if(rightLimit > field.length)
			rightLimit = field.length - 1;
		
		int upperLimit = active.getyLanePosition() + this.range;
		if(upperLimit >= field[0].length)
			upperLimit = field[0].length -1;
		
		int lowerLimit = active.getyLanePosition() - this.range;
		if(lowerLimit < 0)
			lowerLimit = 0;
		
		int activeX = active.getXLanePosition();
		int activeY = active.getyLanePosition();
		
		for(int j = leftLimit; j <= rightLimit; j++)
		{
			for(int k = lowerLimit; k <= upperLimit; k++)
			{
				Node toCheck = field[j][k];
				if(toCheck != null && toCheck.getID() != active.getID())
				{
					
					int toCheckX = toCheck.getXLanePosition() * this.width;
					int toCheckY = toCheck.getyLanePosition();
					double distance = Math.sqrt(Math.pow(toCheckX - activeX, 2) + 
							Math.pow(toCheckY - activeY, 2));
					//System.out.println(distance);
					if(distance < this.range)
						toCheck.rBuffer(m);
				}
			}
		}
	}

	public void sendACKInRange(int senderID, ACKMessage m)
	{
		Node active = nodes.get(senderID);
		int activeLane = active.getLane();
		//int activeLanePosition = active.getLanePosition();
		//System.out.println(field.length+ "   x     " + field[0].length);
		
		//Left most lane with which communication may occur
		int leftLimit = ((activeLane * this.width) - this.range)/this.width;
		if(leftLimit < 0)
			leftLimit = 0;
		
		
		//Right most lane with which communication may occur
		int rightLimit = ((activeLane * this.width) + this.range)/this.width;
		if(rightLimit > field.length)
			rightLimit = field.length - 1;
		
		int upperLimit = active.getyLanePosition() + this.range;
		if(upperLimit >= field[0].length)
			upperLimit = field[0].length -1;
		
		int lowerLimit = active.getyLanePosition() - this.range;
		if(lowerLimit < 0)
			lowerLimit = 0;
		
		int activeX = active.getXLanePosition();
		int activeY = active.getyLanePosition();
		
		for(int j = leftLimit; j <= rightLimit; j++)
		{
			for(int k = lowerLimit; k <= upperLimit; k++)
			{
				Node toCheck = field[j][k];
				if(toCheck != null && toCheck.getID() != active.getID())
				{
					
					int toCheckX = toCheck.getXLanePosition() * this.width;
					int toCheckY = toCheck.getyLanePosition();
					double distance = Math.sqrt(Math.pow(toCheckX - activeX, 2) + 
							Math.pow(toCheckY - activeY, 2));
					//System.out.println(distance);
					if(distance < this.range)
						toCheck.recACK(m);
				}
			}
		}
	}
	public void sendJamInRange(int senderID, JamMessage m)
	{
		Node active = nodes.get(senderID);
		int activeLane = active.getLane();
		//int activeLanePosition = active.getLanePosition();
		//System.out.println(field.length+ "   x     " + field[0].length);
		
		//Left most lane with which communication may occur
		int leftLimit = ((activeLane * this.width) - this.range)/this.width;
		if(leftLimit < 0)
			leftLimit = 0;
		
		
		//Right most lane with which communication may occur
		int rightLimit = ((activeLane * this.width) + this.range)/this.width;
		if(rightLimit > field.length)
			rightLimit = field.length - 1;
		
		int upperLimit = active.getyLanePosition() + this.range;
		if(upperLimit >= field[0].length)
			upperLimit = field[0].length -1;
		
		int lowerLimit = active.getyLanePosition() - this.range;
		if(lowerLimit < 0)
			lowerLimit = 0;
		
		int activeX = active.getXLanePosition();
		int activeY = active.getyLanePosition();
		
		for(int j = leftLimit; j <= rightLimit; j++)
		{
			for(int k = lowerLimit; k <= upperLimit; k++)
			{
				Node toCheck = field[j][k];
				if(toCheck != null && toCheck.getID() != active.getID())
				{
					
					int toCheckX = toCheck.getXLanePosition() * this.width;
					int toCheckY = toCheck.getyLanePosition();
					double distance = Math.sqrt(Math.pow(toCheckX - activeX, 2) + 
							Math.pow(toCheckY - activeY, 2));
					//System.out.println(distance);
					if(distance < this.range)
						toCheck.recieveJam(m);;
				}
			}
		}
	}
	/**
	 * Sets the PoS of all nodes in teh simulation; used for testing different PoS 
	 * without needing to start program constantly
	 * @param p
	 */
	public void setAllNodePoS(double p)
	{
		for(Node n: nodes)
		{
			n.setPoS(p);
		}
	}
	/**
	 * Resets node parameters allowing for the simulation to be rerun
	 * without positions or reservations changing
	 */
	public void resetNodes()
	{
		for(Node n: nodes)
		{
			n.reset();
		}
	}
	/**
	 * Clears node reservations until b% of nodes do not have reservations
	 * @param b
	 */
	public void setInitialNodesWithoutReservation(double b)
	{
		int numToConvert = (int)Math.ceil(b * this.numNodes);
		int converted = 0;
		while(converted < numToConvert)
		{
			int nodeToChange = r.nextInt(this.numNodes);
			Node n = nodes.get(nodeToChange);
			if(n.getReservation() != -1)
			{
				noRes.add(n);
				//-1 is the default value if have no reservation
				List<Integer> THS = n.getTHS();
				for(int t: THS)
				{
					Node removeFrom = nodes.get(t);
					removeFrom.removeNeighbour(nodeToChange);
				}
				
				//System.out.println(converted);
				n.clearReservation();
				converted++;
			}
		}
	}
	public void resetNodesWithoutReservation()
	{
		for(Node n: noRes)
		{
			List<Integer> THS = n.getTHS();
			for(int t: THS)
			{
				if(t!= n.getID())
				{
					Node removeFrom = nodes.get(t);
					removeFrom.removeNeighbour(n.getID());
				}
			}
			n.clearReservation();
		}
	}
}
