copilot-instructions.md

## Code Quality Principles

<!-- https://github.com/mieweb/template-mieweb-opensource/blob/main/.github/copilot-instructions.md -->

### 🎯 DRY (Don't Repeat Yourself)
- **Never duplicate code**: If you find yourself copying code, extract it into a reusable function
- **Single source of truth**: Each piece of knowledge should have one authoritative representation
- **Refactor mercilessly**: When you see duplication, eliminate it immediately
- **Shared utilities**: Common patterns should be abstracted into utility functions

### 💋 KISS (Keep It Simple, Stupid)
- **Simple solutions**: Prefer the simplest solution that works
- **Avoid over-engineering**: Don't add complexity for hypothetical future needs
- **Clear naming**: Functions and variables should be self-documenting
- **Small functions**: Break down complex functions into smaller, focused ones
- **Readable code**: Code should be obvious to understand at first glance

### 🧹 Folder Philosophy
- **Clear purpose**: Every folder should have a main thing that anchors its contents.
- **No junk drawers**: Don’t leave loose files without context or explanation.
- **Explain relationships**: If it’s not elegantly obvious how files fit together, add a README or note.
- **Immediate clarity**: Opening a folder should make its organizing principle clear at a glance.

### 🔄 Refactoring Guidelines
- **Continuous improvement**: Refactor as you work, not as a separate task
- **Safe refactoring**: Always run tests before and after refactoring
- **Incremental changes**: Make small, safe changes rather than large rewrites
- **Preserve behavior**: Refactoring should not change external behavior
- **Code reviews**: All refactoring should be reviewed for correctness

### ⚰️ Dead Code Management
- **Immediate removal**: Delete unused code immediately when identified
- **Historical preservation**: Move significant dead code to `.attic/` directory with context
- **Documentation**: Include comments explaining why code was moved to attic
- **Regular cleanup**: Review and clean attic directory periodically
- **No accumulation**: Don't let dead code accumulate in active codebase

## Documentation Preferences

### Diagrams and Visual Documentation
- **Always use Mermaid diagrams** instead of ASCII art for workflow diagrams, architecture diagrams, and flowcharts
- **Use memorable names** instead of single letters in diagrams (e.g., `Engine`, `Auth`, `Server` instead of `A`, `B`, `C`)
- Use appropriate Mermaid diagram types:
  - `graph TB` or `graph LR` for workflow architectures 
  - `flowchart TD` for process flows
  - `sequenceDiagram` for API interactions
  - `gitgraph` for branch/release strategies
- Include styling with `classDef` for better visual hierarchy
- Add descriptive comments and emojis sparingly for clarity

### Documentation Standards
- **`README.md` is the single source of truth**: Every code change that affects subsystems, commands, controller bindings, autonomous behavior, constants, or vendor dependencies **must** be reflected in `README.md` as part of the same change — never defer it.
- Keep documentation DRY (Don't Repeat Yourself) - reference other docs instead of duplicating
- Use clear cross-references between related documentation files
- Update the main architecture document when workflow structure changes

#### `// README: <Section>` convention
Source files use inline comments to mark exactly which README section must be updated when that code changes:
```java
// README: Subsystems > CANFuelSubsystem
// README: Controller Bindings > Operator Controller
// README: Autonomous
// README: Vendor Libraries
```
When you touch a line annotated with `// README: ...`, update that section in `README.md` in the same commit.

## Working with GitHub Actions Workflows

### Development Philosophy
- **Script-first approach**: All workflows should call scripts that can be run locally
- **Local development parity**: Developers should be able to run the exact same commands locally as CI runs
- **Simple workflows**: GitHub Actions should be thin wrappers around scripts, not contain complex logic
- **Easy debugging**: When CI fails, developers can reproduce the issue locally by running the same script

## FRC Robot Development Patterns

### Command-Based Architecture
- Every subsystem is a SubsystemBase with command interface
- Commands are one-purpose actions bound to buttons/triggers
- Default commands enforce motor safety (stop when not commanded)
- Use SequentialCommandGroup for multi-step sequences (e.g., SpinUp → Launch)

### Motor Configuration Pattern
1. Define all config in Constants.java as static instances
2. Apply at subsystem instantiation (TalonFX: .getConfigurator().apply(); SparkMax: .configure())
3. Inversion defined at config time, never toggle in control logic
4. Current limits tuned to prevent brownout while protecting motors

### Autonomous (ChoreoLib)
- Trajectories designed in Choreo desktop app, saved to src/main/deploy/choreo/
- ChoreoLib auto-deploys files to RoboRIO
- AutoFactory wired to drivetrain; followTrajectory applies feedforward + pose PID
- Add new autos: choreoFactory.trajectoryCmd("Name") → autoChooser.addOption()

### SmartDashboard Tuning Workflow
1. Subsystems expose default constants to SmartDashboard on init
2. Commands read live values from dashboard in initialize()
3. After field testing, copy tuned values back to Constants.java
4. Do not commit dashboard-only tunings; sync Constants.java instead

### Console Control Access
- Driver Controller (Port 0): Left/Right sticks + bumpers (drive + SysId)
- Operator Controller (Port 1): Bumpers (fuel actions) + POV (climb/speed adjust)
- Define all ports in OperatorConstants; use CommandXboxController for binding

### Swerve-Specific Notes
- TunerConstants.java is auto-generated; regenerate with Tuner X, never hand-edit
- Open-loop voltage for joystick drive; PID only for Choreo path correction
- Max speed: Configured as fraction of kSpeedAt12Volts (currently 100% = ~5.12 m/s)
- Orientation: Field-centric default (can RobotCentric for testing)

### Vendor Library Constraints
- Phoenix 6: All configurations must match SwerveModuleConstants expectations
- REVLib: SparkMax brushed motors require MotorType.kBrushed; coast/brake per use case
- ChoreoLib: Always design paths in desktop app; do not hand-edit trajectory JSON
- AdvantageKit: All periods logged; use Logger.recordMetadata() for session identification

## Quick Reference — 2026-EveryBot Subsystem Integration

### To Add a New Subsystem
1. Create SubsystemBase in src/main/java/frc/robot/subsystems/
2. Add motor/config constants to Constants.java inner class
3. Create commands for each action in commands/
4. Instantiate subsystem in RobotContainer; wire button bindings
5. Update README.md subsystem table and controller bindings table
6. Add // README: <Section> comments to mark docs sync points

### Troubleshooting Checklist
- [ ] Motor inverted unexpectedly? Check TalonFXConfiguration.MotorOutput.Inverted
- [ ] Motor not stopping? Verify default command calls stop() in subsystem
- [ ] Swerve drive jerky? Check odometry frequency; verify SwerveModuleConstants match TunerConstants
- [ ] SmartDashboard values not applied? Commands must read in initialize(), not execute()
- [ ] Deployment failed? Run ./gradlew build first; check team number in .wpilib/wpilib_preferences.json
- [ ] Choreography path not deploying? Ensure src/main/deploy/choreo/ contains .traj file; rebuild and redeploy

## Quick Reference

### 🪶 All Changes should be considered for Pull Request Philosophy

* **Smallest viable change**: Always make the smallest change that fully solves the problem.
* **Fewest files first**: Start with the minimal number of files required.
* **No sweeping edits**: Broad refactors or multi-module changes must be split or proposed as new components.
* **Isolated improvements**: If a change grows complex, extract it into a new function, module, or component instead of modifying multiple areas.
* **Direct requests only**: Large refactors or architectural shifts should only occur when explicitly requested.
 
### Code Quality Checklist
- [ ] **DRY**: No code duplication - extracted reusable functions?
- [ ] **KISS**: Simplest solution that works?
- [ ] **Minimal Changes**: Smallest viable change made for PR?
- [ ] **Naming**: Self-documenting function/variable names?
- [ ] **Size**: Functions small and focused?
- [ ] **Dead Code**: Removed or archived appropriately?
- [ ] **README**: `README.md` updated to reflect any changes to subsystems, commands, bindings, auto, constants, or deps?
- [ ] **Test**: Run tests