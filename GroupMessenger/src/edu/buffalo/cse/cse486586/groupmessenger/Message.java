package edu.buffalo.cse.cse486586.groupmessenger;

import com.google.gson.Gson;

/**
 * Class to represent a message
 * @author biplap
 *
 */
public class Message {
	private String message;		// Actual content of the message
	private int sequence;		// sequence number of the message
	private int sender;			// Sender of the message
	private boolean isDeliverable;	// Flag to know if the message is deliverable or not

	/**
	 * Returns the content of the message
	 * @return
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the content of the message
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Returns the sequence number of the message
	 * @return
	 */
	public int getSequence() {
		return sequence;
	}

	/**
	 * Sets the sequence number of the message
	 * @param sequence
	 */
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	/**
	 * Returns the sender of the message
	 * @return
	 */
	public int getSender() {
		return sender;
	}

	/**
	 * Sets the sender of the message
	 * @param sender
	 */
	public void setSender(int sender) {
		this.sender = sender;
	}

	/**
	 * returns boolean value to determine if the  message is deliverable or not
	 * @return
	 */
	public boolean isDeliverable() {
		return isDeliverable;
	}

	/**
	 * Sets the value if message is deliverable or not
	 * @param isDeliverable
	 */
	public void setDeliverable(boolean isDeliverable) {
		this.isDeliverable = isDeliverable;
	}

	/**
	 * Deserializes Message object from it's Json representation
	 * Refer https://code.google.com/p/google-gson/
	 * 
	 * @param jsonString	Json string representing the Message Object
	 * @return	Message object
	 */
	public static Message fromJson(String jsonString){
		Gson gson = new Gson();
		Message message = gson.fromJson(jsonString, Message.class);
		return message;
	}

	/**
	 * Serializes Message object to it's Json representation
	 * Refer https://code.google.com/p/google-gson/
	 * 
	 * @return Json representation of the object
	 */
	public String toJson(){
		Gson gson = new Gson();
		String jsonString = gson.toJson(this);
		return jsonString;
	}
}
