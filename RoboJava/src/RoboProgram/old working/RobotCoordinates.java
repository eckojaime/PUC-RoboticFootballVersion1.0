/*
//------------------------------------------------------------//

**************		****		****		**************		
**************		****		****		**************
****		 ****		****		****		****
****		 ****		****		****		****
**************		****		****		****
**************		****		****		****
****					****		****		****
****					****		****		****
****					*************		**************
****					*************		**************		

-------------------------------------------------------------//
Programmer				Date				Modification Reason

Jaime Alvarez			01-31-14			Initial Implementation


//-----------------------------------------------------------//

Description:
This class is to contain the coordinates of a robot
The reason we used a class to represent this is so the 
ProcessUserInput thread object will see the position of
the quarterback and the receiver to determine when it's
good to throw while the ProcessRobotPosition thread will
update the positions accordingly. The beauty of pass by 
reference.
*/

package RoboProgram;

class RobotCoordinates
{
   //The current position of the robot
   private double Xcurrent = 0.0f;
   private double Ycurrent = 0.0f;
   //In radians
   private double PHIcurrent = 0.0f;
   
   //Start the robot in a specified coordinate
   public RobotCoordinates(double x, double y, double phi)
   {
      Xcurrent = x;
      Ycurrent = y;
      PHIcurrent = phi;
   }
   
   //Set methods
   public void setX(double x){Xcurrent = x;}
   public void setY(double y){Ycurrent = y;}
   public void setPHI(double phi){PHIcurrent = phi;}
   
   //Get methods
   public double getX(){return Xcurrent;}
   public double getY(){return Ycurrent;}
   public double getPHI(){return PHIcurrent;}
}