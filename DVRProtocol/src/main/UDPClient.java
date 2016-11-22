package main;

import java.io.*;
import java.net.*;
import static main.SysoutWrapper.*;

class UDPClient {
   public static void main(String args[]) throws Exception {
      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
      DatagramSocket clientSocket = new DatagramSocket();
      InetAddress IPAddress = InetAddress.getLocalHost();
      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
      String sentence = inFromUser.readLine();
      sendData = sentence.getBytes();
      int serverPort = Integer.parseInt(args[0]);
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
      clientSocket.send(sendPacket);
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      String modifiedSentence = new String(receivePacket.getData());
      println("FROM SERVER:" + modifiedSentence);
      clientSocket.close();
   }
}
