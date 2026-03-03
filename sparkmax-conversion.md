# SparkMax → Talon FX Conversion Notes

**Date:** March 3, 2026  
**Branch:** main  
**Scope:** Full codebase review for REV SparkMax motor controller references to be replaced with CTRE Talon FX (Phoenix 6)

---

## Summary

The Everybot codebase was imported with REV Robotics SparkMax motor controllers throughout the fuel (intake/launcher) and climber subsystems. The swerve drivetrain already uses CTRE Phoenix 6 (Talon FX) and the Phoenix 6 vendordep (`Phoenix6-26.1.0.json`) is already present. There is **no** REV Robotics vendordep present — it will need to be removed from any dependency listing and the Phoenix 6 API used exclusively.

There is also a **ghost drivetrain subsystem reference** (`CANDriveSubsystem`) in two command files that does not exist in the current codebase, left over from the original tank-drive Everybot code. Those files will also need to be cleaned up.

---

## ⚠️ CAN ID Verification Required

Before deploying, confirm that the CAN IDs assigned to the fuel and climber motors in `Constants.java` do not overlap with any other device on the robot's CAN bus — including the swerve devices defined in `TunerConstants.java`.

The swerve module IDs currently in `TunerConstants.java` for reference:

| ID | Device |
|---|---|
| 0 | Pigeon 2 IMU |
| 1 | Back Left CANcoder |
| 2 | Back Left steer motor |
| 3 | Back Left drive motor |
| 4 | Front Left CANcoder |
| 5 | Front Left steer motor |
| 6 | Front Left drive motor |
| 7 | Front Right CANcoder |
| 8 | Front Right steer motor |
| 9 | Front Right drive motor |
| 10 | Back Right CANcoder |
| 11 | Back Right steer motor |
| 12 | Back Right drive motor |

The fuel and climber motor IDs (currently 5, 6, 7, 8 in `Constants.java`) were inherited from the original Everybot and are placeholders — **they have not been assigned to physical hardware yet**. Once the Talon FX devices are physically installed and configured via Tuner X, update `Constants.java` to match whatever IDs are flashed onto them, ensuring no two devices on the bus share an ID.

---



| Action | File |
|--------|------|
| **Remove** REV Robotics vendordep (if added) | `vendordeps/REVLib-*.json` |
| **Keep** CTRE Phoenix 6 vendordep (already present) | `vendordeps/Phoenix6-26.1.0.json` |

> ⚠️ No REV vendordep JSON was found in the workspace, but the code will not compile until all `com.revrobotics.*` imports are removed and replaced with CTRE equivalents.

---

## Files Requiring Changes

### 1. `src/main/java/frc/robot/Constants.java`

**Lines 7–9** — Unused/incorrect imports that reference REV classes not used anywhere in the file body:

```java
// REMOVE these imports:
import com.revrobotics.spark.FeedbackSensor;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkFlexConfig;
```

**Changes needed:**
- Remove all three `com.revrobotics.*` imports — they are unused in this file.
- The `DriveConstants` inner class (CAN IDs 1–4 for left/right leader/follower) was for the old tank-drive and is **no longer used** since the drivetrain is now swerve via `TunerConstants`. Consider removing `DriveConstants` entirely or repurposing it for documentation only.
- The `FuelConstants` and `ClimbConstatns` (note: typo — "Constatns") CAN IDs and percent values are still valid and should be kept, but the motor type comments referencing "brushed" or "brushless" should be updated to reflect Talon FX (brushless only).

---

### 2. `src/main/java/frc/robot/subsystems/CANFuelSubsystem.java`

This is the most heavily SparkMax-dependent file. All motor controller code must be replaced.

**Imports to remove (lines 7–14):**
```java
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkMax;
import edu.wpi.first.wpilibj.motorcontrol.Spark; // also unused
```

**Imports to add:**
```java
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.InvertedValue;
```
```java
// REMOVE:
private final SparkMax LeftIntakeLauncher;
private final SparkMax RightIntakeLauncher;
private final SparkMax Indexer;

// REPLACE WITH:
private final TalonFX leftIntakeLauncher;
private final TalonFX rightIntakeLauncher;
private final TalonFX indexer;
private final DutyCycleOut dutyCycleRequest = new DutyCycleOut(0);
```

**Constructor to replace (lines 31–62):**

The SparkMax constructor takes a `MotorType` (brushed vs. brushless). Talon FX only supports brushless (Falcon 500 / Kraken X60) so `MotorType` is not needed.

The SparkMax configuration pattern (`SparkMaxConfig` → `.configure(config, ResetMode, PersistMode)`) must be replaced with CTRE's `TalonFXConfiguration` applied via `motor.getConfigurator().apply(config)`.

Key config mappings:
| SparkMax API | Talon FX Equivalent |
|---|---|
| `SparkMaxConfig.smartCurrentLimit(amps)` | `config.CurrentLimits.SupplyCurrentLimit = amps` + `config.CurrentLimits.SupplyCurrentLimitEnable = true` |
| `SparkMaxConfig.idleMode(IdleMode.kCoast)` | `config.MotorOutput.NeutralMode = NeutralModeValue.Coast` |
| `SparkMaxConfig.idleMode(IdleMode.kBrake)` | `config.MotorOutput.NeutralMode = NeutralModeValue.Brake` |
| `SparkMaxConfig.voltageCompensation(12)` | Not directly needed; Talon FX uses `DutyCycleOut` (% of bus voltage) or `VoltageOut` control requests |
| `SparkMaxConfig.inverted(true)` | `config.MotorOutput.Inverted = InvertedValue.Clockwise_Positive` |
| `motor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters)` | `motor.getConfigurator().apply(config)` |

**Methods to replace (lines 64–83):**

The `.set(power)` method on SparkMax must be replaced with a control request:
```java
// REMOVE (SparkMax):
LeftIntakeLauncher.set(power);

// REPLACE WITH (Talon FX):
leftIntakeLauncher.setControl(dutyCycleRequest.withOutput(power));
```

> ⚠️ Note: The original code uses `Indexer` typed as brushed (`MotorType.kBrushed`). Talon FX **does not support brushed motors**. If a brushed motor is being used for the indexer, a different controller (e.g., a Spark MAX or Talon SRX) must remain for that motor, or the indexer motor must be swapped to a brushless motor compatible with Talon FX (Falcon 500 or Kraken X60).

---

### 3. `src/main/java/frc/robot/subsystems/ClimberSubsystem.java`

**Imports to remove (lines 3–8):**
```java
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.SparkMax;
```

**Imports to add:**
```java
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.NeutralModeValue;
```

**Field to replace (line 15):**
```java
// REMOVE:
private final SparkMax climberMotor;

// REPLACE WITH:
private final TalonFX climberMotor;
private final DutyCycleOut dutyCycleRequest = new DutyCycleOut(0);
```

**Constructor to replace (lines 20–28):**
```java
// REMOVE:
climberMotor = new SparkMax(CLIMBER_MOTOR_ID, MotorType.kBrushed);
SparkMaxConfig climbConfig = new SparkMaxConfig();
climbConfig.smartCurrentLimit(CLIMBER_MOTOR_CURRENT_LIMIT);
climbConfig.idleMode(IdleMode.kBrake);
climberMotor.configure(climbConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

// REPLACE WITH:
climberMotor = new TalonFX(CLIMBER_MOTOR_ID);
TalonFXConfiguration climbConfig = new TalonFXConfiguration();
climbConfig.CurrentLimits.SupplyCurrentLimit = CLIMBER_MOTOR_CURRENT_LIMIT;
climbConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
climbConfig.MotorOutput.NeutralMode = NeutralModeValue.Brake;
climberMotor.getConfigurator().apply(climbConfig);
```

**Methods to replace:**
```java
// REMOVE:
climberMotor.set(power);

// REPLACE WITH:
climberMotor.setControl(dutyCycleRequest.withOutput(power));
```

> ⚠️ Same brushed motor caveat as the indexer above — the climber motor was configured as `MotorType.kBrushed`. If this is a brushed motor, it **cannot** be driven by a Talon FX. Confirm the physical motor type before converting.

---

### 4. `src/main/java/frc/robot/RobotContainer.java`

The current `RobotContainer.java` was generated by Tuner X and only wires up swerve drive. The Everybot's fuel and climber subsystems are not yet instantiated or bound to any controls. The following additions are needed — **the existing swerve driving block must not be touched**.

**Fields to add** (after the `drivetrain` field declaration):
```java
// Operator controller (port 1) — fuel mechanism and climber
private final CommandXboxController operatorController = new CommandXboxController(1);

// Mechanism subsystems
private final CANFuelSubsystem fuelSubsystem = new CANFuelSubsystem();
private final ClimberSubsystem climberSubsystem = new ClimberSubsystem();
```

**Imports to add:**
```java
import frc.robot.commands.ClimbDown;
import frc.robot.commands.ClimbUp;
import frc.robot.commands.Eject;
import frc.robot.commands.Intake;
import frc.robot.commands.LaunchSequence;
import frc.robot.subsystems.CANFuelSubsystem;
import frc.robot.subsystems.ClimberSubsystem;
```

**Bindings to add** at the end of `configureBindings()`, after the existing swerve bindings:
```java
// --- Operator controller bindings (fuel mechanism & climber) ---

// While left bumper is held, run the intake
operatorController.leftBumper().whileTrue(new Intake(fuelSubsystem));
// While right bumper is held, spin up then launch fuel
operatorController.rightBumper().whileTrue(new LaunchSequence(fuelSubsystem));
// While A is held, eject fuel back out the intake
operatorController.a().whileTrue(new Eject(fuelSubsystem));
// While D-pad down is held, lower the climber
operatorController.povDown().whileTrue(new ClimbDown(climberSubsystem));
// While D-pad up is held, raise the climber
operatorController.povUp().whileTrue(new ClimbUp(climberSubsystem));

// Default commands — stop mechanisms when no button is held
fuelSubsystem.setDefaultCommand(fuelSubsystem.run(() -> fuelSubsystem.stop()));
climberSubsystem.setDefaultCommand(climberSubsystem.run(() -> climberSubsystem.stop()));
```

> **Note:** The Everybot's `EverybotRobotContainer.java` mapped fuel/climb controls to the *driver* controller (port 0). We intentionally move them to a *separate operator controller* (port 1) to avoid conflicts with the existing swerve bindings on the driver controller (`a()`, `b()`, `leftBumper()`, etc.).

> **Note:** The Everybot's `Drive` command and `driveSubsystem.setDefaultCommand(...)` are **not** ported — swerve driving is already handled by TunerX-generated code and must remain untouched.

---

### 5. `src/main/java/frc/robot/commands/Drive.java` *(Dead code / broken reference)*

**Line 8 & 11:**
```java
import frc.robot.subsystems.CANDriveSubsystem;
// ...
CANDriveSubsystem driveSubsystem;
```

The class `CANDriveSubsystem` **does not exist** in this codebase — it was part of the original Everybot tank-drive subsystem that was not imported. Since the robot now uses swerve drive (`CommandSwerveDrivetrain`), this entire command file is **obsolete** and should be deleted. Driving is now handled directly in `RobotContainer` via `SwerveRequest`.

**Recommended action:** Delete `Drive.java`.

---

### 6. `src/main/java/frc/robot/commands/AutoDrive.java` *(Dead code / broken reference)*

**Lines 8, 13, 16:**
```java
import frc.robot.subsystems.CANDriveSubsystem;
// ...
CANDriveSubsystem driveSubsystem;
// ...
public AutoDrive(CANDriveSubsystem driveSystem, double xSpeed, double zRotation)
```

Same issue as `Drive.java` — references the non-existent `CANDriveSubsystem`. This file also calls `driveSubsystem.driveArcade(...)` which is a WPILib `DifferentialDrive` method and has no equivalent in swerve.

`ExampleAuto.java` depends on `AutoDrive` but the `RobotContainer.getAutonomousCommand()` has already been rewritten to use `SwerveRequest` directly, making `ExampleAuto` also redundant.

**Recommended action:** Delete `AutoDrive.java` and `ExampleAuto.java`.

---

### 7. `src/main/java/frc/robot/Constants.java` — `DriveConstants` inner class *(Orphaned)*

```java
public static final class DriveConstants {
    public static final int LEFT_LEADER_ID = 1;
    public static final int LEFT_FOLLOWER_ID = 3;
    public static final int RIGHT_LEADER_ID = 2;
    public static final int RIGHT_FOLLOWER_ID = 4;
    public static final int DRIVE_MOTOR_CURRENT_LIMIT = 60;
}
```

These CAN IDs were for the original tank-drive. Swerve drive motor IDs are defined in `generated/TunerConstants.java`. This class is now orphaned.

**Recommended action:** Remove `DriveConstants` from `Constants.java` (verify no remaining references first).

---

### 8. `src/main/java/frc/robot/Constants.java` — Typo in `ClimbConstatns`

The class `ClimbConstatns` (line ~65) has a typo — it should be `ClimbConstants`. This is a low-priority cleanup but should be corrected alongside the motor conversion to avoid confusion.

---

## Conversion Checklist

- [ ] **Verify CAN IDs** — once Talon FX devices are physically installed and configured via Tuner X, update `Constants.java` fuel and climber IDs to match; confirm no two devices on the bus share an ID (swerve device IDs are listed above for reference)
- [ ] Confirm physical motor types for **Indexer** (currently wired as brushed in code)
- [ ] Confirm physical motor type for **Climber** (currently wired as brushed in code)
- [ ] Add REV Robotics vendordep **only** if any brushed motor must remain on SparkMax; otherwise remove all REV references
- [ ] Replace all `com.revrobotics.*` imports in `CANFuelSubsystem.java`
- [ ] Replace all `com.revrobotics.*` imports in `ClimberSubsystem.java`
- [ ] Remove unused `com.revrobotics.*` imports from `Constants.java`
- [ ] Replace `SparkMax` instances with `TalonFX` in `CANFuelSubsystem.java`
- [ ] Replace `SparkMax` instance with `TalonFX` in `ClimberSubsystem.java`
- [ ] Replace `SparkMaxConfig` configurations with `TalonFXConfiguration` in both subsystems
- [ ] Replace `.set(power)` calls with `.setControl(dutyCycleRequest.withOutput(power))` in both subsystems
- [ ] Add `CANFuelSubsystem` and `ClimberSubsystem` fields to `RobotContainer.java`
- [ ] Add operator controller (port 1) to `RobotContainer.java`
- [ ] Add fuel and climber button bindings and default commands to `configureBindings()` in `RobotContainer.java`
- [ ] Delete `Drive.java` (references non-existent `CANDriveSubsystem`)
- [ ] Delete `AutoDrive.java` (references non-existent `CANDriveSubsystem`)
- [ ] Delete `ExampleAuto.java` (depends on deleted `AutoDrive.java`)
- [ ] Remove `DriveConstants` class from `Constants.java`
- [ ] Fix typo: `ClimbConstatns` → `ClimbConstants` (update all references)
- [ ] Test build after all changes

---

## Phoenix 6 API Quick Reference

**Import package root:** `com.ctre.phoenix6`

| Need | Class/Method |
|---|---|
| Motor controller | `com.ctre.phoenix6.hardware.TalonFX` |
| Duty cycle (% output) control | `com.ctre.phoenix6.controls.DutyCycleOut` |
| Voltage output control | `com.ctre.phoenix6.controls.VoltageOut` |
| Motor configuration | `com.ctre.phoenix6.configs.TalonFXConfiguration` |
| Apply configuration | `motor.getConfigurator().apply(config)` |
| Neutral mode (brake/coast) | `config.MotorOutput.NeutralMode = NeutralModeValue.Brake` |
| Supply current limit | `config.CurrentLimits.SupplyCurrentLimit = amps` |
| Invert direction | `config.MotorOutput.Inverted = InvertedValue.Clockwise_Positive` |
| Import for invert | `import com.ctre.phoenix6.signals.InvertedValue` |
| Stop motor | `motor.setControl(new DutyCycleOut(0))` or `motor.stopMotor()` |

The Phoenix 6 vendordep (`Phoenix6-26.1.0.json`) is already present in the project — no additional vendordep installation is required for Talon FX support.
