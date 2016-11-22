package main;

import java.io.*;
import java.net.*;

import static main.SysoutWrapper.*;

class UDPServer {
	
	private static DatagramSocket serverSocket;
	private static int id;
	private static int numOfServers = 4;
	private static int[] neighborIds;
	
		
	public static int getServerPort() {
		return serverSocket.getPort();
	}

	public static void main(String args[]) throws Exception {
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        
       
        //console-> java main.UDPServer 2
        //create server id:2
        
        switch(Integer.parseInt(args[0])) {
        	case 1: serverSocket = new DatagramSocket(9111);
        			id = 1;
        			neighborIds = new int[3];
        			neighborIds[0] = 2;
        			neighborIds[1] = 3;
        			neighborIds[2] = 4;
        			break;
        	case 2: serverSocket = new DatagramSocket(9222);
        			id = 2;
        			neighborIds = new int[2];
        			neighborIds[0] = 1;
        			neighborIds[1] = 3;
        			
					break;
        	case 3: serverSocket = new DatagramSocket(4333);
        			id = 3;
        			neighborIds = new int[3];
        			neighborIds[0] = 1;
        			neighborIds[1] = 2;
        			neighborIds[2] = 4;
					break;
        	case 4: serverSocket = new DatagramSocket(4444);
        			id = 4;
        			neighborIds = new int[2];
        			neighborIds[0] = 1;
        			neighborIds[1] = 3;
					break;
        }
        
        
        
        
        while(true){
              DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
              serverSocket.receive(receivePacket);
              String sentence = new String( receivePacket.getData());
              println("RECEIVED: " + sentence);
              InetAddress IPAddress = receivePacket.getAddress();
              int port = receivePacket.getPort();
              String capitalizedSentence = sentence.toUpperCase();
              sendData = capitalizedSentence.getBytes();
              DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
              serverSocket.send(sendPacket);
        }
	}
}