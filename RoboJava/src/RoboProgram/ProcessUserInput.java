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
This the class that will mainly concentrate on
getting, interpreting, and sending Xbox 360 
controller input to a robot.
*/

package RoboProgram;


import java.io.*;
import static java.lang.Math.*;
import java.net.*;

//For controller inputs
import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;


class ProcessUserInput extends Thread
{      
   //Variables used
   //A description is available in the constructor description
   private String player = "";
   private String theIP = "";
   private int thePort;
   private Controller controller;
   private RobotCoordinates QBposition;
   private RobotCoordinates RCposition;
   
   private DatagramSocket robotSocket;
   
   //This keeps track of the current Axis position from the controller
   private float xAxisPercentage = 0.0f;
   private float yAxisPercentage = 0.0f;
   
   //This keeps track if the robot is turning for one analog stick control
   private boolean isTurning = false;
   
   //Boolean for button pressed
   private boolean bPressed = false;
   
   //This will keep track of the values for each stick, to send it all at once instead of two packets for using tank movement control
   private boolean gotLeftStickValue = false;
   private boolean gotRightStickValue = false;
   private String leftStickValue = "";
   private String rightStickValue = "";
   
   //Add a "gear shift" for the robot, to make sure robot can go straight at a moderate speed for presentation
   private boolean gearedDown = true;
   
   //If true, have constant speeds in certain intervals for sensitivity issues due to human error
   private boolean callOfDutyStyle = true;
   private int intervalDeviation = 25;
   
   //Constants and variables to use for determining required for successful passing to the receiver
   private static final double MAX_DISTANCE = 20.0; //20ft
   private static final double MIN_DISTANCE = .5; //.5ft
   private double xDistance = 0;
   private double yDistance = 0;
   private double hypotenuseDistance = 0;
   private double maxDegreesDeviation = 10; //10 degrees
   private double theta = 0;//Degrees to turn to face Receiver
   
   //Constructor
   //Pass in the player's controller reference, the IP and Port number to send the input data to the right Robot,
   //and keep track of the position of the Quarterback and Receiver.
   public ProcessUserInput(String player, Controller controller, String IP, int portNumber, RobotCoordinates QBposition, RobotCoordinates RCposition)
   {
      this.player = player;
      this.controller = controller;
      theIP = IP;
      thePort = portNumber;
      this.QBposition = QBposition;
      this.RCposition = RCposition;
      
      try
      {
    	  robotSocket = new DatagramSocket();
    	  System.out.println("IP: " + theIP + "\nPort: " +thePort);
      }
      catch(Exception e)
      {
    	  System.out.println(player + "Robo Socket Error: " + e.getMessage());
      }
   }
   
   //Override the run() method from the super class Thread
   public void run()
   {
      while(!this.isInterrupted())
      {   
         //Poll controller for current data, and break while loop if controller is disconnected.
         if(!controller.poll())
         {
             System.out.println(player + " is Disconnected!");
             
             //Tell the corresponding BeagleBone(robot) to stop
             tellOtherSide("Done");             
             break;                            
         }
         
         //Check if we're good to throw
         if(goodToThrow())
         {
            //If good to throw, then tell the robot to do a pass
            tellOtherSide("P" + hypotenuseDistance);
            //Stop sending anything else for 4 seconds
            try
            {
               Thread.sleep(4000);
            }
            catch(InterruptedException e)
            {
          	   this.interrupt();
            }
            catch(Exception ex)
            {
               System.out.println(ex.getMessage());
            }
         }      
                 
         //Go trough all components of the controller.
         Component[] components = controller.getComponents();
         for(int i=0; i < components.length; i++)
         {
             Component component = components[i];
             
             //Identify the component
             Identifier componentIdentifier = component.getIdentifier();
             
             //Buttons             
             //if(component.getName().contains("Button")){ // If the language is not english, this won't work.
             if(componentIdentifier.getName().matches("^[0-9]*$"))// If the component identifier name contains only numbers, then this is a button.
             { 
                 //Is button pressed?
                 boolean isItPressed = true;
                 if(component.getPollData() == 0.0f)
                     isItPressed = false;
                 
                 //Send the button pressed to the robot
                 if(isItPressed)
                 {
                    String buttonIndex = component.getIdentifier().toString();                    
                    sendButtonPressed(buttonIndex);
                  }  
                                     
                 // We know that this component was button so we can skip to next component.
                 continue;
             }
             
             //Hat switch
             //For testing purposes ignore these for now
             if(componentIdentifier == Component.Identifier.Axis.POV){
                 float hatSwitchPosition = component.getPollData();
                                     
                 //We know that this component was a hat switch so we can skip to next component.
                 continue;
             }
             
             //Axes
             //Movement!
             if(component.isAnalog())
             {
                 //Get any movement the analog stick does or doesn't
                 float axisValueInPercentage = component.getPollData();
                                                      
                 // X axis
                 //Ignore for tank movement control
                 /*if(componentIdentifier == Component.Identifier.Axis.X){
                     xAxisPercentage = axisValueInPercentage;
                     //sendMovement(-1*xAxisPercentage, 'X');
                     continue; // Go to next component.
                 }*/
                 // Y axis
                 if(componentIdentifier == Component.Identifier.Axis.Y)
                 {
                     //Ignore these for tank control movement
                     //yAxisPercentage = axisValueInPercentage;
                     //sendMovement(yAxisPercentage, 'Y');
                	   sendTankMovement(axisValueInPercentage, 'L', callOfDutyStyle);
                 }
                 else
                 {
                	   sendTankMovement(axisValueInPercentage, 'R', callOfDutyStyle);  	 
                 }
                 
                 //Send the packets every 30ms to allow sufficient time for the robots to interpret
                 try
                 {
                     Thread.sleep(30);
                 }
                 catch(InterruptedException e)
                 {
                	   this.interrupt();
                 }
                 catch(Exception ex)
                 {
                     System.out.println(ex.getMessage());
                 }                 
             }                         
         }
      }//while()
      
      tellOtherSide("Done");
      try
      {
    	  robotSocket.close();
      }
      catch(Exception ex)
      {
    	  System.out.println(player + " socket didn't close. Choose new port #");
      }
      System.out.println(player + " Thread has stopped sending successfully. Done");
      
   }//run()

   //Tell the other side a message i.e. the BeagleBone
   private void tellOtherSide(String message)
   {
      try
      {
         DatagramPacket msg = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(theIP), thePort);         
         robotSocket.send(msg);
         
         //If button pressed give the robot 7.5 seconds to allow throw
         if(bPressed)
         {
            Thread.sleep(7500);
            bPressed = false;
         }
      }
      catch(Exception e)
      {
         System.out.println(e.getMessage());
      }
      System.out.println(message);
   }//tellOtherSide()
   
   private void sendButtonPressed(String btn)
   {
      String btnPressed = btn;
      bPressed = false;           
      
      //Xbox 360 A button 
      if(btn.equals("0"))
      {
         btnPressed = "BA";
         bPressed = true;
      }
      //Xbox 360 B button 
      else if(btn.equals("1"))
      {
         bPressed = true;
         btnPressed = "BB";
      }
      //Xbox 360 X button 
      else if(btn.equals("2"))
      {
         bPressed = true;
         btnPressed = "BX";
      }
      //Xbox 360 Y button 
      else if(btn.equals("3"))
      {
         bPressed = true;
         btnPressed = "BY";
      }
      //Xbox 360 Left Bumper button
      else if(btn.equals("4"))
      {
         //Geared down for slower speeds
         gearedDown = true;
      }
      //Xbox 360 Right Bumper button
      else if(btn.equals("5"))
      {         
         //Allow max speeds
         gearedDown = false;
      }
      //Xbox 360 Start button 
      else if(btn.equals("7"))
      {
         btnPressed = "BS";
      }
      //Xbox 360 Back button 
      else if(btn.equals("6"))
      {
         btnPressed = "BZ";
      }
      
      //Just send the buttons the robot needs to interpret
      if(btnPressed.length() == 2)
      {      
         tellOtherSide(btnPressed);
      }
      System.out.println(btnPressed);
   }
   
   private void sendTankMovement(float percentage, char whichThumbstick, boolean callOfDutyStyle)
   {
	   if(gotLeftStickValue && gotRightStickValue)
	   {
		   tellOtherSide(leftStickValue + "X" + rightStickValue);
		   System.out.println(leftStickValue + "X" + rightStickValue);
		   
         //Reset booleans
		   gotLeftStickValue = false;
		   gotRightStickValue = false;
	   }
	   else
	   {         
		   int movement = (int)(-255*percentage);
		   
		   //If value is less than 60 and greater than -60 interpret it as zero movement
		   if(movement < 60 && movement > -60)
		   {
		       movement = 0;
		   }
		   
         if(gearedDown)
         {
            movement = (int)(movement * 85.0/255.0);//Debug speeds
         }
         else
         {
            if(callOfDutyStyle)
            {
               if(movement != 0)
               {
                  //Make the movement positive to change to use one set of logic; less code
                  boolean isNegative = false;
                  if(movement < 0)
                  {
                     isNegative = true;
                     movement *= -1;
                  }
                  
                  //Work with the values >= 60 & < 200
                  if(movement < 200)
                  {
                     if(movement >= 60 && movement < 100)
                     {
                        movement = 100;
                     }
                     else if(movement >= 100 && movement < 150)
                     {
                        movement = 150;
                     }
                     else if(movement >= 150 && movement < 200)
                     {
                        movement = 200;
                     }                        
                     /*else if(movement >= 100 && movement < 125)
                     {
                        movement = 125;
                     }
                     else if(movement >= 125 && movement < 150)
                     {
                        movement = 150;
                     }
                     else if(movement >= 150 && movement < 175)
                     {
                        movement = 175;
                     }
                     else if(movement >= 175 && movement < 200)
                     {
                        movement = 200;
                     }
                     else if(movement >= 200 && movement < 225)
                     {
                        movement = 225;
                     }*/
                  }
                  //Just max it out
                  else
                  {
                     movement = 255;
                  }                     
                  
                  //Was a negative change it back to negative
                  //Keep same magnitude though
                  if(isNegative)
                  {
                     movement *= -1;
                  }
               }
            }
         }
         
         if(!gotLeftStickValue && whichThumbstick == 'L')
         {
			   leftStickValue = "S" + Integer.toString(movement);
			   gotLeftStickValue = true;
         }
         if(!gotRightStickValue && whichThumbstick == 'R')
         {
            rightStickValue = "S" + Integer.toString(movement);
			   gotRightStickValue = true;
         }		   
	   }
   }//sendTankMovement()
   
   //Robot Movement! One stick
   private void sendMovement(float percentage, char axis)
   {
      //Inverted apparently
      int movement = (int)(-255*percentage);
      //If value is less than 50 and greater than -50 interpret it as zero movement
      if(movement < 60 && movement > -60)
      {
          movement = 0;
      }      
      //Check if it's a X or Y axis input using the axis parameter
      String commandType = "S";
      if(axis == 'X')
    	  commandType = "T";
      
      boolean canISend = true;
      
      //If the robot is turning, ignore the move forward and backward commands completely
      if(isTurning && axis == 'Y')
      {
    	  canISend = false;
      }
      //If the robot is turning, send the new turning speed
      else if(isTurning && axis == 'X' && (movement > 49 || movement < -49))
      {
    	  //canISend already equals true and keep the isTurning boolean as it is (true)
      }
      //If the robot is turning, but the turning command is zero then stop turning
      else if(isTurning && axis == 'X' && (movement < 49 && movement > -49))
      {
    	  isTurning = false;
    	  //canISend already true, send T0    	  
      }
      //If we want to start turning again
      else if(!isTurning && axis == 'X' && (movement > 49 || movement < -49))
      {
    	  isTurning = true;
    	  //canISend is true leave it as it is
      }
      //If NOT turning at all then don't send the turning zero value
      else if(!isTurning && axis == 'X' && (movement < 49 && movement > -49))
      {
    	  canISend = false;    	  
      }
      //Else just go straight forward or backward
      else
      {
    	  //Send full forward or full backward command, so leave canISend (true)
      }      
      
      if(canISend)
      {
    	  String data = commandType + Integer.toString(movement);
          tellOtherSide(data);
      }      
        
   }//sendMovement()
   
   //The method that will determine if it's good to throw
   //Note: since multiple math functions were used, I imported the Math library for ease on readability
   public boolean goodToThrow()
   {
	   //Calculate the X and Y distances between the two robots
	   xDistance = RCposition.getX() - QBposition.getX();
	   yDistance = RCposition.getY() - QBposition.getY();
	   
	   //Calculate the distance between the two robots i.e. hypotenuse
	   hypotenuseDistance = sqrt(pow(xDistance, 2.0) + pow(yDistance, 2.0));
	   
	   //The amount of degrees we want the Quarterback to turn to face the Receiver
	   theta = toDegrees(acos(xDistance/hypotenuseDistance));
	   
	   //First check if the distance between them is appropriate
	   if(hypotenuseDistance >= MIN_DISTANCE && hypotenuseDistance <= MAX_DISTANCE)
	   {
		   //boolean to check if it's good to throw
		   boolean goodToThrow = false;
         
		   //It's better to look at pictures of the scenarios of how the robots are set up to understand this logic
		   //Make sure the robots are not aligned vertically
		   if(xDistance != 0.0)
		   {
			   //If the Receiver is more positive on the coordinate system with respect to the y-axis
			   if(yDistance > 0.0)
			   {
				   //Within reasonable range in degrees
				   if(toDegrees(QBposition.getPHI()) >= (theta - maxDegreesDeviation) && toDegrees(QBposition.getPHI()) <= (theta + maxDegreesDeviation))
				   {
					   goodToThrow = true;
				   }
			   }
			   //If the Quarterback is more positive on the coordinate system with respect to the y-axis
			   else if(yDistance < 0.0)
			   {
				   //Again reasonable range
				   theta = -1.0 * theta;
				   if(toDegrees(QBposition.getPHI()) >= (theta - maxDegreesDeviation) && toDegrees(QBposition.getPHI()) <= (theta + maxDegreesDeviation))
				   {
					  goodToThrow = true; 
				   }
			   }
			   //They horizontally match up then
			   else
			   {
				   if(xDistance > 0.0)
				   {
					   if(toDegrees(QBposition.getPHI()) >= -1*maxDegreesDeviation && toDegrees(QBposition.getPHI()) <= maxDegreesDeviation)
					   {
						   goodToThrow = true;
					   }				   
				   }
				   else if(xDistance < 0.0)
				   {
					   if(toDegrees(QBposition.getPHI()) >= (180.0 - maxDegreesDeviation) && toDegrees(QBposition.getPHI()) <= (180.0 + maxDegreesDeviation))
					   {
						   goodToThrow = true;
					   }
				   }
				   
				   //xDistance can't be zero because the outermost if statement checks the minimum throwing distance.
				   //So if yDistance is zero then xDistance must be greater than or equal to the minimum throwing distance.
			   }  
			   
		   }
		   //They're aligned vertically
		   else
		   {
			   //Still working on it
			   if(yDistance > 0.0)
			   {
				   if(toDegrees(QBposition.getPHI()) >= -1*maxDegreesDeviation && toDegrees(QBposition.getPHI()) <= maxDegreesDeviation)
				   {
					   goodToThrow = true;
				   }				   
			   }
			   else if(yDistance < 0.0)
			   {
				   if(toDegrees(QBposition.getPHI()) >= (180.0 - maxDegreesDeviation) && toDegrees(QBposition.getPHI()) <= (180.0 + maxDegreesDeviation))
				   {
					   goodToThrow = true;
				   }
			   }
		   }
		   
		   return goodToThrow;
	   }
	   else
	   {
		   return false;
	   }
	   
   }//goodToThrow()
}//ProcessUserInput