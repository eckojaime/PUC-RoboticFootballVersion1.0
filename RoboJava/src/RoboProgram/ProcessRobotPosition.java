/*
//------------------------------------------------------------//

**************    ****     ****     **************		
**************    ****     ****     **************
****      ****    ****     ****     ****
****      ****    ****     ****     ****
**************    ****     ****     ****
**************    ****     ****     ****
****              ****     ****     ****
****              ****     ****     ****
****              *************     **************
****              *************     **************		

-------------------------------------------------------------//
Programmer				Date				Modification Reason

Jaime Alvarez			01-31-14			Initial Implementation

Jaime Alvarez        04-30-14       Completed Version 1.0

//-----------------------------------------------------------//

Description:
This class wil be responsible of getting the 
current position data from the corresponding
robot and updating it to the RobotCoordinates
object so the Quarterback can see if it's good
to throw.
*/

package RoboProgram;

//For sockets
import java.net.*;

//For updating position label
import javax.swing.JLabel;

//For controller inputs
import net.java.games.input.Controller;


class ProcessRobotPosition extends Thread
{
   //The player's controller, IP and socket from where it's listening
   private String player = "";
   private Controller controller;
   private RobotCoordinates robot;
   private String theIP = "";
   private int thePort;
   private DatagramSocket serverSocket;
   private byte[] receiveData = new byte[512];
   
   //The Label to update position
   private JLabel positionLabel;
   
   //Constructor
   //The reason we are also passing a Controller reference is to determine if the controller
   //is still connected, if not terminate the thread. The IP and port is used to determine
   //where it's listening to get the data from the robot
   public ProcessRobotPosition(String player, Controller controller, String IP, int portNumber, RobotCoordinates robot, JLabel positionLabel)
   {
      this.player = player;
      this.controller = controller;
      this.robot = robot;
      theIP = IP;
      thePort = portNumber;
      this.positionLabel = positionLabel;
      
      //Setup the port for receiving
      try
      {
         serverSocket = new DatagramSocket(thePort);
         
         //Set a timeout for 1 millisecond
         serverSocket.setSoTimeout(1);
         System.out.println("Socket created!");
         System.out.println("IP: " + theIP + "\nPort: " +thePort);
      }
      catch(Exception e)
      {
         System.out.println("Server Socket Error: " + e.getMessage());
      }
   }
   
   //Override the run() method
   public void run()
   {
      System.out.println(player + " Thread receiving started."); 
      while(!this.isInterrupted())
      {
         //Check if the corresponding controller is still connected, if not then stop the thread
         if(!controller.poll())
         {
            System.out.println(player + " is Disconnected, will stop getting data.");
            break;
         }
         
         retrieveData();
      }
      try
      {
    	  serverSocket.close();
      }
      catch(Exception e)
      {
    	  System.out.println("Didn't close server socket, switch port #");
      }
      System.out.println(player + " Thread successfully stopped receiving. Done");
   }//run()

   //Method to retrieve any data from the robots
   //Expecting position data
   private void retrieveData()
   {
      try
      {
         DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
         serverSocket.receive(receivePacket);
         String msg = new String(receivePacket.getData());
         char[] posData = msg.toCharArray();
         String x = "";
         String y = "";
         String PHI = "";
         
         //Get the right parts of the data
         //The format received looks like this
         //3.3333X4.4444X2.2222X
         int Xcounter = 0;
         int i = 0;
         boolean keepGoing = true;
         while(keepGoing)
         {
            if(posData[i] != 'X')
            {
               if(Xcounter == 0)
                  x += posData[i];
               else if(Xcounter == 1)
                  y += posData[i];
               else if(Xcounter == 2)
                  PHI += posData[i];
               else
                  keepGoing = false;
            }
            else
            {
               Xcounter++;
            }
            i++;              
         }
         
         //Parse the strings into floats
         //Set the new position of the robot
         robot.setX(Float.parseFloat(x));
         robot.setY(Float.parseFloat(y));
         robot.setPHI(Float.parseFloat(PHI));
         
         updatePositionLabel();
         
         //Used for debugging
         /*System.out.println(msg);
         System.out.println(x);
         System.out.println(y);
         System.out.println(PHI);*/                
      }
      catch(SocketTimeoutException ex)
      {
    	  //Socket timed out, this out of the method
      } 
      catch(PortUnreachableException ex2)
      {
    	  System.out.println("Port Unreachable!");
      }      
      catch(Exception e)
      {
         System.out.println("Error getting Data " + e.getMessage());
         e.printStackTrace();
      }
   
   }//retrieveData()
   
   private void updatePositionLabel()
   {
      if(player.equals("Player 1"))
      {
         positionLabel.setText("Quar2pac: (" + robot.getX() + ", " + robot.getY() +", " + Math.toDegrees(robot.getPHI())+ ")");
      }
      else if(player.equals("Player 2"))
      {
         positionLabel.setText("Receiver: (" + robot.getX() + ", " + robot.getY() +", " + Math.toDegrees(robot.getPHI())+ ")");
      }      
   }
   
}//ProcessRobotPosition