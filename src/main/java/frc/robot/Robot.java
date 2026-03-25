// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.io.ObjectInputStream.GetField;
import java.util.Optional;

import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.util.sendable.SendableRegistry;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.commands.Launch;

// README: Subsystems > AdvantageKit Logging
public class Robot extends LoggedRobot {
    private Command m_autonomousCommand;

    private final RobotContainer m_robotContainer;

    public Robot() {
        // Configure AdvantageKit logger
        Logger.recordMetadata("ProjectName", "10434-Northrop-Cybears");

        if (isReal()) {
            Logger.addDataReceiver(new WPILOGWriter()); // Log to USB stick
            Logger.addDataReceiver(new NT4Publisher()); // Publish to NetworkTables
        } else {
            Logger.addDataReceiver(new WPILOGWriter());
            Logger.addDataReceiver(new NT4Publisher());
        }

        Logger.start();

        m_robotContainer = new RobotContainer();
    }
    double matchTime;
    String gameData;
    boolean redFirstShift;
    boolean hubActive;
    boolean WeFirstShift;
    double remainingShiftTime;
    Optional<Alliance> alliance;
    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();
    }

    @Override
    public void disabledInit() {}

    @Override
    public void disabledPeriodic() {}

    @Override
    public void disabledExit() {}

    @Override
    public void autonomousInit() {
        m_autonomousCommand = m_robotContainer.getAutonomousCommand();

        if (m_autonomousCommand != null) {
            CommandScheduler.getInstance().schedule(m_autonomousCommand);
        }
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void autonomousExit() {}

    @Override
    public void teleopInit() {
        if (m_autonomousCommand != null) {
            CommandScheduler.getInstance().cancel(m_autonomousCommand);
        }
    }

    @Override
    public void teleopPeriodic() {
       {

    alliance = DriverStation.getAlliance();
    matchTime = DriverStation.getMatchTime();
    gameData = DriverStation.getGameSpecificMessage();
    
    if(gameData.isEmpty() || alliance.isEmpty())
    {
      hubActive = true;
    }
    else
    {
      if(gameData.charAt(0) == 'R')
      {
        redFirstShift = false;
      } 
      else 
      {
        redFirstShift = true;
      }

      if((alliance.get() == Alliance.Red && redFirstShift) || (alliance.get() == Alliance.Blue && !redFirstShift))
      {
        WeFirstShift = true;
      }
      else
      {
        WeFirstShift = false;
      }


      if (matchTime > 130) {
        // Transition shift, hub is active.
        remainingShiftTime = matchTime - 130;
        hubActive = true;
      } 
      else if (matchTime > 105) 
      {
        // Shift 1
        remainingShiftTime = matchTime - 105;
        hubActive = WeFirstShift;
      } 
      else if (matchTime > 80) 
      {
        // Shift 2
        remainingShiftTime = matchTime - 80;
        hubActive = !WeFirstShift;
      } 
      else if (matchTime > 55) 
      {
        // Shift 3
        remainingShiftTime = matchTime - 55;
        hubActive = WeFirstShift;
      } 
      else if (matchTime > 30) 
      {
        // Shift 4
        remainingShiftTime = matchTime - 30;
        hubActive = !WeFirstShift;
      } 
      else 
      {
        // End game, hub always active.
        remainingShiftTime = matchTime;
        hubActive = true;
      }
    }
    SmartDashboard.putNumber("shift timer", remainingShiftTime);
    SmartDashboard.putBoolean("hub active", hubActive); }
    SmartDashboard.putNumber("Adjusted Launch Speed", Launch.adjustedSpeed);
    }

    @Override
    public void teleopExit() {}

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void testPeriodic() {}

    @Override
    public void testExit() {}

    @Override
    public void simulationPeriodic() {}
}
