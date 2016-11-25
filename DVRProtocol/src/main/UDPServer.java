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
	  		    	  	//sendData = new byte[1024];
		  		    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		  		    	try {
		  					serverSocket.receive(receivePacket);
		  				} catch (IOException e) {
		  					println("ERROR: server socket receive failed for server id " + id);
		  					e.printStackTrace();
		  				}
		  		    	String sentence = new String( receivePacket.getData());
		  		    	String args[] = sentence.split(" ");
		  		    	if(args[0].equals("UPDATE") && args.length == 4){ // Receiving update packet
		  		    		// if the first word in the packet is 'UPDATE'
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
		  		    		secondsMissed[id-1]		= 0;
//		  		    		for(int i = 0; i < neighbors.length; i++){
//		  		    			if(neighbors[i] == id){
//
//		  		    			}
//		  		    		}	
		  		    	}
		  		    	
		  		    	
		  		    	if(args[0].equals("TEST")){ // Receiving test packet
		  		    		println("Test: " + sentence);
		  		    	}
		  		    	
//		  		    	println("Server>");
		  		    	
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
				checkDisconnect();
			}
			break;
	    
	    case "display":
	    	display();
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
		  }
		  
	}
	
	// Routing Update on time interval
	private void routingUpdate(int seconds){
		Runnable broadcast = new Runnable() {
		    public void run() {
		    	byte[] sendData = new byte[1024];
				byte[] receiveData = new byte[1024];
		    	InetAddress IPAddress = null;
				try {
					IPAddress = InetAddress.getLocalHost();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        for(int i = 0; i < neighbors.length; i++){
//		        			         	  --id-- 				           --IP destination--				        --port--
//		            println("send this to "+servers[neighbors[i]-1][0]+" "+servers[neighbors[i]-1][1]+" "+servers[neighbors[i]-1][2]);
		        	sendData = new byte[1024];
					receiveData = new byte[1024];
					String data = "UPDATE "+id+" "+initCosts[neighbors[i]-1]+" "+updateInterval ;

					sendData = data.getBytes();
//					int sendToPort = Integer.parseInt(servers[neighbors[i][0]-1][2]);
					int sendToPort = Integer.parseInt(servers[neighbors[i]-1][2]);
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, sendToPort);
					try {
						clientSocket.send(sendPacket);
					} catch (IOException e) {
						println("Couldn't send packet to id: " + servers[neighbors[i]-1][0] + ", port: "+ sendToPort);
						e.printStackTrace();
					}
		        }
		    }
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(broadcast, 0, seconds, TimeUnit.SECONDS);
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
		    	display();
		    }
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(checkDisconnects, 0, 1, TimeUnit.SECONDS);
	}
	
	public void display(){
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