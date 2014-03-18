package CAH;

import java.util.*;
import java.io.*;
/**
 * The main class responsible for driving the simulation, collecting results and printing them
 * to console and to Results.txt
 * @author Ross Kitsis
 *
 */
public class Driver 
{
	public static void main(String[] args) throws FileNotFoundException
	{
		//Model parameters
		int numNodes = 500;
		int numLanes = 2;
		int width = 5;
		int range = 200;
		double PoS = 0.5;
		double density = 0.0175;
		int numSlots = 60;

		//AdHoc parameters
		double b = 0.1;
		int reservationDuration = Integer.MAX_VALUE;
		
		//Simulation run parameters
		int numTrials = 1;
		int numFrames = 5000;

		//File output parameters
		File f = new File("Results.txt");
		PrintWriter pw = new PrintWriter(f);
		
		//Clock
		Clock c = Clock.CLOCK;
		c.setNumSlots(numSlots);
		
		//Max time message in memory
		int maxMemory = 100;
		
		//Model
		Model m = new Model(numNodes, numLanes, width, PoS,
				density, range, b, reservationDuration, numSlots, maxMemory);
		m.buildModel();
		
		RetransmitterTracker rt = RetransmitterTracker.RetransmittorTracker;
		
		List<Node> nodes = m.getAllNodes();
//		for(int i = 0; i < numNodes; i++)
//		{
//			System.out.println(nodes.get(i).getxLanePosition() + "     " + nodes.get(i).getyLanePosition());
//		}
//		
		//Node[][] field = m.getField();
		Node n = nodes.get(150);
		Node n2 = nodes.get(151);
		System.out.println("");
		System.out.println(n.getOHS().size());
		System.out.println(n.getTHS().size());
		System.out.println(n.getReservation());
		
		/*
		int[] v = n.getTimeSlots();
		for(int t: v)
		{
			System.out.print(t + ",");
		}
		System.out.println("");
		v = n2.getTimeSlots();
		for(int t: v)
		{
			System.out.print(t + ",");
		}*/
		
		System.out.println("\n\n\n");
		double avgNumNodes = 0;
		for(int num = 0; num < numTrials; num++)
		{
			for( int p = 0; p <=100; p = p + 5)
			{
				m.setAllNodePoS(p/100.0);
				//m.setAllNodePoS(50/100.0);
				
				avgNumNodes = m.getAverageNumberOfOHSSize();
				
				for(int i = 0; i < numFrames; i++)
				{
					m.processFrame();
				}
				
				int suc = 0;
				int numSent = 0;
				for(int i = 0; i < numNodes; i++)
				{
					numSent += nodes.get(i).getNumSent();
					suc += nodes.get(i).getSuccess();
				}
				/*v = n2.getTimeSlots();
				for(int t: v)
				{
					System.out.print(t + ",");
				}*/
				//System.out.println("");
				System.out.println(p + "," + (double)suc/numSent + "," + rt.getAverageNumberOfRetransmittors());
				pw.println(p + "," + (double)suc/numSent + "," + rt.getAverageNumberOfRetransmittors());
				
				c.reset();
				m.resetNodes();
				m.resetNodesWithoutReservation();
				rt.reset();
			}
			pw.println(avgNumNodes);
		}
//		for(int i = 0; i < numNodes; i++)
//		{
//			Node a = nodes.get(i);
//			int[] slots = a.getTimeSlots();
//			for(int j = 0; j < numSlots; j++)
//			{
//				System.out.print(slots[j] + " , ");
//			}
//			System.out.println("");
//		}
		
		pw.close();
	}
}
