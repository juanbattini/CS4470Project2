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
	private String IP;
	private int port;
	private int updateInterval;
	private static String[][] servers = new String[4][4]; // 0:id, 1:destination, 2:port of other servers, 3:online(1=true, 0=false) 
//	private static int[][] costs = new int[4][4];
	private static int[] costs = new int[4];
	private static int[] initCosts = new int[4];
	private int[] 	neighbors; // neighbor ids,
	private int[]	timeIntervals = new int[4];
	private int[]	receivedPackets = new int[4];
	private int[]	missedPackets = new int[4];
	private int[]	secondsMissed = new int[4];
	
	
	
	private Thread sender;
	private Thread receiver;
	public static int inf = Integer.MAX_VALUE; 
	
	public static void main(String args[]) throws Exception {
		server = new UDPServer();
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
		  		      byte[] sendData = new byte[56];
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
		  		      // Doesn't send anything if port isn't defined
		  		      if (sendToPort != -1){
			  		      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
				  		  try {
								clientSocket.send(sendPacket);
				  		  } catch (IOException e) {
				  			  // TODO Auto-generated catch block
				  			  e.printStackTrace();
				  		  }
		  		      }
		  		      sendData = null;
	  		      }	  		     
		    }
		};
	}

	public void defineReceiverThread(DatagramSocket serverSocket, int id) {
		// RECEIVER Thread
		this.receiver = new Thread() {
			byte[] receiveData = new byte[56];
			public void run() {
	  		      while(true) {
	  		    	  	receiveData = new byte[56];
	  		    	  	//sendData = new byte[4];
		  		    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		  		    	try {
		  					serverSocket.receive(receivePacket);
		  					
		  				} catch (IOException e) {
		  					println("ERROR: server socket receive failed for server id " + id);
		  					e.printStackTrace();
		  				}
		  		    	String sentence = new String( receivePacket.getData());
		  		    	String args[] = sentence.split(" ");
		  		    	// Update Interval 
		  		    	if(args[0].equals("UPDATE-INTERVAL") && args.length == 4){ // Receiving update packet
		  		    		// if the first word in the packet is 'UPDATE-INTERVAL'
		  		    		// 2nd arg is id from sending server
		  		    		int id = Integer.parseInt(args[1]);
		  		    		// 3rd is the cost from the sending server
		  		    		int cost = Integer.parseInt(args[2]);
		  		    		// 4th is time interval from sending server
		  		    		int interval = Integer.parseInt(args[3].trim());
		  		    		servers[id-1][3] 		= "1"; 		// server is online
		  		    		costs[id-1] 			= cost;		//neighbor cost
		  		    		timeIntervals[id-1] 	= interval; // set time interval of server of that id
		  		    		receivedPackets[id-1] 	+= 1;		// increase packets received by one for that id
		  		    		secondsMissed[id-1]		= 0;		// reset miss timer
		  		    		println("RECEIVED A MESSAGE FROM SERVER "+id);
		  		    	}
		  		    	
		  		    	if(args[0].equals("?") && args.length == 3){ 
		  		    		int fromID = Integer.parseInt(args[1]);
		  		    		// ID in question INFINITY
		  		    		int idINF = Integer.parseInt(args[2].trim());
		  		    		byte[] sendData = new byte[56];
			  		      	InetAddress IPAddress = null;
			  		  		try {
			  		  			IPAddress = InetAddress.getLocalHost();
			  		  		} catch (UnknownHostException e1) {
			  		  			// TODO Auto-generated catch block
			  		  			e1.printStackTrace();
			  		  		} 
  		  	        		sendData = new byte[56];
  		  	        		int totalCost = costs[idINF-1]; 
  		  	        		String data = "! "+id+" "+idINF+" "+totalCost;
  		  					sendData = data.getBytes();
  		  					int sendToPort = Integer.parseInt(servers[fromID-1][2]);
  		  					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
  		  					try {
  		  						clientSocket.send(sendPacket);
  		  					} catch (IOException e) {
  		  						println("update ERROR: Couldn't send packet to id: "+fromID+", port: "+ sendToPort);
  		  						e.printStackTrace();
  		  					}
  		  					data = null;
  		  					sendData = null;
		  		    	}
		  		    	
		  		    	if(args[0].equals("!") && args.length == 4){ 
		  		    		int fromID = Integer.parseInt(args[1]);
		  		    		// ID in question INFINITY
		  		    		int idINF = Integer.parseInt(args[2]);
		  		    		int cost = Integer.parseInt(args[3].trim());
		  		    		costs[idINF-1] = (cost+costs[fromID-1]);
		  		    	}
		  		    	
		  		    	
//		  		    	String data = "UPDATE-INTERVAL "+id+" "+updateInterval;
//						for(int i = 0; i < numOfServers; i++){
//							data += " "+servers[i][0]+" "+costs[i];
//						}
						
//		  		    	if(args[0].equals("UPDATE-INTERVAL") && args.length == 11){ // Receiving update packet
//		  		    		println("receive:");
//		  		    		println(sentence);
//		  		    		int fromID = Integer.parseInt(args[1]);
//		  		    		int interval = Integer.parseInt(args[2]);
//		  		    		timeIntervals[fromID-1] 	= interval; // set time interval of server of that id
//		  		    		receivedPackets[fromID-1] 	+= 1;		// increase packets received by one for that id
//		  		    		secondsMissed[fromID-1]		= 0;		// reset miss timer
//		  		    		for(int i = 0; i < numOfServers; i++){
//		  		    			
//		  		    			if (id!=(i+1) && initCosts[i] != inf){
//		  		    				if(args[4+(i*2)].equals("inf") || args[4+(i*2)].trim() == "inf" || args[4+(i*2)].trim().equals("inf")){
//			  		    				if(costs[i] > inf){
//			  		    					costs[i] = inf;
//			  		    				} 
//		  		    					
//			  		    			} else {
//			  		    				if(costs[i] > Integer.parseInt(args[4+(i*2)].trim())){
//			  		    					costs[i] = Integer.parseInt(args[4+(i*2)].trim());
//			  		    				}
//			  		    			}
//		  		    			}
//							}
//		  		    		costs[id-1] = 0;
//		  		    		costs[fromID-1]=initCosts[fromID-1];
//
//		  		    	}
		  		    	
		  		    	if(args[0].equals("UPDATE")){ // Receiving update link command
		  		    	// if the first word in the packet is 'UPDATE'
		  		    		// 3rd is the id of this id
		  		    		int toID = Integer.parseInt(args[2]);
		  		    		if(id == toID){
		  		    			// 2nd arg is id from sending server
			  		    		int fromID = Integer.parseInt(args[1]);
			  		    		
			  		    		// 4th is cost of link
			  		    		int cost;
			  		    		if(args[3].trim().equals("inf")){
			  		    			cost = inf;
			  		    		} else {
			  		    			cost = Integer.parseInt(args[3].trim());
			  		    		}
			  		    		
			  		    		servers[fromID-1][3] 		= "1"; 		// server is online
			  		    		costs[fromID-1] 			= cost;		//neighbor cost
			  		    		initCosts[fromID-1] 		= cost;	
			  		    		receivedPackets[fromID-1] 	+= 1;		// increase packets received by one for that id
			  		    		secondsMissed[fromID-1]		= 0;		// reset miss timer
			  		    		println("RECEIVED A MESSAGE FROM SERVER "+fromID);
		  		    		}
		  		    	}
		  		    	
		  		    	if(args[0].equals("DISABLE")){ // Receiving disable command
		  		    		clientSocket.close();
		  		    		serverSocket.close();
		  		    	}
		  		    	
		  	            receiveData = null;
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
				println("server ERROR: Invalid server command");
				break;
			} else {
				openTopo(args[2]);
				this.updateInterval = Integer.parseInt(args[4]);
				openServerSocket(this.id, this.port);
				routingUpdate(this.updateInterval);
				checkDisconnect();
			}
			break;
		case "update":
			if(args.length != 4){
				println("update ERROR: Invalid update command");
				break;
			} else {
				int id1 = Integer.parseInt(args[1]);
				int id2 = Integer.parseInt(args[2]);
				String cost = args[3];
				update(id1, id2, cost);
			}
			break;	
		case "step":
			step();
			break;
		case "packets":
	    	packets();
	    	break;
	    case "display":
	    	display();
	    	break;
	    case "disable":
	    	if(args.length != 2){
				println("disable ERROR: invalid disable command");
				break;
			} else {
				boolean isNeighbor = false;
				int id = Integer.parseInt(args[1]);
				for(int i = 0; i < neighbors.length; i++){
					if (neighbors[i] == id){
						isNeighbor = true;
						sendDisable(neighbors[i]);
						break;
					}
				}
				if(isNeighbor == false){
					println("disable ERROR: id is not a neighbor");
				}
			}
	    	break;
	    case "crash":
	    	  clientSocket.close();
	    	  serverSocket.close();
	    	  System.exit(0);
	    	  break;
	    case "close":
	    	  clientSocket.close();
	    	  serverSocket.close();
	    	  System.exit(0);
	    	  break;  
    	  
	    }
	}
	
	private void sendDisable(int toID){
		InetAddress IPAddress = null;
		try {
			IPAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(int i = 0; i < neighbors.length; i++){
			if(neighbors[i] == toID){
				byte[] sendData = new byte[56];
				String data = "DISABLE " ;
				
				sendData = data.getBytes();
				
				int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
				
				try {
					clientSocket.send(sendPacket);
					println("disable "+toID+" SUCCESS");
				} catch (IOException e) {
					println("disable "+toID+" ERROR: Couldn't disable ID "+toID);
					e.printStackTrace();
				}
			}
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
		  } catch (Exception e){
		    System.err.format("Exception occurred trying to read '%s'.", filename);
		    e.printStackTrace();
		  } 
		  
		  // GET number of servers from topology
		  String nosarr[] = records.get(0).split(" ");
		  this.numOfServers = Integer.parseInt(nosarr[0]);
		  // GET number of neighbors from topology
		  String nonarr[] = records.get(1).split(" ");
		  this.numOfNeighbors = Integer.parseInt(nonarr[0]);
		  // GET ID from topology
		  String idarr[] = records.get(6).split(" ");
		  this.id = Integer.parseInt(idarr[0]);
		  // GET Port from topology
		  for(int i = 2; i < numOfServers+2; i++){
			  String arr[] = records.get(i).split(" ");
			  // 	--id-- 				 --IP destination--				--port--
			  servers[i-2][0]=arr[0];  servers[i-2][1]=arr[1];  servers[i-2][2] =  arr[2];
			  if(this.id == Integer.parseInt(arr[0])){
				  this.port = Integer.parseInt(arr[2]);
			  }
		  }
		  
		  // Set initial costs to infinity
		  for(int i = 0; i < numOfServers; i++){
			  if(i+1 == id){
				  initCosts[i] = 0;
			  } else {
				  costs[i] = inf;
			  }
		  }
		  //Get neighbors IDs and costs from topology
		  neighbors = new int[numOfNeighbors];
		  for(int i = 2+numOfServers; i < 2+numOfServers+numOfNeighbors; i++){
			  String arr[] = records.get(i).split(" ");
			  neighbors[i-(2+numOfServers)]=Integer.parseInt(arr[1]); // set neighbor id
			  initCosts[Integer.parseInt(arr[1])-1] = Integer.parseInt(arr[2]); // set neighbor costs
			  costs[Integer.parseInt(arr[1])-1] = Integer.parseInt(arr[2]);
		  }
		  
	}
	

	// Routing Update on time interval
	private void routingUpdate(int seconds){
		Runnable broadcast = new Runnable() {
		    public void run() {
		    	byte[] sendData = new byte[56];
		    	InetAddress IPAddress = null;
				try {
					IPAddress = InetAddress.getLocalHost();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
//				String data = "UPDATE-INTERVAL "+id+" "+updateInterval;
//				for(int i = 0; i < numOfServers; i++){
//					if(initCosts[i] == inf){
//						data += " "+servers[i][0]+" inf";
//					} else {
//						data += " "+servers[i][0]+" "+initCosts[i];
//					}
//				}
//				println(data);
				
		        for(int i = 0; i < neighbors.length; i++){
		        	sendData = new byte[56];
					String data = "UPDATE-INTERVAL "+id+" "+initCosts[neighbors[i]-1]+" "+updateInterval ;

					sendData = data.getBytes();
					int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
					
					
					try {
						clientSocket.send(sendPacket);
					} catch (IOException e) {
						println("update ERROR: Couldn't send packet to id: " + servers[neighbors[i]-1][0] + ", port: "+ sendToPort);
						e.printStackTrace();
					}
					
					data = null;					
		        }
		        sendData = null;
		        askInf();
		    }
		    
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(broadcast, 0, seconds, TimeUnit.SECONDS);
	}
	
	private void askInf(){
		byte[] sendData = new byte[56];
    	InetAddress IPAddress = null;
		try {
			IPAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        for(int i = 0; i < neighbors.length; i++){
        	for(int j = 0; j < numOfServers; j++){
	        	sendData = new byte[56];
	        	if(costs[j] == inf){
	        		String data = "? "+id+" "+servers[j][0];
	        		
					sendData = data.getBytes();
					int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
					
					
					try {
						clientSocket.send(sendPacket);
					} catch (IOException e) {
						println("update ERROR: Couldn't send packet to id: " + servers[neighbors[i]-1][0] + ", port: "+ sendToPort);
						e.printStackTrace();
					}
					
					data = null;
					sendData = null;
	        	}
				
        	}

        }
	}
	
	private void checkDisconnect(){
		for (int i=0; i < numOfServers; i++) {
    		secondsMissed[i]=0;
    	}
		Runnable checkDisconnects = new Runnable() {
		    public void run() {
		    	for (int i=0; i < numOfServers; i++) {
		    		secondsMissed[i] += 1;
		    		if (secondsMissed[i] > timeIntervals[i]*3 && (i+1) != id) {
			    		costs[i] = inf;
			    	}
		    	}
//		    	display();
		    }
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(checkDisconnects, 0, 1, TimeUnit.SECONDS);
	}
	
	// UPDATE LINK COST COMMAND
	private void update(int fromID, int toID, String cost){
		boolean isNeighbor = false;
		
        for(int i = 0; i < neighbors.length; i++){
        	if (neighbors[i] == toID) {
        		isNeighbor = true;
        		if (cost.equals("inf")){
        			costs[toID-1] = inf;
        		} else {
        			costs[toID-1] = Integer.parseInt(cost);
        		}
        		
        		byte[] sendData = new byte[56];
            	InetAddress IPAddress = null;
        		try {
        			IPAddress = InetAddress.getLocalHost();
        		} catch (UnknownHostException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        		sendData = new byte[56];
    			String data = "UPDATE "+fromID+" "+toID+" "+cost;
    			
    			sendData = data.getBytes();
    			
    			int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
    			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
    			
    			try {
    				clientSocket.send(sendPacket);
    			} catch (IOException e) {
    				println("update ERROR: Couldn't send packet to id: " + servers[neighbors[i]-1][0] + ", port: "+ sendToPort);
    				e.printStackTrace();
    			}
        	}
        }
        if(!isNeighbor){
        	println("update ERROR: "+toID+" is not a neighbor");
        }
	}
	
	
	
	// STEP COMMAND
	private void step(){
		byte[] sendData = new byte[56];
    	InetAddress IPAddress = null;
		try {
			IPAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
//		String data = "UPDATE-INTERVAL "+id+" "+updateInterval;
//		for(int i = 0; i < numOfServers; i++){
//			if(costs[i] == inf){
//				data += " "+servers[i][0]+" inf";
//			} else {
//				data += " "+servers[i][0]+" "+costs[i];
//			}
//		}
		
        for(int i = 0; i < neighbors.length; i++){
        	sendData = new byte[56];
//			String data = "UPDATE-INTERVAL "+id+" "+costs[neighbors[i]-1]+" "+updateInterval ;
			String data = "UPDATE-INTERVAL "+id+" "+initCosts[neighbors[i]-1]+" "+updateInterval ;
			sendData = data.getBytes();
			
			int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
			
			try {
				clientSocket.send(sendPacket);
			} catch (IOException e) {
				println("step ERROR: Couldn't send packet to id: " + servers[neighbors[i]-1][0] + ", port: "+ sendToPort);
				e.printStackTrace();
			}
        }
        askInf();
	}
	
	private void packets(){
		int numOfPackets = 0;
		for(int i = 0; i < numOfServers; i++){
			numOfPackets += receivedPackets[i];
			receivedPackets[i] = 0;
		}
		println("packets received: "+numOfPackets);
		println("packets SUCCESS");
	}
	
	
	public void display(){
		  println("====Routing Table====");
		  println("Dest | To | Cost |");
		  for(int i = 0; i < numOfServers; i++){
			  if(costs[i] == inf){
				  println(id+"    | "+servers[i][0]+"  | inf    |");
			  } else {
				  println(id+"    | "+servers[i][0]+"  | "+costs[i]+"      |");
			  }
		  }

		  println("=====================");
	}
	
	public void displayTest(){
		  println("====Routing Table====");
		  println("To | Cost | Time | PRcv | PMis | Smis");
		  for(int i = 0; i < numOfServers; i++){
			  if(costs[i] == inf){
				  println(servers[i][0]+"  | inf    | "+timeIntervals[i]+"    | "+receivedPackets[i]+"    | "+missedPackets[i]+"    | "+secondsMissed[i]);
			  } else {
				  println(servers[i][0]+"  | "+costs[i]+"    | "+timeIntervals[i]+"    | "+receivedPackets[i]+"    | "+missedPackets[i]+"    | "+secondsMissed[i]);
			  }
		  }

		  println("=====================");
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
	public String getIPAddress(){
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			println("ERROR: Couldn't get Local Host from server id "+this.id);
			e.printStackTrace();
		}
		return null;
	}
	
	public int getServerPort() {
		return this.serverSocket.getLocalPort();
	}

//	byte[] numOfUpdates = new byte[2];
//	byte[] serverPort = new byte[2];
//	byte[] serverIP = new byte[4];
//	
//	byte[] serverIP1 = new byte[4];
//	byte[] serverPort1 = new byte[2];
//	byte[] empty1 = new byte[2];
//	byte[] serverID1 = new byte[2];
//	byte[] serverCost1 = new byte[2];
//	
//	byte[] serverIP2 = new byte[4];
//	byte[] serverPort2 = new byte[2];
//	byte[] empty2 = new byte[2];
//	byte[] serverID2 = new byte[2];
//	byte[] serverCost2 = new byte[2];
//	
//	byte[] serverIP3 = new byte[4];
//	byte[] serverPort3 = new byte[2];
//	byte[] empty3 = new byte[2];
//	byte[] serverID3 = new byte[2];
//	byte[] serverCost3 = new byte[2];
//	
//	byte[] serverIP4 = new byte[4];
//	byte[] serverPort4 = new byte[2];
//	byte[] empty4 = new byte[2];
//	byte[] serverID4 = new byte[2];
//	byte[] serverCost4 = new byte[2];
	
}