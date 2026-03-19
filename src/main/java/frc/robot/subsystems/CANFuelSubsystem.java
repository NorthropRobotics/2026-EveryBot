// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import static frc.robot.Constants.FuelConstants.INDEXER_INTAKING_PERCENT;
import static frc.robot.Constants.FuelConstants.INDEXER_LAUNCHING_PERCENT;
import static frc.robot.Constants.FuelConstants.INDEXER_MOTOR_ID;
import static frc.robot.Constants.FuelConstants.INTAKE_INTAKING_PERCENT;
import static frc.robot.Constants.FuelConstants.LAUNCHING_LAUNCHER_PERCENT;
import static frc.robot.Constants.FuelConstants.LEFT_INTAKE_LAUNCHER_MOTOR_ID;
import static frc.robot.Constants.FuelConstants.RIGHT_INTAKE_LAUNCHER_MOTOR_ID;

import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.commands.Launch;

public class CANFuelSubsystem extends SubsystemBase {
  // private final SparkMax LeftIntakeLauncher;
  // private final SparkMax RightIntakeLauncher;
  // private final SparkMax Indexer;

  private final TalonFX LeftIntakeLauncher;
  private final TalonFX RightIntakeLauncher;
  private final TalonFX Indexer;

  /** Creates a new CANBallSubsystem. */
  public CANFuelSubsystem() {
    // // create brushed motors for each of the motors on the launcher mechanism
    // LeftIntakeLauncher = new SparkMax(LEFT_INTAKE_LAUNCHER_MOTOR_ID, MotorType.kBrushless);
    // RightIntakeLauncher = new SparkMax(RIGHT_INTAKE_LAUNCHER_MOTOR_ID, MotorType.kBrushless);
    // Indexer = new SparkMax(INDEXER_MOTOR_ID, MotorType.kBrushed);

    // // create the configuration for the feeder roller, set a current limit and apply
    // // the config to the controller
    // SparkMaxConfig feederConfig = new SparkMaxConfig();
    // feederConfig.smartCurrentLimit(INDEXER_MOTOR_CURRENT_LIMIT);
    // Indexer.configure(feederConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    // // create the configuration for the launcher roller, set a current limit, set
    // // the motor to inverted so that positive values are used for both intaking and
    // // launching, and apply the config to the controller
    // SparkMaxConfig launcherConfig = new SparkMaxConfig();

    // launcherConfig.smartCurrentLimit(LAUNCHER_MOTOR_CURRENT_LIMIT);
    // launcherConfig.voltageCompensation(12);
    // launcherConfig.idleMode(IdleMode.kCoast);
    // RightIntakeLauncher.configure(launcherConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    // launcherConfig.inverted(true);
    // LeftIntakeLauncher.configure(launcherConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    //declare 
    LeftIntakeLauncher = new TalonFX(LEFT_INTAKE_LAUNCHER_MOTOR_ID);
    RightIntakeLauncher = new TalonFX(RIGHT_INTAKE_LAUNCHER_MOTOR_ID);
    Indexer = new TalonFX(INDEXER_MOTOR_ID);

    LeftIntakeLauncher.getConfigurator().apply(Constants.FuelConstants.LEFT_INTAKE_LAUNCHER_CONFIG);
    RightIntakeLauncher.getConfigurator().apply(Constants.FuelConstants.RIGHT_INTAKE_LAUNCHER_CONFIG);
    Indexer.getConfigurator().apply(Constants.FuelConstants.INDEXER_CONFIG);

    // put default values for various fuel operations onto the dashboard
    // all commands using this subsystem pull values from the dashbaord to allow
    // you to tune the values easily, and then replace the values in Constants.java
    // with your new values. For more information, see the Software Guide.
    // README: Subsystems > CANFuelSubsystem (SmartDashboard tuning keys table)
    SmartDashboard.putNumber("Intaking feeder roller value", INDEXER_INTAKING_PERCENT);
    SmartDashboard.putNumber("Intaking intake roller value", INTAKE_INTAKING_PERCENT);
    SmartDashboard.putNumber("Launching feeder roller value", INDEXER_LAUNCHING_PERCENT);
    SmartDashboard.putNumber("Launching launcher roller value", LAUNCHING_LAUNCHER_PERCENT);
    //SmartDashboard.putNumber("Spin-up feeder roller value", SPIN_UP_FEEDER_VOLTAGE);
  }

  // A method to set the voltage of the intake roller
  public void setIntakeLauncherRoller(double power) {
    LeftIntakeLauncher.set(power);
    RightIntakeLauncher.set(power); // positive for shooting
  }

  // A method to set the voltage of the intake roller
  public void setFeederRoller(double power) {
    Indexer.set(power); // positive for shooting (Launch.adjustedSpeed *)
  }

  // A method to stop the rollers
  public void stop() {
    Indexer.set(0);
    LeftIntakeLauncher.set(0);
    RightIntakeLauncher.set(0);
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
