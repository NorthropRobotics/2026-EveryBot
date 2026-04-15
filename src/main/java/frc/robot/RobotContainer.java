// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static frc.robot.Constants.OperatorConstants.DRIVER_CONTROLLER_PORT;
import static frc.robot.Constants.OperatorConstants.OPERATOR_CONTROLLER_PORT;

import java.util.Optional;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest.FieldCentricFacingAngle;
import com.ctre.phoenix6.swerve.SwerveRequest.ForwardPerspectiveValue;
import com.revrobotics.encoder.DetachedEncoder.PeriodicStatus0;
import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.swerve.SwerveRequest;

import choreo.auto.AutoFactory;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.UsbCamera;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.CommandJoystick;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.FieldObject2d;
import frc.robot.Util.Targeting;
// import frc.robot.commands.ClimbDown;
// import frc.robot.commands.ClimbUp;
import frc.robot.commands.Eject;
import frc.robot.commands.Intake;
import frc.robot.commands.Launch;
import frc.robot.commands.LaunchSequence;
import frc.robot.commands.SpinUp;
import frc.robot.Util.Targeting;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CANFuelSubsystem;
// import frc.robot.subsystems.ClimberSubsystem;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.Vision;
@SuppressWarnings("unused")
public class RobotContainer {
            public double applyInputShaping(double joystickAxis){
            double deadband = .15;
            double exponet = 2;
            if(Math.abs(joystickAxis) < deadband) return 0;
            return Math.signum(joystickAxis) * Math.pow((Math.abs(joystickAxis)-deadband)/(1-deadband), exponet);
        }
    private double MaxSpeed = 1.0 * TunerConstants.kSpeedAt12Volts.in(MetersPerSecond); // kSpeedAt12Volts desired top speed
    private double MaxAngularRate = RotationsPerSecond.of(0.75).in(RadiansPerSecond); // 3/4 of a rotation per second max angular velocity


    /* Setting up bindings for necessary control of the swerve drive platform */
    private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
            .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
            .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors
        private final SwerveRequest.FieldCentricFacingAngle operatorRequest = new FieldCentricFacingAngle()
        .withHeadingPID(10,0,0)
        .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
            .withDriveRequestType(DriveRequestType.OpenLoopVoltage);

        public final SwerveRequest.FieldCentricFacingAngle targetHub = new SwerveRequest.FieldCentricFacingAngle()
                        .withHeadingPID(10, 0, 0)
                        // Add a 10% deadband
                        .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1)
                        .withDriveRequestType(DriveRequestType.Velocity)
                        // Flip perspective (angle and velocity) when on red
                        .withForwardPerspective(ForwardPerspectiveValue.OperatorPerspective);
            //A Robot Centric Option for Alfy Testing 
        // private final SwerveRequest.RobotCentric drive = new SwerveRequest.RobotCentric()
        //     .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
        //     .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors
    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();

    private final Telemetry logger = new Telemetry(MaxSpeed);

    private final CommandXboxController joystick = new CommandXboxController(DRIVER_CONTROLLER_PORT);
    private final CommandJoystick operatorStick = new CommandJoystick(OPERATOR_CONTROLLER_PORT);

    // README: Subsystems
    public final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
    private final CANFuelSubsystem fuelSubsystem = new CANFuelSubsystem();
    
    private final Vision vision = new Vision(drivetrain);
    
    //private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();

    // README: Autonomous
    private final SendableChooser<Command> autoChooser = new SendableChooser<>();
    
        public RobotContainer() {
        configureBindings();
        SmartDashboard.putNumber("Match Time", DriverStation.getMatchTime());
        try (Field2d m_field = new Field2d()) {
            SmartDashboard.putData("Field", m_field);
        }
        
        // README: Autonomous
        var choreoFactory = new AutoFactory(
            drivetrain::getPose,
            drivetrain::resetPose,
            drivetrain::followTrajectory,
            true,
            drivetrain
        );
        //Hub Basic auto
        Command backUpAndShoot = Commands.sequence(choreoFactory.resetOdometry("backUpHub"),
        choreoFactory.trajectoryCmd("backUpHub"),
        Commands.runOnce(() ->  Launch.adjustedSpeed = 0.8),
        Commands.parallel((new LaunchSequence(fuelSubsystem)), drivetrain.applyRequest(() -> brake)),
        new LaunchSequence(fuelSubsystem));

        //Depot Basic Auto
        Command backUpAndShootDepot = Commands.sequence(choreoFactory.resetOdometry("BackUpDepot"),
        choreoFactory.trajectoryCmd("BackUpDepot"),
        Commands.runOnce(() ->  Launch.adjustedSpeed = .80),
        Commands.parallel((new LaunchSequence(fuelSubsystem)), drivetrain.applyRequest(() -> brake)),
        new LaunchSequence(fuelSubsystem));

        //Outpost Basic Auto
        Command backUpAndShootOutpost = Commands.sequence(choreoFactory.resetOdometry("BackUpOutpost"),
        choreoFactory.trajectoryCmd("BackUpOutpost"),
        Commands.runOnce(() ->  Launch.adjustedSpeed = .80),
        Commands.parallel((new LaunchSequence(fuelSubsystem)), drivetrain.applyRequest(() -> brake)),
        new LaunchSequence(fuelSubsystem));

        //Hub Sweep Auto
        // Command HubSweep = Commands.sequence(choreoFactory.resetOdometry("backUpHub"),
        // Commands.parallel(
        //     choreoFactory.trajectoryCmd("backUpHub"),
        //     Commands.sequence(
        //         Commands.runOnce(() ->  Launch.adjustedSpeed = 0.8),
        //         Commands.parallel((new SpinUp(fuelSubsystem)),))))


        autoChooser.setDefaultOption("Basic Hub", backUpAndShoot);
        autoChooser.addOption("Basic Outpost", backUpAndShootOutpost);
        autoChooser.addOption("Basic Depot", backUpAndShootDepot);
        SmartDashboard.putData("Auto Chooser", autoChooser);
        //README: Telemetry & Cameras
        // Check if this is a simulation. If yes, don't run camera init code
        if(!Utils.isSimulation()){
        UsbCamera cam0 = CameraServer.startAutomaticCapture();
        UsbCamera cam1 = CameraServer.startAutomaticCapture();
        
        cam0.setResolution(320, 240);
        cam0.setFPS(15);
        cam1.setResolution(320, 240);
        cam1.setFPS(15);
        }
    }

    private void configureBindings() {
        // Note that X is defined as forward according to WPILib convention,
        // and Y is defined as to the left according to WPILib convention.
        drivetrain.setDefaultCommand(
            // Drivetrain will execute this command periodically
            drivetrain.applyRequest(() ->
                drive.withVelocityX(-applyInputShaping(joystick.getLeftY()) * MaxSpeed) // Drive forward with negative Y (forward)
                    .withVelocityY(-applyInputShaping(joystick.getLeftX()) * MaxSpeed) // Drive left with negative X (left)
                    .withRotationalRate(-joystick.getRightX() * MaxAngularRate) // Drive counterclockwise with negative X (left)
            )
        );
        // drivetrain.setDefaultCommand(
        //     // Drivetrain will execute this command periodically
        //     drivetrain.applyRequest(() ->
        //         drive.withRotationalRate(-operatorController.getRightX() * MaxAngularRate) // Drive counterclockwise with negative X (left)
        //     )
        // );

        // Idle while the robot is disabled. This ensures the configured
        // neutral mode is applied to the drive motors while disabled.
        final var idle = new SwerveRequest.Idle();
        RobotModeTriggers.disabled().whileTrue(
            drivetrain.applyRequest(() -> idle).ignoringDisable(true)
        );

        // README: Controller Bindings > Driver Controller
        joystick.a().whileTrue(drivetrain.applyRequest(() -> brake));
        joystick.b().whileTrue(drivetrain.applyRequest(() ->
            point.withModuleDirection(new Rotation2d(-joystick.getLeftY(), -joystick.getLeftX()))
        ));

        // Run SysId routines when holding back/start and X/Y.
        // Note that each routine should be run exactly once in a single log.
        joystick.back().and(joystick.y()).whileTrue(drivetrain.sysIdDynamic(Direction.kForward));
        joystick.back().and(joystick.x()).whileTrue(drivetrain.sysIdDynamic(Direction.kReverse));
        joystick.start().and(joystick.y()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kForward));
        joystick.start().and(joystick.x()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kReverse));

        // Reset the field-centric heading on left bumper press.
                        // zero gyro yaw on right bumper press
        joystick.leftBumper().onTrue(drivetrain.runOnce(() ->
        // get alliance and make sure it exists
        DriverStation.getAlliance().ifPresent((alliance) ->
        // set gyro to 0 if blue, 180 if red
        drivetrain.getPigeon2().setYaw(alliance == Alliance.Blue ? 0 : 180))));
        // Options to have all controls on the driver contoller. Comment out if using two
        joystick.y().whileTrue(new Intake(fuelSubsystem));
        joystick.rightBumper().whileTrue(new LaunchSequence(fuelSubsystem));
        joystick.x().whileTrue(new Eject(fuelSubsystem));
        joystick.povLeft().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 0.2));
        joystick.povUp().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = .75));
        joystick.povDown().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 0.87));
        joystick.povRight().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 1.0));
        joystick.rightTrigger().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = Launch.adjustedSpeed+.05));
        joystick.leftTrigger().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = Launch.adjustedSpeed-.05));
        drivetrain.registerTelemetry(logger::telemeterize);

        // Fuel and climber subsystem default commands — stop motors when no button held
        fuelSubsystem.setDefaultCommand(fuelSubsystem.run(() -> fuelSubsystem.stop()));
        // climberSubsystem.setDefaultCommand(climberSubsystem.run(() -> climberSubsystem.stop()));

        // README: Controller Bindings > Operator Controller
        operatorStick.button(17).whileTrue(new Intake(fuelSubsystem));
        operatorStick.button(18).whileTrue(new LaunchSequence(fuelSubsystem));
        operatorStick.button(16).whileTrue(new Eject(fuelSubsystem));
        //operatorStick.povLeft().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 0.2));
        operatorStick.button(3).onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = .75));
        //operatorStick.povDown().onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 0.87));
        operatorStick.button(1).onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = 1.0));
        operatorStick.button(2).onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = Launch.adjustedSpeed+.05));
        operatorStick.button(4).onTrue(Commands.runOnce(() ->  Launch.adjustedSpeed = Launch.adjustedSpeed-.05));
        operatorStick.button(12).whileTrue(Commands.run(() ->  Launch.adjustedSpeed = ((SmartDashboard.getNumber("Distance", 1))*0.116)+.513));
        operatorStick.button(10).whileTrue(drivetrain.applyRequest( () -> {return targetHub.withTargetDirection(
        Targeting.getTargetRotation(drivetrain.getPose()))
        .withVelocityX(-applyInputShaping(joystick.getLeftY()) * MaxSpeed) // Drive forward with negative Y (forward)
                    .withVelocityY(-applyInputShaping(joystick.getLeftX()) * MaxSpeed);})
    );

        operatorStick.button(9).whileTrue(drivetrain.applyRequest(() -> operatorRequest.withTargetDirection(getTargetRotation()).withVelocityX(-applyInputShaping(joystick.getLeftY()) * MaxSpeed) // Drive forward with negative Y (forward)
                    .withVelocityY(-applyInputShaping(joystick.getLeftX()) * MaxSpeed)));
    }   
public Rotation2d getTargetRotation(){
return new Rotation2d(-Math.atan2(operatorStick.getRawAxis(1),operatorStick.getRawAxis(0))+Math.PI/2);
}
    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }
}
