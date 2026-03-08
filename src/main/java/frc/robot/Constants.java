// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.revrobotics.spark.FeedbackSensor;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkFlexConfig;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide
 * numerical or boolean constants. This class should not be used for any other
 * purpose. All constants should be declared globally (i.e. public static). Do
 * not put anything functional in this class.
 *
 * <p>
 * It is advised to statically import this class (or one of its inner classes)
 * wherever the constants are needed, to reduce verbosity. 
 */
public final class Constants {

  // README: Subsystems > CANFuelSubsystem | Configuration & Tuning > FuelConstants
  public static final class FuelConstants {
    // Motor controller IDs for Fuel Mechanism motors
    public static final int LEFT_INTAKE_LAUNCHER_MOTOR_ID = 16;
    public static final int RIGHT_INTAKE_LAUNCHER_MOTOR_ID = 15;
    public static final int INDEXER_MOTOR_ID = 13; // labeled feeder

    // Current limit for fuel mechanism motors.
    public static final int INDEXER_MOTOR_CURRENT_LIMIT = 80;
    public static final int LAUNCHER_MOTOR_CURRENT_LIMIT = 80;

    // All values likely need to be tuned based on your robot
    public static final double INDEXER_INTAKING_PERCENT = -.8; 
    public static final double INDEXER_LAUNCHING_PERCENT = 0.6;
    public static final double INDEXER_SPIN_UP_PRE_LAUNCH_PERCENT = -0.5;

    public static final double INTAKE_INTAKING_PERCENT = 0.6;
    public static final double LAUNCHING_LAUNCHER_PERCENT = .85;
    public static final double INTAKE_EJECT_PERCENT = -0.8;

    public static final double SPIN_UP_SECONDS = 0.75;

    public static final TalonFXConfiguration LEFT_INTAKE_LAUNCHER_CONFIG = new TalonFXConfiguration();
    static {
      LEFT_INTAKE_LAUNCHER_CONFIG.CurrentLimits.StatorCurrentLimit = LAUNCHER_MOTOR_CURRENT_LIMIT;
      LEFT_INTAKE_LAUNCHER_CONFIG.MotorOutput.NeutralMode = NeutralModeValue.Coast;
      LEFT_INTAKE_LAUNCHER_CONFIG.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
    }

    public static final TalonFXConfiguration RIGHT_INTAKE_LAUNCHER_CONFIG = new TalonFXConfiguration();
    static {
      RIGHT_INTAKE_LAUNCHER_CONFIG.CurrentLimits.StatorCurrentLimit = LAUNCHER_MOTOR_CURRENT_LIMIT;
      RIGHT_INTAKE_LAUNCHER_CONFIG.MotorOutput.NeutralMode = NeutralModeValue.Coast;
      RIGHT_INTAKE_LAUNCHER_CONFIG.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
    }

    public static final TalonFXConfiguration INDEXER_CONFIG = new TalonFXConfiguration();
    static {
      INDEXER_CONFIG.CurrentLimits.StatorCurrentLimit = LAUNCHER_MOTOR_CURRENT_LIMIT;
      INDEXER_CONFIG.MotorOutput.NeutralMode = NeutralModeValue.Coast;
      INDEXER_CONFIG.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
    }

  }

  // README: Subsystems > ClimberSubsystem | Configuration & Tuning > ClimbConstatns
  public static final class ClimbConstatns {
    // Motor controller IDs for Climb motor
    public static final int CLIMBER_MOTOR_ID = 14;

    // Current limit for climb motor
    public static final int CLIMBER_MOTOR_CURRENT_LIMIT = 40;
    // Percentage to power the motor both up and down
    public static final double CLIMBER_MOTOR_DOWN_PERCENT = -0.8;
    public static final double CLIMBER_MOTOR_UP_PERCENT = 0.8;
  }

  // README: Controller Bindings | Configuration & Tuning > OperatorConstants
  public static final class OperatorConstants {

    // Port constants for driver and operator controllers. These should match the
    // values in the Joystick tab of the Driver Station software
    public static final int DRIVER_CONTROLLER_PORT = 0;
    public static final int OPERATOR_CONTROLLER_PORT = 1;

    // This value is multiplied by the joystick value when rotating the robot to
    // help avoid turning too fast and beign difficult to control
    public static final double DRIVE_SCALING = 0.7;
    public static final double ROTATION_SCALING = 0.8;
  } 
}
