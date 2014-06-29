package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

import android.util.Log;
import android.util.SparseArray;

/**
 * Singleton class to which implements sequencer to preserve total 
 * and causal order by preserving FIFO and total ordering.
 * @author biplap
 *
 */
public class Sequencer {
	private static Sequencer sequencer = new Sequencer();
	private static String TAG = Sequencer.class.getSimpleName();
	private static final int NODE_COUNT = 5;
	static final int BASE_ID = 5554;
	int []expectedSequence = new int[NODE_COUNT];	// Array to keep track of next expected sequence from each node.
	int nextSequence = 0;

	/* SparseArray for each node which is used as a key value pair to keep out of order messages*/
	private ArrayList<SparseArray<Message>> waitMap = new ArrayList<SparseArray<Message>>(NODE_COUNT);

	/**
	 * Initializes waitMap list and sets expected sequence for each node to be 0.
	 * Implemented as private to restrict access to public constructor
	 */
	private Sequencer(){
		for(int i=0;i<NODE_COUNT;i++){
			waitMap.add(new SparseArray<Message>());
			expectedSequence[i]=0;
		}
	}

	/**
	 * Returns a single instance of the sequencer
	 * @return
	 */
	public static Sequencer getInstance(){
		return sequencer;
	}

	/**
	 * Schedules message msg to all the nodes.
	 * Sends it immediately if sequence number of msg is current expected
	 * sequence from the node from it comes from.
	 * If the sequence number is higher than expected, it keeps the message
	 * in waitMap key value pair belonging to sender node.
	 * 
	 * @param msg Message to be scheduled
	 */
	public void scheduleMessage(Message msg){
		int sender = msg.getSender();
		int sequence = msg.getSequence();
		if(expectedSequence[sender]==sequence){		// This was the expected sequence number
			msg.setSequence(nextSequence);			// Stamp the message with sequence number of sequencer
			msg.setDeliverable(true);				// Mark the message as deliverable
			nextSequence++;							
			multicastMessage(msg);					// Multicast the message
			expectedSequence[sender]++;
			while(waitMap.get(sender).get(expectedSequence[sender])!=null){		// Since the message was sent, the expected sequence
				// number of the sender is incremented, now check if
				// there were any pending message from the sender aligned
				// with the expected sequence. If there are some message
				// then send it.
				Message waitingMsg = waitMap.get(sender).get(expectedSequence[sender]);
				waitingMsg.setDeliverable(true);
				waitingMsg.setSequence(nextSequence);
				nextSequence++;
				multicastMessage(waitingMsg);
				expectedSequence[sender]++;
			}
		}
		else{
			waitMap.get(sender).put(sequence, msg);		// Message is out of sequence, keep it in waitMap designated for sender
		}
	}

	/**
	 * Multicasts the message msg to all the nodes in the network
	 * @param msg Message to be multicasted
	 */
	private void multicastMessage(Message msg) {
		for (int i = 0; i < NODE_COUNT; i++) {
			try {
				int remotePort = (BASE_ID + i*2) * 2;
				Socket socket = new Socket(InetAddress.getByAddress(new byte[] {
						10, 0, 2, 2 }), remotePort);

				String msgToSend = msg.toJson();
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						socket.getOutputStream()));
				bw.write(msgToSend);
				bw.flush();
				socket.close();
			} catch (IOException ex) {
				Log.v(TAG, ex.getLocalizedMessage());
			}
		}
	}
}
