# TODO: Wire Up Intake, Shooter, Climb + Choreo Auto

All 7 command files (`Intake`, `SpinUp`, `Launch`, `LaunchSequence`, `Eject`, `ClimbUp`, `ClimbDown`) and both non-drive subsystems (`CANFuelSubsystem`, `ClimberSubsystem`) already exist with correct CAN IDs and TalonFX/SparkMax configurations. The remaining work is wiring, a SmartDashboard addition, and Choreo autonomous.

---

## Checklist

### Step 1 — Add ChoreoLib Vendor Dependency

- [x] URL: `https://choreo.autos/lib/ChoreoLib2026.json`
- [x] Confirm `vendordeps/ChoreoLib.json` appears and Gradle refreshes

---

### Step 2 — Add SmartDashboard Tuning to `CANFuelSubsystem`

File: `src/main/java/frc/robot/subsystems/CANFuelSubsystem.java`

Add to the constructor (after motor configuration) so values are live-tunable from Shuffleboard:

```java
SmartDashboard.putNumber("Intaking feeder roller value", INDEXER_INTAKING_PERCENT);
SmartDashboard.putNumber("Intaking intake roller value", INTAKE_INTAKING_PERCENT);
SmartDashboard.putNumber("Launching feeder roller value", INDEXER_LAUNCHING_PERCENT);
SmartDashboard.putNumber("Launching launcher roller value", LAUNCHING_LAUNCHER_PERCENT);
```

Add import: `import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;`

> After field tuning, copy the final values back into `FuelConstants` in `Constants.java`.

---

### Step 3 — Wire Subsystems into `RobotContainer`

File: `src/main/java/frc/robot/RobotContainer.java`

#### 3a — Add subsystem fields
```java
private final CANFuelSubsystem fuelSubsystem = new CANFuelSubsystem();
private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();
```

#### 3b — Add operator controller field
```java
private final CommandXboxController operatorController =
    new CommandXboxController(OperatorConstants.OPERATOR_CONTROLLER_PORT);
```

#### 3c — Add auto chooser field
```java
private final SendableChooser<Command> autoChooser = new SendableChooser<>();
```

#### 3d — Add default commands (in `configureBindings()`)
```java
fuelSubsystem.setDefaultCommand(fuelSubsystem.run(() -> fuelSubsystem.stop()));
climberSubsystem.setDefaultCommand(climberSubsystem.run(() -> climberSubsystem.stop()));
```

#### 3e — Bind operator controller buttons (in `configureBindings()`)

| Button | Action |
|--------|--------|
| Left bumper (hold) | `new Intake(fuelSubsystem)` |
| Right bumper (hold) | `new LaunchSequence(fuelSubsystem)` |
| A button (hold) | `new Eject(fuelSubsystem)` |
| D-pad Up (hold) | `new ClimbUp(climberSubsystem)` |
| D-pad Down (hold) | `new ClimbDown(climberSubsystem)` |

```java
operatorController.leftBumper().whileTrue(new Intake(fuelSubsystem));
operatorController.rightBumper().whileTrue(new LaunchSequence(fuelSubsystem));
operatorController.a().whileTrue(new Eject(fuelSubsystem));
operatorController.povUp().whileTrue(new ClimbUp(climberSubsystem));
operatorController.povDown().whileTrue(new ClimbDown(climberSubsystem));
```

#### 3f — Add missing imports
```java
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import frc.robot.commands.ClimbDown;
import frc.robot.commands.ClimbUp;
import frc.robot.commands.Eject;
import frc.robot.commands.Intake;
import frc.robot.commands.LaunchSequence;
import frc.robot.subsystems.CANFuelSubsystem;
import frc.robot.subsystems.ClimberSubsystem;
import static frc.robot.Constants.OperatorConstants.*;
```

---

### Step 4 — Choreo Autonomous

#### 4a — Create trajectory in Choreo desktop app
- [x] Open [Choreo](https://choreo.autos) desktop app
- [ ] Create a trajectory (e.g. `ShootAndDrive`) — start in shooting zone, drive to next game piece or scoring position
- [ ] Save to `src/main/deploy/choreo/` (Gradle deploys this folder automatically)
- [ ] Add a named event marker in Choreo called `"launch"` at the point you want shooting to fire

#### 4b — Wire Choreo auto into `RobotContainer`

Replace the current inline swerve auto command with a Choreo-based routine:

```java
// In constructor, after configureBindings():
var choreoFactory = new ChoreoAutoFactory(
    drivetrain::getPose,
    drivetrain::resetPose,
    drivetrain::followTrajectory,
    true,   // alliance flip
    drivetrain
);

Command shootAndDrive = choreoFactory.trajectoryCmd("ShootAndDrive")
    .beforeStarting(new LaunchSequence(fuelSubsystem));

autoChooser.setDefaultOption("Shoot and Drive", shootAndDrive);
SmartDashboard.putData("Auto Chooser", autoChooser);
```

Update `getAutonomousCommand()`:
```java
public Command getAutonomousCommand() {
    return autoChooser.getSelected();
}
```

> Note: `CommandSwerveDrivetrain` must expose `getPose()`, `resetPose()`, and `followTrajectory()` methods compatible with Choreo. Check the [ChoreoLib docs](https://choreo.autos/usage/using-choreo-in-auto/) for the exact swerve integration pattern with CTRE's `CommandSwerveDrivetrain`.

---

## Hardware Reference

> For wiring verification — do not change these IDs.

| CAN ID | Device | Controller | Subsystem |
|--------|--------|------------|-----------|
| 0 | Pigeon2 IMU | — | Swerve |
| 1 | BL CANcoder | — | Swerve |
| 2 | BL Steer | TalonFX | Swerve |
| 3 | BL Drive | TalonFX | Swerve |
| 4 | FL CANcoder | — | Swerve |
| 5 | FL Steer | TalonFX | Swerve |
| 6 | FL Drive | TalonFX | Swerve |
| 7 | FR CANcoder | — | Swerve |
| 8 | FR Steer | TalonFX | Swerve |
| 9 | FR Drive | TalonFX | Swerve |
| 10 | BR CANcoder | — | Swerve |
| 11 | BR Steer | TalonFX | Swerve |
| 12 | BR Drive | TalonFX | Swerve |
| 13 | Indexer | TalonFX | `CANFuelSubsystem` |
| 14 | Climber | SparkMax (brushed) | `ClimberSubsystem` |
| 15 | Right intake/launcher | TalonFX | `CANFuelSubsystem` |
| 16 | Left intake/launcher | TalonFX | `CANFuelSubsystem` |

---

## Verification Checklist

- [x] `./gradlew build` — clean compile, no errors
- [ ] `./gradlew deploy` — successful deploy, no CAN faults in Driver Station
- [ ] **Phoenix Tuner X**: TalonFX IDs 13, 15, 16 respond to operator left bumper (intake), right bumper (launch), A (eject)
- [ ] **REV Hardware Client**: SparkMax ID 14 responds to operator D-pad Up/Down
- [ ] **Shuffleboard**: 4 fuel tuning values appear (`Intaking feeder roller value`, etc.)
- [ ] **Shuffleboard**: `Auto Chooser` dropdown appears in autonomous tab
- [ ] **Field test**: intake picks up game piece, `LaunchSequence` spins up then fires, climber holds position in brake mode when released
- [ ] **Choreo**: trajectory loads and runs in auto, `launch` event fires at correct point on path
