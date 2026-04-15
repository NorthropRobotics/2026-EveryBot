package frc.robot.subsystems;

import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.hardware.Pigeon2;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructPublisher;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Util.LimelightHelpers;

@SuppressWarnings("unused")
public class Vision extends SubsystemBase {
    private final CommandSwerveDrivetrain drivetrain;

    private final NetworkTable visionTable = NetworkTableInstance.getDefault().getTable("Vision");
    private final StructPublisher<Pose2d> shootCamPosePub = visionTable.getStructTopic("shootCamPose", Pose2d.struct)
            .publish();

    public Vision(CommandSwerveDrivetrain drivetrain) {
        this.drivetrain = drivetrain;
    }

    @Override
    public void periodic() {
        boolean rotatingTooFast = Math.abs(drivetrain.getState().Speeds.omegaRadiansPerSecond) > 10;
        if (!rotatingTooFast) {
            Pigeon2 gyro = drivetrain.getPigeon2();
            double yaw = gyro.getYaw().getValueAsDouble();
            double pitch = gyro.getPitch().getValueAsDouble();
            double roll = gyro.getRoll().getValueAsDouble();

            LimelightHelpers.SetRobotOrientation("limelight",
                    yaw, 0.0, pitch, 0.0, roll, 0.0);

            LimelightHelpers.PoseEstimate shootCamEstimate = LimelightHelpers
                    .getBotPoseEstimate_wpiBlue_MegaTag2("limelight");

                shootCamPosePub.set(shootCamEstimate.pose);
                SignalLogger.writeStruct("Vision/shootCamPose", Pose2d.struct, shootCamEstimate.pose);
           
            if(shootCamEstimate.tagCount == 0) return;
            drivetrain.addVisionMeasurement(useGyroRotation(shootCamEstimate.pose), Timer.getFPGATimestamp());
            


        }
    }

    public Pose2d useGyroRotation(Pose2d pose) {
        return new Pose2d(pose.getTranslation(), drivetrain.getPigeon2().getRotation2d());
    }
}
