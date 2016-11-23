package main;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static main.SysoutWrapper.*;

class UDPServer {
	private static UDPServer server;
	private DatagramSocket serverSocket;
	private DatagramSocket clientSocket;
	// topology init
	public int numOfServers;
	public int numOfNeighbors;
	private int id;
	private int port;
	private int updateInterval;
	private static String[][] servers = new String[4][3]; // id, destination, port of other servers
	private static int[][] costs = new int[4][4];
	private int[] neighbors; // IDs of neighbors
	private Thread sender;
	private Thread receiver;
	public static int inf = Integer.MAX_VALUE; 
	
	public static void main(String args[]) throws Exception {
		// Server information
		// 			  --id-- 					--IP destination--				   --port--
//		servers[0][0] = "1";	servers[0][1] = "ip.address.of.1";	servers[0][2] = "9876"; // for server id 1
//		servers[1][0] = "2";	servers[1][1] = "ip.address.of.2";	servers[1][2] = "9875"; // for server id 2
//		servers[2][0] = "3";	servers[2][1] = "ip.address.of.3";	servers[2][2] = "9874"; // for server id 3
//		servers[3][0] = "4";	servers[3][1] = "ip.address.of.4";	servers[3][2] = "9873"; // for server id 4
		// Distance Costs
//		costs[0][0]	= 0;	costs[0][1] = 7;	costs[0][2] = 4;	costs[0][3] = 5;	// 1 to 1,2,3,4
//		costs[1][0]	= 7;	costs[1][1] = 0;	costs[1][2] = 2;	costs[1][3] = inf;	// 2 to 1,2,3,4
//		costs[2][0]	= 4;	costs[2][1] = 2;	costs[2][2] = 0;	costs[2][3] = 6;	// 3 to 1,2,3,4
//		costs[3][0]	= 5;	costs[3][1] = inf;	costs[3][2] = 6;	costs[3][3] = 0;	// 4 to 1,2,3,4
		
//		int id = Integer.parseInt(args[0]);
		server = new UDPServer();
		
		// Neighbors (Like the diagram on project assignment on csns) we could always change it to whatever right here
//		switch(id){
//		case 1: server.neighbors = new int[3]; // server id 1 has 3 neighbors (2, 3, 4)
//			server.neighbors[0] = 2; server.neighbors[1] = 3; server.neighbors[2] = 4;	
//			break;
//		case 2: server.neighbors = new int[2]; // server id 2 has 2 neighbors (1, 3)
//			server.neighbors[0] = 1; server.neighbors[1] = 3;
//			break;
//		case 3: server.neighbors = new int[3]; // server id 3 has 3 neighbors (1, 2, 4)
//			server.neighbors[0] = 1; server.neighbors[1] = 2; server.neighbors[2] = 4;
//			break;
//		case 4: server.neighbors = new int[2]; // server id 4 has 2 neighbors (1, 3)
//			server.neighbors[0] = 1; server.neighbors[1] = 3;
//			break;
//		}
	}

	// Constructor
	public UDPServer(){
		// for initial command and other commands after
		defineSenderThread();
		sender.start();
	}
	
	// Communication Threads
	public void defineSenderThread(){
		// SENDER Thread
		this.sender = new Thread() {
			int sendToPort = -1;
		    public void run() {
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
		  		      sendToPort = -1;
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
						  //=====---- COMMANDS ----=====\\
			  		      CMD(sentence);
			  		      //=====------------------=====\\
					  } catch (IOException e) {
						  // TODO Auto-generated catch block
						  e.printStackTrace();
					  }
		  		      sendData = sentence.getBytes();
		  		      
		  		      
		  		      if (sendToPort != -1){
			  		      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
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
		  		      }
		  		 
		  		      sendData = null;
		  		      receiveData = null; 
	  		      }	  		     
		    }
		};
	}

	public void defineReceiverThread(DatagramSocket serverSocket, int id) {
		// RECEIVER Thread
		this.receiver = new Thread() {
			byte[] receiveData = new byte[1024];
	    	byte[] sendData = new byte[1024];
			public void run() {
	  		      while(true) {
	  		    	  	receiveData = new byte[1024];
	  		    	  	sendData = new byte[1024];
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
	
	// COMMAND FUNCTION
	private void CMD(String cmd){
		String[] args = cmd.split(" ");
		switch (args[0]) {
		case "server": // server -t <topology-file-name> -i <routing-update-interval>
			if(args.length != 5){
				println("ERROR: Invalid server command!");
				break;
			} else {
				openTopo(args[2]);
				this.updateInterval = Integer.parseInt(args[4]);
				openServerSocket(this.id, this.port);
				routingUpdate(this.updateInterval);
			}
			break;
	    case "close":
    	  clientSocket.close();
    	  serverSocket.close();
    	  System.exit(0);
    	  break;
	    }
	}
	
	
	// OPEN topology file SERVER COMMAND
	private void openTopo(String filename){
		  ArrayList<String> records = new ArrayList<String>();
		  try{
		    BufferedReader reader = new BufferedReader(new FileReader(filename));
		    String line;
		    while ((line = reader.readLine()) != null){
		      records.add(line);
		    }
		    reader.close();
		  }
		  catch (Exception e){
		    System.err.format("Exception occurred trying to read '%s'.", filename);
		    e.printStackTrace();
		  }
		  // GET num of servers from topo
		  String nosarr[] = records.get(0).split(" ");
		  this.numOfServers = Integer.parseInt(nosarr[0]);
		  // GET num of neighbors from topo
		  String nonarr[] = records.get(1).split(" ");
		  this.numOfNeighbors = Integer.parseInt(nonarr[0]);
		  // GET ID from topo
		  String idarr[] = records.get(6).split(" ");
		  this.id = Integer.parseInt(idarr[0]);
		  // GET Port from topo
		  for(int i = 2; i < numOfServers+2; i++){
			  String arr[] = records.get(i).split(" ");
			  // 	--id-- 				 --IP destination--				--port--
			  servers[i-2][0]=arr[0];  servers[i-2][1]=arr[1];  servers[i-2][2] =  arr[2];
			  if(this.id == Integer.parseInt(arr[0])){
				  this.port = Integer.parseInt(arr[2]);
			  }
		  }
		  //Get neighbors IDs
		  neighbors = new int[numOfNeighbors];
		  for(int i = 2+numOfServers; i < 2+numOfServers+numOfNeighbors; i++){
			  String arr[] = records.get(i).split(" ");
			  neighbors[i-(2+numOfServers)]=Integer.parseInt(arr[1]);
			  println(Integer.parseInt(arr[1]));
		  }
		  
	}
	
	// Routing Update on time interval
	private void routingUpdate(int seconds){
		Runnable broadcast = new Runnable() {
		    public void run() {
		        for(int i = 0; i < neighbors.length; i++){
		        	println("send this to "+neighbors[i]);
		        }
		        
		        
		    }
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(broadcast, 0, seconds, TimeUnit.SECONDS);
	}
	
	
	// Start Server
	public void start() {
		receiver.start();	//run receiver thread
	}
	
	private void openServerSocket(int id, int port){
		try {
			this.serverSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			println("ERROR: could not create server socket for server id " + id);
			e.printStackTrace();
		}
		this.defineReceiverThread(this.serverSocket, id);
		server.start(); // starts sender input thread and data receiver thread
	}
	
	//-------GET FUNCTIONS-----------
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

	
}