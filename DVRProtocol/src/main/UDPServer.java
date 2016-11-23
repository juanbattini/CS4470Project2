package main;

import java.io.*;
import java.net.*;

import static main.SysoutWrapper.*;

class UDPServer {
	
	private DatagramSocket serverSocket;
	private int id;
	private String[][][] servers; // id, destination, port of other servers
	private int[] neighbors; // IDs of neighbors
	private Thread sender;
	private Thread receiver;
	
	public int numOfServers = 4;

	public UDPServer(int id, int port){
		this.id = id;
		try {
			this.serverSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			println("ERROR: could not create server socket for server id " + this.id);
			e.printStackTrace();
		}
		this.defineCommThreads(this.serverSocket, id);
	}
	
	public InetAddress getIPAddress(){
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			println("ERROR: Couldn't get Local Host from server id "+this.id);
			e.printStackTrace();
		}
		return null;
	}
	
	public int getServerPort() {
		return this.serverSocket.getLocalPort();
	}
	
	public void buildTopology(){
		StringBuffer outputBuffer = new StringBuffer();
		
		
		outputBuffer.append("-----------Topology-----------\n");
		outputBuffer.append(String.valueOf(numOfServers));
		
		println(outputBuffer.toString());
//		return outputBuffer.toString();
		
	}
	
	public void defineCommThreads(DatagramSocket serverSocket, int id) {
		
		
		// Define Communication Threads
		// SENDER Thread
		this.sender = new Thread() {
  		    public void run() {
  		    	  DatagramSocket clientSocket = null;
				try {
					clientSocket = new DatagramSocket();
				} catch (SocketException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	  		      while(true) {
	  		    	  print("Server>");
	  		    	  BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	  		    	  
		  		      InetAddress IPAddress = null;
		  		      try {
		  		    	  IPAddress = InetAddress.getLocalHost();
		  		      } catch (UnknownHostException e) {
		  		    	  // TODO Auto-generated catch block
		  		    	  e.printStackTrace();
		  		      }
		  		      byte[] sendData = new byte[1024];
		  		      byte[] receiveData = new byte[1024];
		  		      String sentence = null;
					  try {
						  sentence = inFromUser.readLine();
					  } catch (IOException e) {
						  // TODO Auto-generated catch block
						  e.printStackTrace();
					  }
		  		      sendData = sentence.getBytes();
		  		      int serverPort = 9876;
		  		      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
			  		  try {
							clientSocket.send(sendPacket);
			  		  } catch (IOException e) {
			  			  // TODO Auto-generated catch block
			  			  e.printStackTrace();
			  		  }
		  		      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		  		      try {
		  		    	  clientSocket.receive(receivePacket);
		  		      } catch (IOException e) {
		  		    	  // TODO Auto-generated catch block
		  		    	  e.printStackTrace();
		  		      }
		  		      String modifiedSentence = new String(receivePacket.getData());
		  		      println("FROM SERVER:" + modifiedSentence);
		  		      if (sentence.trim().equals("close")){
		  		    	  clientSocket.close();
		  		      }
		  		      
		  		      sendData = null;
		  		      receiveData = null;
		  		      
	  		      }
	  		      
  		    }
		};
		// RECEIVER Thread
		this.receiver = new Thread() {
			public void run() {
	  		      while(true) {
	  		    	  	byte[] receiveData = new byte[1024];
	  		    	  	byte[] sendData = new byte[1024];
		  		    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		  		    	try {
		  					serverSocket.receive(receivePacket);
		  				} catch (IOException e) {
		  					println("ERROR: server socket receive failed for server id " + id);
		  					e.printStackTrace();
		  				}
		  		    	String sentence = new String( receivePacket.getData());
		  		    	println("RECEIVED: " + sentence);
		  	            InetAddress IPAddress = receivePacket.getAddress();
		  	            int port = receivePacket.getPort();
		  		    	String capitalizedSentence = sentence.toUpperCase();
		  	            sendData = capitalizedSentence.getBytes();
		  	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		  	            try {
			  				serverSocket.send(sendPacket);
			  			} catch (IOException e) {
			  				println("ERROR: server socket send failed for server id " + id);
			  				e.printStackTrace();
			  			}
		  	            receiveData = null;
		  	            sendData = null;
	  		      }
  		    }
		};
	}
	
	// Start Server
	public void start() {
		sender.start();		//run sender thread
		receiver.start();	//run receiver thread
	}

	public static void main(String args[]) throws Exception {
		UDPServer server = new UDPServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
		server.start();
	}

	
}