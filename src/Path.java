
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
	public static double POSITION_MAX_ACCEL = 100; //inches
	public static double ROBOT_RADIUS = 1;

	private double A,B,C; // omega change periods, time
	private double Q,R,S; // speed change periods, time
	private double T; // total period, time
	private double tA, tM, tD; // thetaAccel, thetaMiddle, and thetaDecel, radians
	private double pA,pD; // positionAccel and positionDecel
	private double sO; // initial speed
	private double T1, T2, T3, T4, T5; // times at discontinuities
	private Point p1, p2, p3, p4; // Points at each discontinuity
	private double tA1, pA1, tA2, pA2, tA3, tA4, pA4, tA5, pA5; // linear and angular acceleration for each endpoint
	private double o2, o3, o4, o5; // offset angles for each endpoint
	private Point s0, s1, s2, s3, s4, s5; // speed of the wheels at each endpoint
	
	/**
	 * Create a path, if possible, with these given parameters.
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
		if (deltaTheta * omega1 < 0) {
			System.out.println("You're trying to accelerate in the wrong direction!");
			throw new IllegalArgumentException();
		}
		
		tA = Math.abs(ANGLE_MAX_ACCEL) * Math.signum(omega1);
		if (tA == 0) {
			tA = Math.abs(ANGLE_MAX_ACCEL);
		}
		
		A = omega1/tA;
		B = middleTime;
		C = ( 2*deltaTheta - A*omega1 - B*(omega1+omega2) ) / omega2;
		
		tD = -omega2/C;
		if (Math.abs(tD) > Math.abs(tA)+0.001 || C < 0) {
			System.out.println("Trying to decelerate too fast!");
			throw new IllegalArgumentException();
		}
		tM = (omega2-omega1)/B;
		if (Math.abs(tM) > Math.abs(tA)+0.001) {
			System.out.println("Trying to change directions too fast!");
			throw new IllegalArgumentException();
		}
		
		T = A+B+C;
		
		pA = Math.abs(POSITION_MAX_ACCEL) * Math.signum(wantedSpeed - startSpeed);
		pD = Math.abs(POSITION_MAX_ACCEL) * Math.signum(endSpeed - wantedSpeed);
		if (pA == 0) {
			pA = Math.abs(POSITION_MAX_ACCEL);
		} else if (pD == 0) {
			pD = Math.abs(POSITION_MAX_ACCEL);
		}
		
		Q = (wantedSpeed - startSpeed)/pA;
		S = (endSpeed - wantedSpeed)/pD;
		R = T - Q - S;
		sO = startSpeed;
		
		if (R < 0) {
			System.out.println("Can't reach wantedSpeed in time!");
			throw new IllegalArgumentException();
		}
		
		T1 = Math.min(A, Q);
		T2 = Math.max(A, Q);
		T3 = Math.min(A+B, Q+R);
		T4 = Math.max(A+B, Q+R);
		T5 = T;
		
		// from here on down it's all caching things for higher performance
		
		tA1 = angularAcceleration(T1);
		tA2 = angularAcceleration(T2);
		tA3 = angularAcceleration(T3);
		tA4 = angularAcceleration(T4);
		tA5 = angularAcceleration(T5);
		
		pA1 = linearAcceleration(T1);
		pA2 = linearAcceleration(T2);
		pA4 = linearAcceleration(T4);
		pA5 = linearAcceleration(T5);
		
		o2 = PathMath.offset(tA2, omega(T1)) - angle(T1);
		o3 = PathMath.offset(tA3, omega(T2)) - angle(T2);
		o4 = PathMath.offset(tA4, omega(T3)) - angle(T3);
		o5 = PathMath.offset(tA5, omega(T4)) - angle(T4);
		
		p1 = new Point();
		position(T1, p1);
		p2 = new Point();
		position(T2, p2);
		p3 = new Point();
		position(T3, p3);
		p4 = new Point();
		position(T4, p4);
		
		s0 = new Point();
		wheelSpeeds(0, s0);
		s1 = new Point();
		wheelSpeeds(T1, s1);
		s2 = new Point();
		wheelSpeeds(T2, s2);
		s3 = new Point();
		wheelSpeeds(T3, s3);
		s4 = new Point();
		wheelSpeeds(T4, s4);
		s5 = new Point();
		wheelSpeeds(T5, s5);

	}
	
	/**
	 * Returns angle of the robot as a function of time
	 * 
	 * @param t
	 * @return Angle in radians
	 */
	public double angle(double t) {
		if (t < 0) {
			return 0;
		} else if (t <= A) {
			return area(omega(0), omega(t), 0, t);
		} else if (t <= A+B) {
			return  area(omega(A), omega(t), A, t) + area(omega(0), omega(A), 0, A);
		} else if (t <= T) {
			return area(omega(A+B), omega(t), A+B, t) + area(omega(A), omega(A+B), A, A+B) + area(omega(0), omega(A), 0, A);
		} else {
			return area(omega(A+B), omega(T), A+B, T) + area(omega(A), omega(A+B), A, A+B) + area(omega(0), omega(A), 0, A);
		}
	}
	
	/**
	 * Returns angular velocity of the robot as a function of time
	 * 
	 * @param t
	 * @return Omega (radians)
	 */
	public double omega(double t) {
		if (t < 0) {
			return 0;
		} else if (t <= A) {
			return tA*t;
		} else if (t <= A+B) {
			return tA*A + tM*(t-A);
		} else if (t <= T) {
			return tD*(t-A-B) + tM*B + tA*A;
		} else {
			return tD*C + tM*B + tA*A;
		}
	}
	
	/**
	 * Returns angular acceleration of the robot as a function of time
	 * 
	 * @param t
	 * @return Alpha (radians)
	 */
	public double angularAcceleration(double t) {
		if (t < 0) {
			return 0;
		} else if (t <= A) {
			return tA;
		} else if (t <= A+B) {
			return tM;
		} else if (t <= T) {
			return tD;
		} else {
			return 0;
		}
	}
	
	/**
	 * Returns speed in the direction the robot is facing as a function of time
	 * 
	 * @param t
	 * @return Speed
	 */
	public double speed(double t) {
		if (t < 0) {
			return sO;
		} else if (t <= Q) {
			return pA*t + sO;
		} else if (t <= Q+R) {
			return pA*Q + sO;
		} else if (t <= T) {
			return pD*(t-Q-R) + pA*Q + sO;
		} else {
			return speed(T);
		}
	}
	
	/**
	 * Returns acceleration in the direction the robot is facing as a function of time
	 * 
	 * @param t
	 * @return Acceleration
	 */
	public double linearAcceleration(double t) {
		if (t < 0) {
			return 0;
		} else if (t <= Q) {
			return pA;
		} else if (t <= Q+R) {
			return 0;
		} else if (t <= T) {
			return pD;
		} else {
			return 0;
		}
	}
	
	/**
	 * Finds the (x, y) position of the robot at time t
	 *
	 * @param t
	 * @param dest The {@link Point} to put the coordinates into.
	 */
	public void position(double t, Point dest) {
		if (t < 0) {
			
			dest.x = 0;
			dest.y = 0;
			
		} else if (t <= T1) {
			
			PathMath.integrate(t, tA1, omega(0), pA1, speed(0), dest);
			
		} else if (t <= T2) {
			
			PathMath.integrate(t - T1, tA2, omega(T1), pA2, speed(T1), dest);
			PathMath.rotatePoint(dest.x, dest.y, o2, dest);
			dest.x += p1.x;
			dest.y += p1.y;
			
		} else if (t <= T3) {
			
			PathMath.integrate(t - T2, tA3, omega(T2), 0, speed(T2), dest);
			PathMath.rotatePoint(dest.x, dest.y, o3, dest);
			dest.x += p2.x;
			dest.y += p2.y;
			
		} else if (t <= T4) {
			
			PathMath.integrate(t - T3, tA4, omega(T3), pA4, speed(T3), dest);
			PathMath.rotatePoint(dest.x, dest.y, o4, dest);
			dest.x += p3.x;
			dest.y += p3.y;
			
		} else if (t <= T5) {
			
			PathMath.integrate(t - T4, tA5, omega(T4), pA5, speed(T4), dest);
			PathMath.rotatePoint(dest.x, dest.y, o5, dest);
			dest.x += p4.x;
			dest.y += p4.y;
			
		} else {
			
			PathMath.integrate(T5 - T4, tA5, omega(T4), pA5, speed(T4), dest);
			PathMath.rotatePoint(dest.x, dest.y, o5, dest);
			dest.x += p4.x;
			dest.y += p4.y;
			
		}
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
	
	/**
	 * Finds the distance each wheel has traveled in units at the given time.
	 * 
	 * @param t
	 * @param dest A {@link Point} where x is the left wheel and y is the right wheel -- (left, right)
	 */
	public void wheelDistances(double t, Point dest) {
		if (t < 0) {
			dest.x = 0;
			dest.y = 0;
		} else if (t <= T1) {
			wheelSpeeds(t, dest);
			dest.x = area(s0.x,dest.x,0,t);
			dest.y = area(s0.y,dest.y,0,t);
		} else if (t <= T2) {
			wheelSpeeds(t, dest);
			dest.x = area(s1.x, dest.x, T1, t) + area(s0.x, s1.x, 0, T1);
			dest.y = area(s1.y, dest.y, T1, t) + area(s0.y, s1.y, 0, T1);
		} else if (t <= T3) {
			wheelSpeeds(t, dest);
			dest.x = area(s2.x, dest.x, T2, t) + area(s1.x, s2.x, T1, T2) + area(s0.x, s1.x, 0, T1);
			dest.y = area(s2.y, dest.y, T2, t) + area(s1.y, s2.y, T1, T2) + area(s0.y, s1.y, 0, T1);
		} else if (t <= T4) {
			wheelSpeeds(t, dest);
			dest.x = area(s3.x, dest.x, T3, t) + area(s2.x, s3.x, T2, T3) + area(s1.x, s2.x, T1, T2) + area(s0.x, s1.x, 0, T1);
			dest.y = area(s3.y, dest.y, T3, t) + area(s2.y, s3.y, T2, T3) + area(s1.y, s2.y, T1, T2) + area(s0.y, s1.y, 0, T1);
		} else if (t <= T5) {
			wheelSpeeds(t, dest);
			dest.x = area(s4.x, dest.x, T4, t) + area(s3.x, s4.x, T3, T4) + area(s2.x, s3.x, T2, T3) + area(s1.x, s2.x, T1, T2) + area(s0.x, s1.x, 0, T1);
			dest.y = area(s4.y, dest.y, T4, t) + area(s3.y, s4.y, T3, T4) + area(s2.y, s3.y, T2, T3) + area(s1.y, s2.y, T1, T2) + area(s0.y, s1.y, 0, T1);
		} else {
			dest.x = area(s4.x, s5.x, T4, T5) + area(s3.x, s4.x, T3, T4) + area(s2.x, s3.x, T2, T3) + area(s1.x, s2.x, T1, T2) + area(s0.x, s1.x, 0, T1);
			dest.y = area(s4.y, s5.y, T4, T5) + area(s3.y, s4.y, T3, T4) + area(s2.y, s3.y, T2, T3) + area(s1.y, s2.y, T1, T2) + area(s0.y, s1.y, 0, T1);
		}
	}
	
	/**
	 * Time for the whole path to complete
	 * @return Time
	 */
	public double duration() {
		return T;
	}
	
	private static double area(double h1, double h2, double t1, double t2) {
		return (h1+h2)*(t2-t1)/2;
	}
	
	public static void main(String[] args) {
		double time = System.currentTimeMillis();
		int samples = 100000;
		Point point = new Point();
		for (int i = 0; i < samples; i++) {
			Path path = new Path(1.35, 1.15, 0.2, 0, 20, 20, Math.PI/3);
			path.position(path.duration(), point);
		}
		System.out.println(System.currentTimeMillis() - time);
	}
	
}
