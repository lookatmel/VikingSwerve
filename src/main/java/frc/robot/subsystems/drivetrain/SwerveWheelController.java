package frc.robot.subsystems.drivetrain;

import frc.robot.output.commands.drivetrain.TeleopDrive;
import frc.robot.subsystems.drivetrain.SwerveWheel;

import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.command.Subsystem;

import frc.team6854.controllers.SwerveWheelDrive;
import frc.team6854.controllers.SwerveWheelDrive.SwerveWheelDriveType;

import com.kauailabs.navx.frc.AHRS;

public class SwerveWheelController extends Subsystem implements SwerveDrivetrainConstants {

    private static SwerveWheelController instance = null;

    private SwerveWheelDrive frontRightDrive = new SwerveWheelDrive(SwerveWheelDriveType.TalonSRX, frontRightDriveID, true);
    private SwerveWheelDrive frontLeftDrive = new SwerveWheelDrive(SwerveWheelDriveType.VictorSPX, frontLeftDriveID, false);
    private SwerveWheelDrive backRightDrive = new SwerveWheelDrive(SwerveWheelDriveType.VictorSPX, backRightDriveID, true);
    private SwerveWheelDrive backLeftDrive = new SwerveWheelDrive(SwerveWheelDriveType.Spark, backLeftDriveID, false);
    
    private SwerveWheel frontRight = new SwerveWheel(frontRightDrive, frontRightTurnTalonID, frontRightEncoderID, frontRightEncoderOffset, "Front Right");
    private SwerveWheel frontLeft = new SwerveWheel(frontLeftDrive, frontLeftTurnTalonID, frontLeftEncoderID, frontLeftEncoderOffset, "Front Left");
    private SwerveWheel backRight = new SwerveWheel(backRightDrive, backRightTurnTalonID, backRightEncoderID, backRightEncoderOffset, "Back Right");
    private SwerveWheel backLeft = new SwerveWheel(backLeftDrive, backLeftTurnTalonID, backLeftEncoderID, backLeftEncoderOffset, "Back Left");

    private AHRS gyro;

    // Get distance between wheels
    private double r = Math.sqrt((L * L) + (W * W));

    private boolean isFieldCentric = true;
    private boolean gyroEnabled = false;
    
    private SwerveWheelController(){
        try {
            gyro = new AHRS(SPI.Port.kMXP); 
            gyroEnabled = true;
        } catch (RuntimeException ex ) {
            System.out.println("--------------");
            System.out.println("NavX not plugged in");
            System.out.println("--------------");
            gyroEnabled = false;
        }
    }

    // x1 = strafe, y1 = speed, x2 = rotation 
    // Holonomic drive
    public void drive(double x1, double y1, double x2, double gyroValue) {

        y1 *= -1;

        // Calculate magnitude of joystick
        double magnitude = Math.sqrt((Math.pow(x1, 2)) + (Math.pow(y1,2)));

        if (magnitude >= 0.15) {

            // I got this bit of code from the NavX website
            if (isFieldCentric == true && gyroEnabled == true) {
                // Convert gyro angle to radians
                double gyro = gyroValue * Math.PI / 180;

                double temp = x1 * Math.cos(gyro) + y1 * Math.sin(gyro); 
                y1 = -x1 * Math.sin(gyro) + y1 * Math.cos(gyro); 
                x1 = temp;
            }

            // -------------------------------------
            // Do the swerve wheel math for speed and angles
            // -------------------------------------
            double a = x1 - x2 * (L / r);
            double b = x1 + x2 * (L / r);
            double c = y1 - x2 * (W / r);
            double d = y1 + x2 * (W / r);

            double frontLeftSpeed = Math.sqrt((b * b) + (c * c));
            double frontRightSpeed = Math.sqrt((b * b) + (d * d));
            double backRightSpeed = Math.sqrt((a * a) + (d * d));
            double backLeftSpeed = Math.sqrt((a * a) + (c * c));

            double backRightAngle = Math.atan2(a, d) * 180 / Math.PI;
            double backLeftAngle = Math.atan2(a, c) * 180 / Math.PI;
            double frontRightAngle = Math.atan2(b, d) * 180 / Math.PI;
            double frontLeftAngle = Math.atan2(b, c) * 180 / Math.PI ;
            // -------------------------------------

            // -------------------------------------
            // This bit of code normalizes the speed
            // -------------------------------------
            double max = frontLeftSpeed;
            max = Math.max(max, frontRightSpeed);
            max = Math.max(max, backRightSpeed);
            max = Math.max(max, backLeftSpeed);

            if (max > 1) {
                frontLeftSpeed /= max;
                frontRightSpeed /= max;
                backRightSpeed /= max;
                backLeftSpeed /= max;
            }
            // -------------------------------------

            frontRight.setSetpoint(frontRightAngle);
            frontLeft.setSetpoint(frontLeftAngle);
            backRight.setSetpoint(backRightAngle);
            backLeft.setSetpoint(backLeftAngle);

            frontLeft.setSpeed(frontLeftSpeed);
            frontRight.setSpeed(frontRightSpeed);
            backRight.setSpeed(backRightSpeed);
            backLeft.setSpeed(backLeftSpeed);
        } 
        else {
            frontLeft.setSpeed(0);
            frontRight.setSpeed(0);
            backRight.setSpeed(0);
            backLeft.setSpeed(0);
        }
    }

    // Zero the Gryo
    public void resetGyro() {
        gyro.reset();
    }

    // Get the Gyro Angle (-180 to 180)
    public double gyroAngle() {
        return gyro.getYaw();
    }

    // Set the controller to be field centric
    public void setFieldCentric(boolean value) {
        isFieldCentric = value;
    }

    public void zero() {
        frontRight.setSetpoint(0);
        frontLeft.setSetpoint(0);
        backRight.setSetpoint(0);
        backLeft.setSetpoint(0);

        frontLeft.setSpeed(0);
        frontRight.setSpeed(0);
        backRight.setSpeed(0);
        backLeft.setSpeed(0);
    }

    /**
     * @return the instance
     */
    public static SwerveWheelController getInstance() {
        if (instance == null) {
            instance = new SwerveWheelController();
        }

        return instance;
    }

    // Set the default command to be normal driving
    @Override
    protected void initDefaultCommand() {
        setDefaultCommand(new TeleopDrive());
    }
}
