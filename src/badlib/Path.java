package badlib;

/**
 * This class takes in specific inputs for how the robot should be moving over a period of time
 * and creates a path from that information. You can query this class's methods to find different pieces of 
 * information about the path at a given time.
 * 
 * @author andrew
 *
 */
public class Path {
	
	public static double ANGLE_MAX_ACCEL = Math.PI*2; //radians
	public static double POSITION_MAX_ACCEL = 50; //inches
	public static double ROBOT_RADIUS = 1;

	private PathData pathData;
	private double[] o;
	private Point[] p;
	
	/**
	 * Creates a path, if possible, with these given parameters.
	 * Will use {@link #ANGLE_MAX_ACCEL} and {@link #POSITION_MAX_ACCEL}, or smaller for 
	 * linear and angular acceleration.
	 * 
	 * @param omega1 starting target angular velocity
	 * @param omega2 ending target angular velocity
	 * @param middleTime time to go from omega1 to omega2
	 * @param startSpeed starting linear speed
	 * @param wantedSpeed wanted linear speed at max
	 * @param endSpeed wanted linear speed once finished
	 * @param omegaStartCoast Delay time before angle begins changing when path starts
	 * @param omegaMiddleCoast Wait time when omega is 0 (if it is 0) around the middle of the path
	 * @param omegaEndCoast Wait time when omega is 0 at the end of the path before the path finishes
	 * @param speedStartCoast Delay time before speed starts changing when path starts
	 * @param speedEndCoast Wait time at end of speed path
	 * @param deltaTheta total change in theta from point A to B 
	 */
	public Path(double omega1, double omega2, double middleTime, double startSpeed, 
			double wantedSpeed, double endSpeed, double omegaStartCoast, 
			double omegaMiddleCoast, double omegaEndCoast, double speedStartCoast, 
			double speedEndCoast, double deltaTheta
	) {
		if (deltaTheta * omega1 < 0) {
			System.out.println("You're trying to accelerate in the wrong direction!");
			throw new IllegalArgumentException();
		}
		
		double tA = Math.abs(ANGLE_MAX_ACCEL) * Math.signum(omega1);
		if (tA == 0) {
			tA = Math.abs(ANGLE_MAX_ACCEL);
		}
		
		double A = omega1/tA;
		double B = middleTime;
		double C = ( 2*deltaTheta - A*omega1 - B*(omega1+omega2) ) / omega2;
		
		double tD = -omega2/C;
		if (Math.abs(tD) > Math.abs(tA)+0.001 || C < 0) {
			System.out.println("Trying to decelerate too fast!");
			throw new IllegalArgumentException();
		}
		double tM = (omega2-omega1)/B;
		if (Math.abs(tM) > Math.abs(tA)+0.001) {
			System.out.println("Trying to change directions too fast!");
			throw new IllegalArgumentException();
		}
		
		double D = -omega1 / tM;
		if (D > B) {
			D = 0;
			omegaMiddleCoast = 0;
		}
		double T = A + B + C + omegaStartCoast + omegaMiddleCoast + omegaEndCoast;
		
		double pA = Math.abs(POSITION_MAX_ACCEL) * Math.signum(wantedSpeed - startSpeed);
		double pD = Math.abs(POSITION_MAX_ACCEL) * Math.signum(endSpeed - wantedSpeed);
		if (pA == 0) {
			pA = Math.abs(POSITION_MAX_ACCEL);
		} else if (pD == 0) {
			pD = Math.abs(POSITION_MAX_ACCEL);
		}
		
		double Q = (wantedSpeed - startSpeed)/pA;
		double S = (endSpeed - wantedSpeed)/pD;
		double R = T - Q - S - speedStartCoast - speedEndCoast;
		
		if (R < 0) {
			System.out.println("Can't reach wantedSpeed in time!");
			throw new IllegalArgumentException();
		}
		
		pathData = new PathData(
				startSpeed,
				new Period(omegaStartCoast, 0, true),
				new Period(speedStartCoast, 0, false),
				new Period(A, tA, true),
				new Period(D, tM, true),
				new Period(omegaMiddleCoast, 0, true),
				new Period(B-D, tM, true),
				new Period(C, tD, true),
				new Period(Q, pA, false),
				new Period(R, 0, false),
				new Period(S, pD, false),
				new Period(speedEndCoast, 0, false),
				new Period(omegaEndCoast, 0, true)
			);
		
		this.o = new double[pathData.t.length];
		this.p = new Point[pathData.t.length];
		this.p[0] = new Point();		
		
		for (int i = 1; i < this.o.length; i++) {
			this.p[i] = new Point();
			
			this.o[i] = PathMath.offset(pathData.tA[i], pathData.omega(i, pathData.t[i])) - pathData.angle(i, pathData.t[i]);
			this.position(i-1, pathData.t[i], this.p[i]);
		}
	}
	
	/**
	 * Creates a path, if possible, with these given parameters.
	 * Will use {@link #ANGLE_MAX_ACCEL} and {@link #POSITION_MAX_ACCEL}, or smaller for 
	 * linear and angular acceleration.
	 * 
	 * @param omega1 starting target angular velocity
	 * @param omega2 ending target angular velocity
	 * @param middleTime time to go from omega1 to omega2
	 * @param startSpeed starting linear speed
	 * @param wantedSpeed wanted linear speed at max
	 * @param endSpeed wanted linear speed once finished
	 * @param deltaTheta total change in theta from point A to B 
	 */
	public Path(double omega1, double omega2, double middleTime, double startSpeed, double wantedSpeed, double endSpeed, double deltaTheta) {
		this(omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, 0, 0, 0, 0, 0, deltaTheta);
	}
	
	public Point omega1Limits(double omega2, double middleTime, double startSpeed, double wantedSpeed, double endSpeed, double deltaTheta) {
		return null;
	}
	
	/**
	 * Calculates whether given parameters will create a valid path.
	 * 
	 * @param omega1 starting target angular velocity
	 * @param omega2 ending target angular velocity
	 * @param middleTime time to go from omega1 to omega2
	 * @param startSpeed starting linear speed
	 * @param wantedSpeed wanted linear speed at max
	 * @param endSpeed wanted linear speed once finished
	 * @param deltaTheta total change in theta from point A to B 
	 * @return True if the path will be valid
	 */
	public static boolean validParameters(double omega1, double omega2, double middleTime, double startSpeed, double wantedSpeed, double endSpeed, double deltaTheta) {
		if (deltaTheta * omega1 < 0) {
			System.out.println("You're trying to accelerate in the wrong direction!");
			return false;
		}
		
		double tA = Math.abs(ANGLE_MAX_ACCEL) * Math.signum(omega1);
		if (tA == 0) {
			tA = Math.abs(ANGLE_MAX_ACCEL);
		}
		
		double A = omega1/tA;
		double B = middleTime;
		double C = ( 2*deltaTheta - A*omega1 - B*(omega1+omega2) ) / omega2;
		
		double tD = -omega2/C;
		if (Math.abs(tD) > Math.abs(tA)+0.001 || C < 0) {
			System.out.println("Trying to decelerate too fast!");
			return false;
		}
		double tM = (omega2-omega1)/B;
		if (Math.abs(tM) > Math.abs(tA)+0.001) {
			System.out.println("Trying to change directions too fast!");
			return false;
		}
				
		double pA = Math.abs(POSITION_MAX_ACCEL) * Math.signum(wantedSpeed - startSpeed);
		double pD = Math.abs(POSITION_MAX_ACCEL) * Math.signum(endSpeed - wantedSpeed);
		if (pA == 0) {
			pA = Math.abs(POSITION_MAX_ACCEL);
		} else if (pD == 0) {
			pD = Math.abs(POSITION_MAX_ACCEL);
		}
		
		double Q = (wantedSpeed - startSpeed)/pA;
		double S = (endSpeed - wantedSpeed)/pD;
		double R = A + B + C - Q - S;
		
		if (R < 0) {
			System.out.println("Can't reach wantedSpeed in time!");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns angle of the robot as a function of time
	 * 
	 * @param t
	 * @return Angle in radians
	 */
	public double angle(double t) {
		return pathData.angle(pathData.indexForTime(t), t);
	}
	
	/**
	 * Returns angular velocity of the robot as a function of time
	 * 
	 * @param t
	 * @return Omega (radians)
	 */
	public double omega(double t) {
		return pathData.omega(pathData.indexForTime(t), t);
	}
	
	/**
	 * Returns angular acceleration of the robot as a function of time
	 * 
	 * @param t
	 * @return Alpha (radians)
	 */
	public double angularAcceleration(double t) {
		return pathData.tA[pathData.indexForTime(t)];
	}
	
	/**
	 * Returns speed in the direction the robot is facing as a function of time
	 * 
	 * @param t
	 * @return Speed
	 */
	public double speed(double t) {
		return pathData.speed(pathData.indexForTime(t), t);
	}
	
	/**
	 * Returns acceleration in the direction the robot is facing as a function of time
	 * 
	 * @param t
	 * @return Acceleration
	 */
	public double linearAcceleration(double t) {
		return pathData.pA[pathData.indexForTime(t)];
	}
		
	/**
	 * Finds the (x, y) position of the robot at time t
	 *
	 * @param t
	 * @param dest The {@link Point} to put the coordinates into.
	 */
	public void position(double t, Point dest) {
		int index = pathData.indexForTime(t);
		position(index, t, dest);
	}
	
	private void position(int index, double t, Point dest) {		
		if (t > pathData.T) {
			t = pathData.T;
		}
		
		PathMath.integrate(
				t - pathData.t[index], 
				pathData.tA[index], 
				pathData.omega(index, pathData.t[index]), 
				pathData.pA[index], 
				pathData.speed(index, pathData.t[index]), 
				dest
		);
		PathMath.rotatePoint(
				dest.x, 
				dest.y, 
				this.o[index], 
				dest
		);
		
		dest.x += p[index].x;
		dest.y += p[index].y;
	}
	
	/**
	 * Finds the position of each wheel as a function of time. 
	 * 
	 * @param t
	 * @param left Left wheel's (x, y) position as a {@link Point}
	 * @param right Right wheel's (x, y) position as a {@link Point}
	 */
	public void wheelPositions(double t, Point left, Point right) {
		double angle = this.angle(t) - Math.PI/2;
		position(t, left);
		
		right.x = left.x + ROBOT_RADIUS * Math.cos(angle);
		right.y = left.y + ROBOT_RADIUS * Math.sin(angle);
		
		angle += Math.PI;
		left.x += ROBOT_RADIUS * Math.cos(angle);
		left.y += ROBOT_RADIUS * Math.sin(angle);
	}
	
	/**
	 * Finds the speed of the wheel in units at the given time.
	 * 
	 * @param t
	 * @param dest A {@link Point} where x is the left wheel and y is the right wheel -- (left, right)
	 */
	public void wheelSpeeds(double t, Point dest) {
		double speed = speed(t);
		double omega = omega(t);
		double tangential = omega * ROBOT_RADIUS;
		
		dest.x = speed - tangential;
		dest.y = speed + tangential;
	}
	
	private void wheelSpeeds(int index, double t, Point dest) {
		double speed = pathData.speed(index, t);
		double omega = pathData.omega(index, t);
		double tangential = omega * ROBOT_RADIUS;
		
		dest.x = speed - tangential;
		dest.y = speed + tangential;
	}
	
	/**
	 * Finds the distance each wheel has traveled in units at the given time.
	 * 
	 * @param t
	 * @param dest A {@link Point} where x is the left wheel and y is the right wheel -- (left, right)
	 */
	public void wheelDistances(double t, Point dest) {
		if (t > pathData.T) {
			t = pathData.T;
		}

		Point wheelSpeeds = new Point();
		Point nextWheelSpeeds = new Point();
		dest.x = 0;
		dest.y = 0;
		int index = pathData.indexForTime(t);
		
		for (int i = 0; i < index; i++) {
			wheelSpeeds(i+1, pathData.t[i+1], nextWheelSpeeds);
			
			dest.x += area(nextWheelSpeeds.x, wheelSpeeds.x, pathData.t[i], pathData.t[i+1]);
			dest.y += area(nextWheelSpeeds.y, wheelSpeeds.y, pathData.t[i], pathData.t[i+1]);
			
			wheelSpeeds.x = nextWheelSpeeds.x;
			wheelSpeeds.y = nextWheelSpeeds.y;
		}
		
		wheelSpeeds(index, t, nextWheelSpeeds);
		
		dest.x += area(nextWheelSpeeds.x, wheelSpeeds.x, pathData.t[index], t);
		dest.y += area(nextWheelSpeeds.y, wheelSpeeds.y, pathData.t[index], t);
	}
	
	/**
	 * Time for the whole path to complete
	 * @return Time
	 */
	public double duration() {
		return pathData.T;
	}
	
	private static double area(double h1, double h2, double t1, double t2) {
		return (h1+h2)*(t2-t1)/2;
	}
	
	public static Path pathToPoint(double startingSpeed, double endingSpeed, double deltaAngle, Point dest) {
		double omega1 = 0;
		double omega2 = 0;
		double middleTime = 0;
		Path path = new Path(omega1, omega2, middleTime, startingSpeed, endingSpeed, endingSpeed, deltaAngle);
	}
	
}
