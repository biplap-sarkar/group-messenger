package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
/*****************************************************************************/
/*****************************************************************************/
/**           Brief Description of the Solution                             **/
/** The solution achieves Total + Causal ordering by achieving Total + FIFO **/
/** ordering using a sequencer. Each process is assigned an id which starts **/
/** from 0 to n-1. Process 0 acts as sequencer.                             **/
/** 
/** Algorithm in sequencer node:- Each process sends messages to be 
/** multicasted to the sequencer by putting their own sequence              **/
/** numbers to them and ensuring the sequences are in increasing order.     **/
/** The sequencer maintains next expected sequence (starting from seq 0)    **/
/** for each process. If the sequencer receives a message from a process    **/
/** with sequence number equal to the expected sequence number for that     **/
/** process, it puts it's own sequence number in the message, marks the     **/
/** message as deliverable and sends it to all the processes. In case       **/
/** message received by the sequence is not equal to the expected sequence  **/
/** from the sending process, it buffers the message to be multicasted      **/
/** later.                                                                  **/
/** 
/** Algorithm in other nodes:- Each node maintains next sequence of the     **/
/** message from the sequencer. If the message from the sequencer contains  **/
/** sequence which is expected, it puts the message into the content        **/
/** If it does not match, it buffers the message to be put later.           **/
/**																			**/
/** Guarantee for FIFO ordering :- As each process sends messages with      **/
/** increasing sequence numbers and the sequencers accepts them in          **/
/** increasing order and then multicasts, FIFO ordering is preserved.       **/
/**																			**/
/** Guarantee for Total ordering :- As the sequencer multicasts the         **/
/** the messages with increasing sequence numbers and all the processes     **/
/** deliver the messages in that order only, each process delivers messages **/
/** in same order and hence Total ordering is preserved.                    **/


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	static final int BASE_ID = 5554;
	static final int SERVER_PORT = 10000;
	static final int NODE_COUNT = 5;
	private int senderSequence = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		final Integer myId = (Integer.parseInt(portStr)-BASE_ID)/2;





		/*
		 * TODO: Use the TextView to display your messages. Though there is no grading component
		 * on how you display the messages, if you implement it, it'll make your debugging easier.
		 */
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		/*
		 * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
		 * OnPTestClickListener demonstrates how to access a ContentProvider.
		 */
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		/*
		 * TODO: You need to register and implement an OnClickListener for the "Send" button.
		 * In your implementation you need to get the message from the input box (EditText)
		 * and send it to other AVDs in a total-causal order.
		 */
		findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
			
			/**
			 * Creates a Message object by adding the process id and sequence number
			 * of the process and send it to the sequencer
			 */
			@Override
			public void onClick(View v) {
				EditText editText = (EditText) findViewById(R.id.editText1);
				String msg = editText.getText().toString();
				editText.setText("");
				Message message = new Message();
				message.setSender(myId);
				message.setSequence(senderSequence);
				message.setMessage(msg);
				message.setDeliverable(false);
				new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
			}

		});

		try {
			/*
			 * Create a server socket as well as a thread (AsyncTask) that listens on the server
			 * port.
			 * 
			 * AsyncTask is a simplified thread construct that Android provides. Please make sure
			 * you know how it works by reading
			 * http://developer.android.com/reference/android/os/AsyncTask.html
			 */
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket,myId);
		} catch (IOException e) {
			/*
			 * Log is a good way to debug your code. LogCat prints out all the messages that
			 * Log class writes.
			 * 
			 * Please read http://developer.android.com/tools/debugging/debugging-projects.html
			 * and http://developer.android.com/tools/debugging/debugging-log.html
			 * for more information on debugging.
			 */
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}

	/**
	 * AsyncTask which listens for incoming messages from the nodes.
	 * @author biplap
	 *
	 */
	private class ServerTask extends AsyncTask<Object, String, Void> {
		private int expectedSequence = 0;
		private SparseArray<Message> waitMap = new SparseArray<Message>();
		public static final String KEY_FIELD = "key";
		public static final String VALUE_FIELD = "value";
		private final ContentResolver mContentResolver = getContentResolver();
		private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");

		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}
		
		/**
		 * This method implements the main logic of delivering the message or scheduling it.
		 * 
		 * 1.) If the incoming message is marked as deliverable, it checks if the sequence of the
		 * message is same as the expected sequence and delivers it to UI if it is.
		 * If not, it stores the message in waitMap key value pair.
		 * 
		 * 2.) If the incoming message is not marked as deliverable and this process is working
		 * as sequencer, it schedules the message using the sequencer.
		 * 
		 * 3.) If the incoming message is not marked as deliverable and this process is not working
		 * as sequencer, it just ignores the message
		 */
		@Override
		protected Void doInBackground(Object... params) {
			ServerSocket serverSocket = (ServerSocket) params[0];
			final Integer myId = (Integer) params[1];


			while(true){
				try {
					Socket soc = serverSocket.accept();
					BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
					String rawMsg = br.readLine();
					Message inMsg = Message.fromJson(rawMsg);
					if(inMsg.isDeliverable()){		// Message is marked as deliverable
						int sequence = inMsg.getSequence();
						if(expectedSequence == sequence){	// The sequence of message is same as expected sequence
							
							// put in content provider
							ContentValues values = new ContentValues();
							values.put(KEY_FIELD, String.valueOf(sequence));
							values.put(VALUE_FIELD, inMsg.getMessage());
							mContentResolver.insert(mUri, values);
							
							// publish progress
							publishProgress(inMsg.getMessage());
							
							// increment the next expected sequence from the sequencer
							expectedSequence++;
							while(waitMap.get(expectedSequence)!=null){		// Since the expected sequence number is incremented now
																			// check if there are any pending message in waitMap whose
																			// sequence number matches the current sequence number and
																			// deliver them.
								Message waitingMessage = waitMap.get(expectedSequence);
								sequence = waitingMessage.getSequence();
								values = new ContentValues();
								values.put(KEY_FIELD, String.valueOf(sequence));
								values.put(VALUE_FIELD, waitingMessage.getMessage());
								mContentResolver.insert(mUri, values);
								publishProgress(inMsg.getMessage());
								expectedSequence++;
							}
						}
						else{
							waitMap.put(sequence, inMsg);		// Message is out of order, put it in waitMap
						}
					}
					else{		// Message is not marked as deliverable
						if(myId == 0){			// This process is the sequencer
							Sequencer sequencer = Sequencer.getInstance();
							sequencer.scheduleMessage(inMsg);		// Schedule the message
						}
					}
					soc.close();
				} catch (IOException e) {
					Log.e(TAG, "ServerTask socket IOException");
				}
			}
		}

		protected void onProgressUpdate(String...strings) {
			/*
			 * The following code displays what is received in doInBackground().
			 */
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setMovementMethod(new ScrollingMovementMethod());
			tv.append(strings[0]+"\n");
			return;
		}
	}

	/***
	 * ClientTask is an AsyncTask that should send a string over the network.
	 * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
	 * an enter key press event.
	 * 
	 * For this project, the ClientTask is invoked each time the send button is clicked.
	 * The job of the ClientTask is to send the message to the sequencer.
	 * 
	 * @author stevko
	 *
	 */
	private class ClientTask extends AsyncTask<Message, Void, Void> {

		/**
		 * Send the message to the sequencer
		 */
		@Override
		protected Void doInBackground(Message... msgs) {
			try {
				Message message = msgs[0];
				int remotePort = BASE_ID * 2;
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						remotePort);

				String msgToSend = message.toJson();

				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				bw.write(msgToSend);
				bw.flush();
				socket.close();
				senderSequence++;
			} catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException");
			}

			return null;
		}
	}
}
