package CAH;

import java.util.*;

public class Node 
{
	//Node parameters
	private int ID; //Unique ID of the node
	private int numSlots;
	private double PoS;
	private int range;
	private int lane;
	private int xlanePosition;
	private int ylanePosition;
	
	private List<Integer> OHS = new ArrayList<Integer>();
	private List<Integer> THS = new ArrayList<Integer>();
	
	Clock clock = Clock.CLOCK;
	RetransmitterTracker rt = RetransmitterTracker.RetransmittorTracker;
	
	//Timeslot variables
	private int reservation = -1; // -1 is default if have no reservation
	private int originalReservation; //Backup reservation for when resetting the node
	private ReservationBean[] timeSlots;
	private int[] FI;
	
	Random rng = new Random();
	
	//Retransmit parameters
	private boolean intention = false;
	private int retransIndex = -1;
	private int failIndex = -1;
	private int retransID = -1;
	private int frameRetransmit = -1;
	private Message toCooperate = null;
	
	//Utilization statistics
	private int numSent = 0;
	private int success = 0;
	
	//Clock-collision parameters
	private int lastRecMsgTime = -1;
	private boolean decSuccess = true;
	private Message mayRemove = null; //Msg to remove from buffer if have a collision
	
	//Map of the messages received and the message in memory
	private Map<Integer,ArrayList<MemoryBean>> rMsgs = new HashMap<Integer,ArrayList<MemoryBean>>();
	
	//Retransmission Ack parameters
	private int ackSlot = -1;
	private int ackID = -1;
	
	//Message buffer to deal with collisions
	private List<Message> buffer = new ArrayList<Message>();
	
	//Parameters for reserving a slot
	private int waitCount = 0;
	private int maxReservationDuration;
	private int remainingReservationTime; //Remaining time until need to contend again
	
	//Jam parameters
	private boolean haveJam = false;
	
	public Node(int ID, int numSlots, double PoS, int range,int xlanePosition, int ylanePosition, int maxReservationDuration)
	{
		this.ID = ID;
		this.numSlots = numSlots;
		this.PoS = PoS;
		this.range = range;
		this.xlanePosition = xlanePosition;
		this.ylanePosition = ylanePosition;
		this.maxReservationDuration = maxReservationDuration;
		this.remainingReservationTime = rng.nextInt(this.maxReservationDuration) + 1;
		
		timeSlots = new ReservationBean[this.numSlots];
		FI = new int[this.numSlots];
		for(int i = 0; i < numSlots; i++)
		{
			timeSlots[i] = new ReservationBean(-1, this.maxReservationDuration); //Set all timeslots to -1
			
			FI[i] = -1;
		}
	}
	/**
	 * gets the Nodes ID
	*/
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public int getRemainingReservationDuration()
	{
		return this.remainingReservationTime;
	}
	public int getRange() {
		return range;
	}
	public int getLane() {
		return lane;
	}
	public int getXLanePosition() {
		return xlanePosition;
	}
	public int getyLanePosition() {
		return ylanePosition;
	}
	public int getNumSlots() {
		return numSlots;
	}
	public void setNumSlots(int numSlots) {
		this.numSlots = numSlots;
	}
	public double getPoS() {
		return PoS;
	}
	public void setPoS(double poS) {
		PoS = poS;
	}
	
	public void addToOHS(int addID)
	{
		this.OHS.add(addID);
	}
	public List<Integer> getOHS()
	{
		return this.OHS;
	}
	public void addToTHS(int addID)
	{
		this.THS.add(addID);
	}
	public List<Integer> getTHS()
	{
		return this.THS;
	}
	public int getReservation()
	{
		return this.reservation;
	}
	public void setReservation(int reservation)
	{
		this.originalReservation = reservation;
		this.reservation = reservation;
		this.timeSlots[reservation].setHolder(this.ID);
	}
	/**
	 * Only called during model initialization to initialize the FID values
	 */
	public void setFI(int index, int ID)
	{
		this.FI[index] = ID;
	}
	/**
	 * Only called during model initialization to set reserved timeslots
	 * @param index
	 * @param ID
	 */
	public void setTimeSlotReservation(int index, int ID, int duration)
	{
		this.timeSlots[index].setHolder(ID);
		this.timeSlots[index].setDuration(duration);
	}
	
	public ReservationBean[] getTimeSlots()
	{
		return this.timeSlots;
	}
	public int[] getFI()
	{
		return this.FI;
	}
	/**
	 * Sends out a jam message to all nodes in range if the node detects a collision in its timeslot
	 * @return a jam message
	 */
	public JamMessage sendJam()
	{
		if(this.haveJam == true)
		{
			this.haveJam = false;
			
			//this.reservation = -1;
			this.timeSlots[reservation].setHolder(-1);
			for(int i = 0; i < this.numSlots; i++)
			{
				this.FI[i] = -1;
				this.timeSlots[i].setHolder(-1);
			}
			this.reservation = -1;
			
			
			//Reset retransmission parameters
			this.intention = false;
			this.failIndex = -1;
			this.retransIndex = -1;
			this.frameRetransmit = -1;
			
			//Clear memory
			this.rMsgs.clear();
			this.retransID = -1;
			
			//Clear retrans Ack
			this.ackSlot = -1;
			this.ackID = -1;
			
			//Reset collisions
			this.mayRemove = null;
			this.decSuccess = false;
			
			//Reset reservation times
			this.waitCount = 0;
			
			return new JamMessage(this.ID);

		}else
		{
			return null;
		}
	}
	public void recieveJam(JamMessage m)
	{
		//Recieved a jam message from a node which experianced a collision in its timeslot
		//Need to clear that timeslot and FI
		//Clear buffer?
		if(this.rng.nextDouble() <= this.PoS)
		{
			int rID = this.timeSlots[clock.getTimeSlot()].getHolder();
			if(rMsgs.containsKey(rID))
			{
				rMsgs.remove(rID);
			}
			
			this.timeSlots[clock.getTimeSlot()].setHolder(-1);
			this.timeSlots[clock.getTimeSlot()].setDuration(this.maxReservationDuration);
			
			this.FI[clock.getTimeSlot()] = -1;
		}
	}
	/**
	 * Sends an acknowledgement to a node which wishes to retransmit a message for the node
	 * The message contains the ID of the node which is permitted to send
	 * @return
	 */
	public ACKMessage sendACK()
	{
		int timeSlot = clock.getTimeSlot();
		if(timeSlot == this.ackSlot)
		{
			return new ACKMessage(ackID);
		}
		return null;
	}
	/**
	 * Recieves retransmit acknowledgement and proceeds tih retransmit or aborts
	 * @param m
	 */
	public void recACK(ACKMessage m)
	{
		if(this.rng.nextDouble() <= this.PoS)
		{
			if(this.ID != m.getAckID() && this.intention)
			{
				this.intention = false;
				this.failIndex = -1;
				this.retransIndex = -1;
				this.frameRetransmit = -1;
			}
		}
	}
	/**
	 * Sends a message to a random node within the known OHS
	 * @return
	 */
	public Message sendMsg()
	{
		int currentTimeSlot = clock.getTimeSlot();
		Message toSend = null;
		CoopHeader c = new CoopHeader(intention, failIndex, retransIndex, retransID);
		
		//System.out.println(this.intention);
		if(this.reservation == currentTimeSlot)
		{
			//My time to send
			if(this.OHS.size() > 0)
			{
//				if(this.ID == 300)
//				{
//					System.out.println("Node " + ID + "sending at: " + clock.getTimeSlot());
//				}
				//Have someone I can send to
				int nodeToRecieve = this.OHS.get(rng.nextInt(this.OHS.size()));
				String mID = this.ID + "" + clock.getTime();
				this.numSent++;
				toSend = new Message(Integer.parseInt(mID), this.ID, nodeToRecieve,
						this.reservation, clock.getTime(),false, this.FI, c);
				
				//Decrement remaining reservation time, if the reminaing reservation time is 0, set reservation to 0 and reset wait counter
				this.remainingReservationTime--;
				if(this.remainingReservationTime == 0)
				{
					
					this.timeSlots[this.reservation].setHolder(-1);
					this.reservation = -1;
					this.waitCount = 0;
				}
			}
		}else if(this.retransIndex == currentTimeSlot && 
						this.frameRetransmit == clock.getFrame())
		{
			toSend = this.toCooperate;
			
			this.intention = false;
			this.failIndex = -1;
			this.retransIndex = -1;
			this.frameRetransmit = -1;
			this.retransID = -1;
			//System.out.println("Sending retransmit");
		}else if(this.reservation == -1) //Node does not have a slot and cannot sent
		{								//Atempt to reserve a slot
			//System.out.println("Shouldnt be here");
		}
		return toSend;
	}
	public void rBuffer(Message m)
	{
		buffer.add(m);
	}
	/**
	 * Runs after all nodes in the model have been processed
	 * Will call the receive msg method if size = 1 (no collision)
	 * or will simply clear the buffer (collision if more than 1 msg in a timeslot)
	 * No action if buffer is 0 (no msgs received)
	 */
	public void processBuffer()
	{
		if(buffer.size() == 1)
		{
			this.receieveMsg(buffer.get(0));
		}else if(buffer.size() > 1 && clock.getTimeSlot() == this.reservation)
		{
			//Check for merge collision
			
			//According to paper if there is a collision need to release timeslot if there is a collision
			//How do i release timeslot such that everyone knows about it? Send a short msg somehow?
			int numNewMsg = 0;
			for(int i = 0; i < buffer.size(); i++)
			{
				if(!buffer.get(i).isRetransmit())
				{
					numNewMsg++;
				}
			}
			if(numNewMsg > 1)
			{
				this.haveJam = true;
				//System.out.println("Node " + this.ID + " has jam with " + buffer.get(1).getSenderID() + " at timeslot " + clock.getTimeSlot());
			}
		}
		buffer.clear();
	}
	/**
	 * Method to recieve a msg
	 * @param m
	 */
	public void receieveMsg(Message m)
	{
		if(rng.nextDouble() <= this.PoS)
		{

			boolean isNewMsg = !m.isRetransmit();
			int receiveTimeSlot = clock.getTimeSlot();
			int senderID = m.getSenderID();
			int receiverID = m.getReceiverID();
			boolean containedInOHS = containedInOHS(receiverID);
			
			if(this.lastRecMsgTime != clock.getTime())
			{
				this.lastRecMsgTime = clock.getTime();
				
				if(isNewMsg)
				{
					if(containedInOHS)
					{
						rt.AddPossibleRetransmitNode(m);
					}
					this.FI[receiveTimeSlot] = senderID; //Set FI to show we know node s has the sth timeslot
					this.addToNeihgbourTables(senderID, m.getFID());
					
					if(this.reservation == -1)
					{
						//Neighbourship tables and values have all already been updated
						//Need to check time and then choose a slot
						this.attemptReservation(m);
						return;
					}
					
					if(receiverID == this.ID)
					{
						this.lastRecMsgTime = clock.getTime();
						this.success++;
						this.decSuccess = true;
					}else if(containedInOHS)
					{
						this.mayRemove = m;
						//Message is not for me BUT it is within my OHS, maybe I can cooperate
						if(rMsgs.containsKey(receiverID))//Recieved a message for this node before
						{
							rMsgs.get(receiverID).add(new MemoryBean(m,clock.getFrame()));
						}else //Havent recieved a message for this node before
						{
							ArrayList<MemoryBean> temp = new ArrayList<MemoryBean>();
							temp.add(new MemoryBean(m,clock.getFrame()));
							rMsgs.put(receiverID, temp);
						}
					}


					if(m.getCoHeader().getReceiverID() == this.ID)
					{
						//There will be a retransmit for me later, need to Ack it during the 
						//retrans slot
						this.ackID = senderID;
						this.ackSlot = m.getCoHeader().getIndexRetransmit();
					}

					boolean retransOverlap = haveRetransCollision(m.getCoHeader());
					if(retransOverlap)
					{
						//Another node already planning to retransmit the message I am trying to
						//Reset retransmit parameters
						this.intention = false;
						this.failIndex = -1;
						this.retransIndex = -1;
						this.frameRetransmit = -1;
					}
					
					this.clearSuccessFromCache(m);
					
					boolean canHelpSender = recievedForThisSenderBefore(senderID);
					//System.out.println(this.intention + "     " + canHelpSender);
					if(!this.intention && canHelpSender)
					{
						//System.out.println("got here");
						Message retransmit = findFailedMsg(m);
						//System.out.println(retransmit);
						if(retransmit != null)
						{
							int whenToRetransmit = findRetransmitSlot(receiveTimeSlot);
							if(whenToRetransmit != -1)
							{
								//A timeslot for me to retransmit was found, the frame to retransmit was set
								int originalSender = retransmit.getSenderID();
								int originalReceiver = retransmit.getReceiverID();
								int originalTimeSlot = retransmit.getTimeSlot();
								int originalID = retransmit.getMsgID();
								int originalCreationTime = retransmit.getCreationTime();

								this.intention = true;
								this.failIndex = originalTimeSlot;
								this.retransIndex = whenToRetransmit;
								this.retransID = originalReceiver;
//
//								toCooperate = new Message(originalID, originalSender, originalReceiver, originalTimeSlot, 
//										originalCreationTime, true, retransmit.getFID(), null);
//								toCooperate.setRetransmit(true);
								
								toCooperate = retransmit;
								toCooperate.setRetransmit(true);

								//System.out.println("retransmit triggered");
							}
						}else
						{
							//All recieved
						}
					}
				}else
				{
					//Message is a retransmit
					if(m.getReceiverID() == this.ID)
					{
						this.FI[m.getTimeSlot()] = m.getSenderID();
						this.decSuccess = true;
						success++;
						//System.out.println("Retransmit recieved");
					}
				}
			}else
			{
//				if(decSuccess)
//				{
//					success--;
//					decSuccess = false;
//				}else if(mayRemove != null)
//				{
//					Collection<ArrayList<MemoryBean>> c = this.rMsgs.values();
//					for(ArrayList<MemoryBean> mb: c)
//					{
//						for(int i = 0; i < mb.size(); i++)
//						{
//							MemoryBean b = mb.get(i);
//							if(b.getM().getMsgID() == m.getMsgID())
//							{
//								mb.remove(i);
//								break;
//							}
//						}
//					}
//					mayRemove = null;
//				}
				System.out.println("Collision");
			}
		}else
		{
			this.FI[clock.getTimeSlot()] = -1;
		}
	}
	/**
	 * Finds a slot in which the node will attempt a retransmission
	 * @param receiveTimeSlot
	 * @return
	 */
	private int findRetransmitSlot(int receiveTimeSlot)
	{
		int slot = -1;
		for(int i = this.reservation; i < this.numSlots; i++)
		{
			if(this.timeSlots[i].getHolder() == -1)
			{
				slot = i;
				this.frameRetransmit = clock.getFrame() + 1;
				break;
			}
		}
		return slot;
	}
	/**
	 * Returns true if the passed ID is in the nodes OHS, false otherwise
	 * @param ID
	 * @return
	 */
	private boolean containedInOHS(int ID)
	{
		boolean contained = false;
		for(int i: this.OHS)
		{
			if(i == ID)
			{
				contained = true;
				break;
			}
		}
		return contained;
	}
	/**
	 * Returns true if the passed ID is in the nodes THS, false otherwise
	 * @param ID
	 * @return
	 */
	private boolean containedInTHS(int ID)
	{
		boolean contained = false;
		for(int i: this.THS)
		{
			if(i == ID)
			{
				contained = true;
				break;
			}
		}
		return contained;
	}
	public void addToNeihgbourTables(int sID, int[] mFI)
	{
		
		for(int i =0; i < this.numSlots; i++)
		{
			if(this.timeSlots[i].getHolder() == -1 && mFI[i] != -1)
			{
				//System.out.println("Message from " + sID + " at " + clock.getTimeSlot());
				this.timeSlots[i].setHolder(mFI[i]);
				this.timeSlots[i].setDuration(this.maxReservationDuration);
				if(!this.containedInOHS(sID))
				{
					//Add sender to OHS if not already there
					this.OHS.add(sID);
				}
				
				
				for(int j = 0; j < this.numSlots; j++)
				{
					if(mFI[j] != -1 && !this.containedInTHS(mFI[j]))
					{
						//Add FI entries in the message FI to THS if not already there
						this.THS.add(mFI[j]);
						this.timeSlots[j].setHolder(mFI[j]);
						this.timeSlots[j].setDuration(this.maxReservationDuration); //****MAY NEED TO CHANGE THIS ***
					}
				}
				
			}
//			else if(this.timeSlots[i] != -1 && mFI[i] != -1 && this.timeSlots[i] != mFI[i])
//			{
////				System.out.println(timeSlots[i] + "     " + mFI[i] + "   " + i);
////				
////				for(int j = 0; j < this.numSlots; j++)
////				{
////					System.out.print(this.FI[j] + " , ");
////				}
////				System.out.println("");
////				for(int j = 0; j < this.numSlots; j++)
////				{
////					System.out.print(mFI[j] + " , ");
////				}
////				System.exit(0);
////			//	System.out.println("TIMESLOT COLLISION");
//			}
		}
	}
	/**
	 * Multiple nodes wish to cooperate; have a collision
	 * @param c
	 * @return
	 */
	private boolean haveRetransCollision(CoopHeader c)
	{
		boolean haveCollision = false;
		if(c.getIntention() == true && this.intention == true)
		{
			if(c.getIndexFailure() == this.failIndex)
			{
				haveCollision = true;
			}
		}
		return haveCollision;
	}
	/**
	 * Checks the list of received messages to see if have received for this node before
	 * @param senderID
	 * @return
	 */
	private boolean recievedForThisSenderBefore(int senderID)
	{
		boolean RFTSB = false;
		if(this.rMsgs.containsKey(senderID))
		{
			RFTSB = true;
		}
		return RFTSB;
	}
	/**
	 * Finds a failed message for the sender of the passed message if a failed message exists
	 * @param m
	 * @return
	 */
	private Message findFailedMsg(Message m)
	{
		ArrayList<MemoryBean> r = rMsgs.get(m.getSenderID());
		//System.out.println(rMsgs.containsKey(m.getSenderID()));
		for(int i = 0; i < r.size(); i++)
		{
			Message possibleRetransmit = r.get(i).getM();
			int slotReceived = possibleRetransmit.getTimeSlot();
			if(this.FI[slotReceived] != -1 && m.getFID()[slotReceived] != this.FI[slotReceived] && m.getFID()[slotReceived] == -1)
			{
				//System.out.println("got here");
				r.remove(i);
				return possibleRetransmit;
			}else
			{
				r.remove(i);
			}
		}
		return null;
	}
	private void clearSuccessFromCache(Message m)
	{
		int[] mFI = m.getFID();
		ArrayList<MemoryBean> r = rMsgs.get(m.getSenderID());
		if(r != null)
		{
			for(int i = 0; i < r.size(); i++)
			{
				Message possibleRemove = r.get(i).getM();
				if((mFI[possibleRemove.getTimeSlot()] == this.FI[possibleRemove.getTimeSlot()]) && (r.get(i).getFrameCreated() == clock.getFrame() -1) && this.FI[possibleRemove.getTimeSlot()] != 0)
				{
					//System.out.println("removing");
					r.remove(i);
				}
			}
		}
	}
	/**
	 * Clears cached messages in memory greater than the passed value
	 */
	public void clearStaleCache(int maxStored)
	{
		int currentFrame = clock.getFrame();
		Set<Integer> keys = rMsgs.keySet();
		Iterator<Integer> it = keys.iterator();
		while(it.hasNext())
		{
			int key = (int) it.next();
			List<MemoryBean> memory = rMsgs.get(key);
			for(int i = 0; i < memory.size(); i++)
			{
				int memoryCreationFrame = memory.get(i).getFrameCreated();
				int diff = currentFrame - memoryCreationFrame;
				if(diff > maxStored)
				{
					memory.remove(i);
				}
			}
		}
	}
	public void attemptReservation(Message m)
	{
		//System.out.println(ID);
		if(this.waitCount < this.numSlots)
		{
			this.waitCount++;
			return;
		}else
		{
			int possibleSlot = findRandomTimeslot();
			if(possibleSlot != -1)
			{
				this.timeSlots[possibleSlot].setDuration(this.ID);
				this.reservation = possibleSlot;
				//System.out.println("Node" + this.ID + " reserved slot " + reservation);
			}

		}
	}
	/**
	 * Returns a random timeslot for the node to reserve
	 * @return
	 */
	public int findRandomTimeslot()
	{
		List<Integer> possible = new ArrayList<Integer>();
		int timeSlot;
		for(int i = 0; i < this.numSlots; i++)
		{
			//System.out.println(this.timeSlots[i].getHolder());
			if(this.timeSlots[i].getHolder() == -1)
			{
				possible.add(i);
			}
		}

		if(possible.size() == 0)
		{
			return -1;
		}else
		{
			timeSlot = this.rng.nextInt(possible.size());
			//System.out.println("Node " + this.ID + " slot: " + timeSlot);
			return timeSlot;
		}
	}
	/**
	 * Resets node parameters
	 */
	public void reset()
	{
		for(int i = 0; i < this.numSlots; i++)
		{
			//this.timeSlots[i] = new ReservationBean(-1,this.maxReservationDuration); //Maybe?
			this.FI[i] = -1;
		}
		//Reset statistics
		this.success = 0;
		this.numSent = 0;
		
		//Reset retransmission parameters
		this.intention = false;
		this.failIndex = -1;
		this.retransIndex = -1;
		this.frameRetransmit = -1;
		
		//Clear memory
		this.rMsgs.clear();
		this.retransID = -1;
		
		//Clear retrans Ack
		this.ackSlot = -1;
		this.ackID = -1;
		
		//Reset collisions
		this.mayRemove = null;
		this.decSuccess = false;
		
		//Reset reservation times
		this.waitCount = 0;
		
		//Reset to orginal reservation
		this.reservation = originalReservation;
	}
	/**
	 * removed the passed niehgbour ID from relationship tables
	 * @param ID
	 */
	public void removeNeighbour(int nID)
	{
		//this.OHS.remove(ID);
		for(int i = 0; i < OHS.size(); i++)
		{
			if(OHS.get(i) == nID)
			{
				OHS.remove(i);
				break;
			}
		}
		//this.THS.remove(ID);
		for(int i = 0; i < THS.size(); i++)
		{
			if(THS.get(i) == nID)
			{
				THS.remove(i);
				break;
			}
		}
		for(int i = 0; i < this.numSlots; i++)
		{
			if(this.timeSlots[i].getHolder() == nID)
			{
				this.timeSlots[i].setHolder(-1);
				this.FI[i] = -1;
			}
		}
	}
	/**
	 * Clears a nodes reservation and its OHS and THS
	 */
	public void clearReservation()
	{
	//	System.out.println("Node " + this.ID + " reservation cleared");
		this.reservation = -1;
		for(int i = 0; i < this.numSlots; i++)
		{
			this.timeSlots[i].setHolder(-1);
			this.FI[i] = -1;
		}
		this.OHS = new ArrayList<Integer>();
		this.THS = new ArrayList<Integer>();
	}
	/**
	 * Decrements durations of timeslots
	 */
	public void updateReservationDurations()
	{
		for(int i = 0; i < this.numSlots; i++)
		{
			this.timeSlots[i].decrementDuration();
		}
	}
	/**
	 * Clears durations which have expired (Duration = 0)
	 */
	public void clearExpiredDurations()
	{
		for(int i = 0; i < this.numSlots; i++)
		{
			if(this.timeSlots[i].getDuration() == 0)
			{
				this.timeSlots[i].setHolder(-1);
			}
		}
	}
	public int getNumSent()
	{
		return this.numSent;
	}
	public int getSuccess()
	{
		return this.success;
	}
	
}
