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

The main server application.
This application will be in charge
of executing threads that will process
Xbox 360 Contorller input and position
of the robots.
*/

package RoboProgram;

//For controller inputs and stuff
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import java.util.*;
import static java.lang.Math.*;

//For GUI portion
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class TheServer implements ActionListener
{
   //Entry of program
   public static void main(String[] args)
   {
      new TheServer();
   }
   
   /*******************GUI stuff**********************/
   //The App
   public JFrame frame;
   
   //The App's window and main panel
   public JPanel theWindowPanel = new JPanel(new BorderLayout(30,30));
   public JPanel mainPanel = new JPanel(new GridLayout(0,2));
   
   //Labels that will display position of quarterback and receiver
   public JLabel posQuar;
   public JLabel posRec;
   
   //Label that will show the status of the program and who's currently connected
   public JLabel appState;
   public JLabel currentlyConnected;
   
   //ProgressBars to display the robots' battery lifes
   public JProgressBar batteryLifeQuar = new JProgressBar(0, 100);
   public JProgressBar batteryLifeRec = new JProgressBar(0, 100);
   
   //Buttons to start and stop plays
   public JButton btnStart = new JButton("Start Play");
   public JButton btnStop = new JButton("Stop Play");
   
   //MenuBar
   public JMenuBar menuBar = new JMenuBar();
   public JButton menuEditBtn = new JButton("Edit Properties");
   public JButton menuSubmitBtn = new JButton("Submit!");
   public JTextField menuTextField  = new JTextField();
      
   //MenuBar menu field reference for editing IPs and ports
   public JMenuItem qbIP;
   public JMenuItem rcIP;
   public JMenuItem qbPort;
   public JMenuItem rcPort;
   public JMenuItem servIP;
   public JMenuItem servPort1;
   public JMenuItem servPort2;
   
   /****************End of GUI stuff******************/
   
   
   //A list to hold the currently connected Xbox 360 controllers
   private ArrayList<Controller> foundControllers = new ArrayList<Controller>();
   
   //Boolean to determine if any controllers are found
   private boolean controllersAreFound;
         
   //Booleans to keep track which controllers are connected
   boolean playerOneIsConnected = false;
   boolean playerTwoIsConnected = false;
      
   //IPs and Ports used
   //For player one and player two respectively
   //These are for the robots
   String[] RobotIPs = {"192.168.1.107", "192.168.1.108"};
   int[] RobotPorts = {5555, 5555};
   
   //For this application
   //It'll retrieve data such as position of the robot
   String ServerIP = "192.168.1.147";
   int ServerPorts[] = {4444, 4445};
   
   //The references for the threads to run
   //Right now only have to worry about the quarterback and receiver
   //Quarterback references
   ProcessUserInput QBSend;
   ProcessRobotPosition QBReceive;
   
   //Receiver references
   ProcessUserInput RCSend;
   ProcessRobotPosition RCReceive;
   
   
   //Boolean to tell if the program is running or currently stopped
   private boolean isRunning = false;
   
   //The program doing its job
   public TheServer()
   {
      searchForControllers();
      
      //Start up the GUI
 	   setUpMenuBar();
      setUpApp(); 	           
   }
   
   //Method to search for any possible controllers currently connected
   private void searchForControllers() 
   {
       //Apparently ControllerEnvironment.getDefaultEnvironment().getControllers() gets more other devices than it should
       //However, through testing, the Xbox controllers are the first ones found so there indexes are 0 and 1
       Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

       for(int i = 0; i < controllers.length; i++)
       {
            Controller controller = controllers[i];
            
            if (controller.getType() == Controller.Type.GAMEPAD)
            {
                //Add new controller to the list of all controllers.
                foundControllers.add(controller);                
            }
       }
       
       controllersAreFound = !foundControllers.isEmpty();
       
       //Set booleans to true if corresponding players are connected
       if(controllersAreFound)
       {
         //Always the first player is connected
         playerOneIsConnected = true;
         System.out.println("Player one is connected!");
         
         //Check if the two controllers are connected         
         if(foundControllers.size() == 2)
         {
            //A second controller is connected then
            playerTwoIsConnected = true;
            System.out.println("Player two is connected!");
         } 
       }       
   }
   
   public void setUpApp()
   {
      //Instantiate the frame for the app
	   frame = new JFrame("RoboGUI");
	     
	   //Set up panels to display data
	   JPanel batteryLifePanel = new JPanel(new BorderLayout(5,5));
	   JPanel posRobotPanel = new JPanel(new BorderLayout(5,5));
	   JPanel buttonPanel = new JPanel();
	   JPanel stateAndConnected = new JPanel(new BorderLayout(5,5));
	     
	   //Setup battery lifes
	   batteryLifeQuar.setValue(100);
	   batteryLifeRec.setValue(100);
	   batteryLifeQuar.setStringPainted(true);
	   batteryLifeRec.setStringPainted(true);
	     
	   JPanel gridLifePanel = new JPanel(new GridLayout(5,1));
	   gridLifePanel.add(new JLabel("Battery Life:"));
	   gridLifePanel.add(new JLabel("Quar2Pac Battery Life:"));
	   gridLifePanel.add(batteryLifeQuar);
	   gridLifePanel.add(new JLabel("Receiver Battery Life:"));
	   gridLifePanel.add(batteryLifeRec);
	     
	   batteryLifePanel.add(gridLifePanel, BorderLayout.CENTER);
	     
	   //Setup position labels
	   JPanel gridPosPanel = new JPanel(new GridLayout(3,1));
	   gridPosPanel.add(new JLabel("Position:"));
	   gridPosPanel.add(posQuar = new JLabel("Quar2pac: (0, 0, 90)"));
	   gridPosPanel.add(posRec = new JLabel("Receiver: (15, 0, 90)"));
	   posRobotPanel.add(gridPosPanel, BorderLayout.EAST);
	     
	   //Setup buttons
	   btnStart.addActionListener(this);
	   btnStop.addActionListener(this);
	     
	   buttonPanel.add(btnStart);
	   buttonPanel.add(btnStop);
	     
	   //Setup state and currently connected labels
	   String initConnected = "";
	   if(controllersAreFound)
	   {    	 
	   	 if(playerOneIsConnected && playerTwoIsConnected)
	   		 initConnected = "Both players are currently connected!";
	   	 else if(playerOneIsConnected)
	   		 initConnected = "Player One is currently connected!";
	   	 else if(playerTwoIsConnected)
	   		 initConnected = "Player Two is currently connected!"; //Not sure if it'll happen, but just in case
	   }
	   else
	   	 initConnected = "No controllers are connected!";
	     
	   stateAndConnected.add(appState = new JLabel("App has successfully started up!"), BorderLayout.WEST);
	   stateAndConnected.add(currentlyConnected = new JLabel(initConnected), BorderLayout.EAST);
	     
	       
	   //Add battery life panel and position panel to the main panel
	   mainPanel.add(batteryLifePanel);
	   mainPanel.add(posRobotPanel);
	     
	   //Add a state and currently connected panel, the button panel, and the main panel to the window panel
	   theWindowPanel.add(stateAndConnected, BorderLayout.NORTH);
	   theWindowPanel.add(buttonPanel, BorderLayout.SOUTH);
	     
	   //Dummy labels to add cushion
	   theWindowPanel.add(new JLabel(""), BorderLayout.WEST);
	   theWindowPanel.add(new JLabel(""), BorderLayout.EAST);
	      
	   theWindowPanel.add(mainPanel, BorderLayout.CENTER);
	   frame.setContentPane(theWindowPanel);
	   frame.setSize(420,300);
	   frame.setLocationRelativeTo(null);
	   frame.setVisible(true);
	   frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      //Add menubar to the app
      frame.setJMenuBar(menuBar);
   }//setUpApp()
   
   public void setUpMenuBar()
   {
	   //Main menus
       JMenu options = new JMenu("Options");
       JMenu help = new JMenu("Help");
       
       //Sub menus
       JMenu robotInfo = new JMenu("Robot Info");
       JMenu serverInfo = new JMenu("Server Info");
       JMenu about = new JMenu("About");
       
       //Sub sub menus
       JMenu roboIPs = new JMenu("Robot IPs");
       JMenu roboPorts = new JMenu("Robot Ports");
       JMenu serverIP = new JMenu("Server IP");
       JMenu serverPorts = new JMenu("Server Ports");
       
       //Details
       qbIP = new JMenuItem("Quarterback: " + RobotIPs[0]);
       rcIP = new JMenuItem("Receiver: " + RobotIPs[1]);
       qbPort = new JMenuItem("Quarterback: " + RobotPorts[0]);
       rcPort = new JMenuItem("Receiver: " + RobotPorts[1]);
       servIP = new JMenuItem(ServerIP);
       servPort1 = new JMenuItem("Quarterback: " + ServerPorts[0]);
       servPort2 = new JMenuItem("Receiver: " + ServerPorts[1]);
       
       String aboutMsg = "RoboProgram(Robo GUI) Version 1.0 Date: 04/30/14\n";
       String quarMsg = "Quarterback Team:\nJaime Alvarez\nTyler Daw\nDavid Benirschke\n";
       String recMsg = "Receiver Team:\nMatthew Gyure\nMike Peschke\nPatrick Hughes\n";
       
       JMenuItem aboutDetails = new JMenuItem(aboutMsg + quarMsg + recMsg);//"Implemented in Spring 2014");
       
       //Add the details to the sub sub menus
       roboIPs.add(qbIP);
       roboIPs.add(rcIP);
       roboPorts.add(qbPort);
       roboPorts.add(rcPort);
       serverIP.add(servIP);
       serverPorts.add(servPort1);
       serverPorts.add(servPort2);
       
       //Add sub sub menus to sub menus
       //Inception ha
       robotInfo.add(roboIPs);
       robotInfo.add(roboPorts);
       serverInfo.add(serverIP);
       serverInfo.add(serverPorts);
       
       about.add(aboutDetails);
       
       //Add sub menus to menus
       options.add(robotInfo);
       options.add(serverInfo);
       help.add(about);
     
       //Add menus to menubar
       menuBar.add(options);
       menuBar.add(help);
       
       //Add the edit and submit buttons and textfield to be able to edit properties
       //First set up the buttons with the action performed method
       menuEditBtn.addActionListener(this);
       menuSubmitBtn.addActionListener(this);
       
       //Initially disable the submit button
       menuSubmitBtn.setEnabled(false);
       
       //Display some dummy IP initially
       menuTextField.setText("172.168.90.100");
       menuBar.add(menuEditBtn);
       menuBar.add(menuSubmitBtn);
       menuBar.add(menuTextField);
       
   }//setUpMenuBar()
   
   public void actionPerformed(ActionEvent e)
   {
	   if(controllersAreFound)
	   {
         //Start button
		   if(e.getActionCommand().equals(btnStart.getText()))
		   {
			   if(!isRunning)
			   {
				   //Set up coordinates
	    	       RobotCoordinates QB = new RobotCoordinates(0.0, 0.0, toRadians(90));
	    	       RobotCoordinates RC = new RobotCoordinates(15.0, 0.0, toRadians(90));
	    	       
	    	       //String to show who exactly is running
	    	       String whosRunning = "";
	    	         
	    	       if(playerOneIsConnected)
	    	       {
	    	          //ProcessUserInput QBSend = new ProcessUserInput("Player 1", foundControllers.get(0), RobotIPs[0], RobotPorts[0], QB, RC);
	    	    	    QBSend = new ProcessUserInput("Player 1", foundControllers.get(0), RobotIPs[0], RobotPorts[0], QB, RC);
	    	    	    //QBSend = new ProcessUserInput("Player 1", null, RobotIPs[0], RobotPorts[0], QB, RC);
	    	          QBReceive = new ProcessRobotPosition("Player 1", foundControllers.get(0), ServerIP, ServerPorts[0], QB, posQuar);
	    	          QBSend.setPriority(10);
	    	          QBReceive.setPriority(2);
	    	          QBSend.start();
	    	          QBReceive.start();
                   whosRunning += "QB";
	    	       }   
	    	       if(playerTwoIsConnected)
	    	       {
	    	    	    //ProcessUserInput RCSend = new ProcessUserInput("Player 2", foundControllers.get(1), RobotIPs[1], RobotPorts[1], QB, RC);
	    	    	    RCSend = new ProcessUserInput("Player 2", foundControllers.get(1), RobotIPs[1], RobotPorts[1], QB, RC);
	    	    	    //RCSend = new ProcessUserInput("Player 2", null, RobotIPs[1], RobotPorts[1], QB, RC);
	    	    	    RCReceive = new ProcessRobotPosition("Player 2", foundControllers.get(1), ServerIP, ServerPorts[1], RC, posRec);
		    	       RCSend.setPriority(10);
		    	       RCReceive.setPriority(2);
		    	       RCSend.start();
		    	       RCReceive.start();
                   whosRunning += "  & RC";		    	       
	    	       }
				   isRunning = true;
				   appState.setText("State: Running " + whosRunning);				   
			   }			   
		   }
         //Stop button			   
		   else if(e.getActionCommand().equals(btnStop.getText()))
		   {
			   if(isRunning)
			   {
	               try
	               {
	                  //Try to interrupt the threads so they complete and end
                     if(playerOneIsConnected)
                     {
                        QBSend.interrupt();
	                     QBReceive.interrupt();
                     }
	                  if(playerTwoIsConnected)
                     {
                        RCSend.interrupt();
	                     RCReceive.interrupt();
                     }
                     
	                  isRunning = false;
					      appState.setText("State: Stopped");
	               }
	               catch(Exception ex)
	               {
	                  appState.setText("Stop Button: " + ex.getMessage());
	                  System.out.println("Stop Button: " + ex.getMessage());
	               }				   				   
			   }			   
		   }	   
	   }
      //Listener for editing properties
      if(e.getActionCommand().equals(menuEditBtn.getText()))
      {
         //To prevent accidental edits
         if(!menuSubmitBtn.isEnabled())
         {
            menuSubmitBtn.setEnabled(true);
         }
      }
      if(e.getActionCommand().equals(menuSubmitBtn.getText()))
      {
         if(menuSubmitBtn.isEnabled())
         {
            //Get the new IP requested and set it up to parse correctly
            String testString = menuTextField.getText().toUpperCase();
            StringTokenizer tokens = new StringTokenizer(testString);
            
            //Variables used to get a VALID IP
            int periodCounter = 0;
            String newValue = "";
            int newPort = 0;
            
            //When submit button is pressed then edit robot properties if the strings have a character that starts with 'Q' or 'R'
            //For quarterback
            if(testString.charAt(0) == 'Q')
            {
               //Change IP only
               if((testString.substring(1,3)).equals("IP"))
               {
                  try
                  {
                     while(tokens.hasMoreElements())
                     {
                        newValue = tokens.nextToken();
                     }
                     
                     //Count how many periods are there
                     periodCounter = newValue.length() - newValue.replace(".","").length();
                     
                     //Check if it's a valid IP
                     if(periodCounter == 3)
                     {
                        RobotIPs[0] = newValue;                   
                        qbIP.setText("Quarterback: " + RobotIPs[0]);
                        menuSubmitBtn.setEnabled(false);
                     }
                     else
                     {
                        appState.setText("Invalid IP!");
                        System.out.println("Invalid IP!");
                     } 
                     
                  }
                  catch(Exception ex)
                  {
                     appState.setText("Submit Failed: " + ex.getMessage());
                     System.out.println("Submit Failed: " + ex.getMessage());   
                  }
               }
               //Change Port
               if((testString.substring(1,5)).equals("PORT"))
               {
                  try
                  {
                     while(tokens.hasMoreElements())
                     {
                        newValue = tokens.nextToken();
                     }
                     
                     newPort = Integer.parseInt(newValue);
                     
                     //A valid unused port is >= 1024
                     if(newPort >= 1024)
                     {
                        RobotPorts[0] = newPort;                   
                        qbPort.setText("Quarterback: " + RobotPorts[0]);
                        menuSubmitBtn.setEnabled(false);
                     }
                     else
                     {
                        appState.setText("Port # has to be >= 1024!");
                        System.out.println("Port # has to be >= 1024!");
                     } 
                     
                  }
                  catch(Exception ex)
                  {
                     appState.setText("Submit Faild: " + ex.getMessage());
                     System.out.println("Submit Faild: " + ex.getMessage());   
                  }
              }               
                  
            }
            //For receiver
            if(testString.charAt(0) == 'R')
            {
            	//Change IP only
                if((testString.substring(1,3)).equals("IP"))
                {
                   try
                   {
                      while(tokens.hasMoreElements())
                      {
                         newValue = tokens.nextToken();
                      }
                      
                      //Count how many periods are there
                      periodCounter = newValue.length() - newValue.replace(".","").length();
                      
                      //Check if it's a valid IP
                      if(periodCounter == 3)
                      {
                         RobotIPs[1] = newValue;                   
                         rcIP.setText("Receiver: " + RobotIPs[1]);
                         menuSubmitBtn.setEnabled(false);
                      }
                      else
                      {
                         appState.setText("Invalid IP!");
                         System.out.println("Invalid IP!");
                      } 
                      
                   }
                   catch(Exception ex)
                   {
                      appState.setText("Submit Faild: " + ex.getMessage());
                      System.out.println("Submit Faild: " + ex.getMessage());   
                   }
                }
                //Change Port
                if((testString.substring(1,5)).equals("PORT"))
                {
                   try
                   {
                      while(tokens.hasMoreElements())
                      {
                         newValue = tokens.nextToken();
                      }
                      
                      newPort = Integer.parseInt(newValue);
                      
                      //A valid unused port is >= 1024
                      if(newPort >= 1024)
                      {
                         RobotPorts[1] = newPort;                   
                         rcPort.setText("Receiver: " + RobotPorts[1]);
                         menuSubmitBtn.setEnabled(false);
                      }
                      else
                      {
                         appState.setText("Port # has to be >= 1024!");
                         System.out.println("Port # has to be >= 1024!");
                      } 
                      
                   }
                   catch(Exception ex)
                   {
                      appState.setText("Submit Faild: " + ex.getMessage());
                      System.out.println("Submit Faild: " + ex.getMessage());   
                   }
               }
            }
            //Change server properties
            if(testString.charAt(0) == 'S')
            {
            	//Change IP only
                if((testString.substring(1,3)).equals("IP"))
                {
                   try
                   {
                      while(tokens.hasMoreElements())
                      {
                         newValue = tokens.nextToken();
                      }
                      
                      //Count how many periods are there
                      periodCounter = newValue.length() - newValue.replace(".","").length();
                      
                      //Check if it's a valid IP
                      if(periodCounter == 3)
                      {
                         ServerIP = newValue;                   
                         servIP.setText(ServerIP);
                         menuSubmitBtn.setEnabled(false);
                      }
                      else
                      {
                         appState.setText("Invalid IP!");
                         System.out.println("Invalid IP!");
                      } 
                      
                   }
                   catch(Exception ex)
                   {
                      appState.setText("Submit Faild: " + ex.getMessage());
                      System.out.println("Submit Faild: " + ex.getMessage());   
                   }
                }
                //Change Port1
                if((testString.substring(1,6)).equals("PORT1"))
                {
                   try
                   {
                      while(tokens.hasMoreElements())
                      {
                         newValue = tokens.nextToken();
                      }
                      
                      newPort = Integer.parseInt(newValue);
                      
                      //A valid unused port is >= 1024
                      if(newPort >= 1024)
                      {
                         ServerPorts[0] = newPort;                   
                         servPort1.setText("Quarterback: " + ServerPorts[0]);
                         menuSubmitBtn.setEnabled(false);
                      }
                      else
                      {
                         appState.setText("Port # has to be >= 1024!");
                         System.out.println("Port # has to be >= 1024!");
                      } 
                      
                   }
                   catch(Exception ex)
                   {
                      appState.setText("Submit Faild: " + ex.getMessage());
                      System.out.println("Submit Faild: " + ex.getMessage());   
                   }
               }
               //Change Port2
               if((testString.substring(1,6)).equals("PORT2"))
               {
            	   try
            	   {
            		   while(tokens.hasMoreElements())
            		   {
            			   newValue = tokens.nextToken();
            		   }
                  
            		   newPort = Integer.parseInt(newValue);
            		   
                     //A valid unused port is >= 1024
            		   if(newPort >= 1024)
            		   {
            			   ServerPorts[1] = newPort;                   
            			   servPort2.setText("Receiver: " + ServerPorts[1]);
            			   menuSubmitBtn.setEnabled(false);
            		   }
            		   else
            		   {
            			   appState.setText("Port # has to be >= 1024!");
            			   System.out.println("Port # has to be >= 1024!");
            		   } 
                  
            	   }
            	   catch(Exception ex)
            	   {
            		   appState.setText("Submit Faild: " + ex.getMessage());
            		   System.out.println("Submit Faild: " + ex.getMessage());   
            	   }
               }
            }
         }
      }	   
   }//actionPerformed()






}