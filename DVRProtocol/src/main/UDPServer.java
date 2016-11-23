package main;

import java.io.*;
import java.net.*;

import static main.SysoutWrapper.*;

class UDPServer {
	
	private DatagramSocket serverSocket;
	private int id;
	private static String[][] servers = new String[4][3]; // id, destination, port of other servers
	private static int[][] costs = new int[4][4];
	private int[] neighbors = null; // IDs of neighbors
	private Thread sender;
	private Thread receiver;
	public int numOfServers = 4;
	public static int inf = Integer.MAX_VALUE; 
	
	public static void main(String args[]) throws Exception {
		// Server information
		// 			  --id-- 					--IP destination--				   --port--
		servers[0][0] = "1";	servers[0][1] = "ip.address.of.1";	servers[0][2] = "9876"; // for server id 1
		servers[1][0] = "2";	servers[1][1] = "ip.address.of.2";	servers[1][2] = "9875"; // for server id 2
		servers[2][0] = "3";	servers[2][1] = "ip.address.of.3";	servers[2][2] = "9874"; // for server id 3
		servers[3][0] = "4";	servers[3][1] = "ip.address.of.4";	servers[3][2] = "9873"; // for server id 4
		// Distance Costs
		costs[0][0]	= 0;	costs[0][1] = 7;	costs[0][2] = 4;	costs[0][3] = 5;	// 1 to 1,2,3,4
		costs[1][0]	= 7;	costs[1][1] = 0;	costs[1][2] = 2;	costs[1][3] = inf;	// 2 to 1,2,3,4
		costs[2][0]	= 4;	costs[2][1] = 2;	costs[2][2] = 0;	costs[2][3] = 6;	// 3 to 1,2,3,4
		costs[3][0]	= 5;	costs[3][1] = inf;	costs[3][2] = 6;	costs[3][3] = 0;	// 4 to 1,2,3,4
		
		int id = Integer.parseInt(args[0]);
		UDPServer server = new UDPServer(id);
		
		// Neighbors (Like the diagram on project assignment on csns) we could always change it to whatever right here
		switch(id){
		case 1: server.neighbors = new int[3]; // server id 1 has 3 neighbors (2, 3, 4)
			server.neighbors[0] = 2; server.neighbors[1] = 3; server.neighbors[2] = 4;	
			break;
		case 2: server.neighbors = new int[2]; // server id 2 has 2 neighbors (1, 3)
			server.neighbors[0] = 1; server.neighbors[1] = 3;
			break;
		case 3: server.neighbors = new int[3]; // server id 3 has 3 neighbors (1, 2, 4)
			server.neighbors[0] = 1; server.neighbors[1] = 2; server.neighbors[2] = 4;
			break;
		case 4: server.neighbors = new int[2]; // server id 4 has 2 neighbors (1, 3)
			server.neighbors[0] = 1; server.neighbors[1] = 3;
			break;
		}
		
		server.start(); // starts sender input thread and data receiver thread
	}

	public UDPServer(int id){
		this.id = id;
		int port = Integer.parseInt(servers[id-1][2]);
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
		  		    	println("Server>");
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

	

	
}