
/**
 * This is a helper class for the {@link Path} class. It does the heavy lifting on the math. It is able to go from
 * a description of motion in polar coordinates (with constants angular and linear acceleration) and find the position
 * at any given time.
 * 
 * In order to use this class, you must first call {@link #cacheConstants(double, double)} and input the angular acceleration
 * and initial angular velocity. Some slightly intensive constants are calculated in this function, then the rest of the 
 * functions will work properly.
 * 
 * @author andrew
 *
 */
public class PathMath {
	
	public static final int TAYLOR_TERMS = 7;
	private static final int TAYLOR_TERMS_OBOB = TAYLOR_TERMS+1; //+1 for OBOB
	
	// Cached constants for performance
	private static double SQRT_A, INV_SQRT_A, INV_A, ADD_CONSTANT;
	private static int SIGN;
	
	/**
	 * Caches constants needed in the taylor series's later on,
	 * for performance reasons.
	 * 
	 * @param a Angular acceleration (radians)
	 * @param b Initial angular velocity (radians)
	 */
	public static void cacheConstants(double a, double b) {
		SIGN = (int) Math.signum(a);
		a = Math.abs(a);
		SQRT_A = Math.sqrt(a/2);
		INV_SQRT_A = 1/SQRT_A;
		INV_A = 2 / a;
		ADD_CONSTANT = SIGN*b/(2*SQRT_A);
	}
	
	/**
	 * Taylor series for the first integral of cosine, where the
	 * function inside the cosine is more complicated than simple "x".
	 * Specifically, the angle at any given time is a quadratic dependent
	 * on angular acceleration and initial angular velocity, which makes taking
	 * the antiderivative hard. The number of terms of the taylor series is
	 * {@value #TAYLOR_TERMS}
	 * 
	 * @param x Time
	 * @return Approximation for integral of the cosine of a function
	 */
	public static double cI1(double x) {
		double sum = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS_OBOB; i++) {
			long outer = termSign * ((4*i+1) * factorial(2*i));
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+1) - Math.pow(ADD_CONSTANT, 4*i+1);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= INV_SQRT_A;
		return sum;
	}
	
	/**
	 * Integral of {@link #cI1(double)}.
	 * 
	 * @param x Time
	 * @return Approximation for second integral of the cosine of a function
	 */
	public static double cI2(double x) {
		double sum = 0;
		double c = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS_OBOB; i++) {
			long outer = termSign * ((4*i+1) * factorial(2*i));
			c += Math.pow(ADD_CONSTANT, 4*i+1) / outer;

			outer = outer * (4*i+2);
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+2) - Math.pow(ADD_CONSTANT, 4*i+2);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= INV_A;
		c *= INV_SQRT_A * x;
		
		return sum - c;
	}
	
	/**
	 * Same as {@link #cI1(double)}, except this Taylor series approximates sine.
	 * 
	 * @param x Time
	 * @return Approximation for integral of the sine of a function
	 */
	public static double sI1(double x) {
		double sum = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS_OBOB; i++) {
			long outer = termSign * ((4*i+3) * factorial(2*i+1));
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+3) - Math.pow(ADD_CONSTANT, 4*i+3);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= SIGN * INV_SQRT_A;
		return sum;
	}
	
	/**
	 * Integral of {@link #sI1(double)}.
	 * 
	 * @param x Time
	 * @return Approximation for second integral of the sine of a function
	 */
	public static double sI2(double x) {
		double sum = 0;
		double c = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS_OBOB; i++) {
			long outer = termSign * ((4*i+3) * factorial(2*i+1));
			c += Math.pow(ADD_CONSTANT, 4*i+3) / outer;

			outer = outer * (4*i+4);
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+4) - Math.pow(ADD_CONSTANT, 4*i+4);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= SIGN * INV_A;
		c *= SIGN * INV_SQRT_A * x;
		
		return sum - c;
	}
	
	/**
	 * When angular acceleration is 0, the antiderivative is greatly simplified, so can simply
	 * be expressed without a Taylor series
	 * 
	 * @param x Time
	 * @param b Initial angular velocity
	 * @return Integral of cosine of a function
	 */
	public static double cI1S(double x, double b) {
		return Math.sin(b*x) / b;
	}
	
	/**
	 * Integral of {@link #cI1S(double, double)}.
	 * 
	 * @param x Time
	 * @param b Initial angular velocity
	 * @return 2nd Integral of cosine of a function
	 */
	public static double cI2S(double x, double b) {
		return (1 - Math.cos(b*x) ) / (b*b);
	}
	
	/**
	 * Same as {@link #cI1S(double, double)}, except for sine.
	 * 
	 * @param x Time
	 * @param b Initial angular velocity
	 * @return Integral of sine of a function
	 */
	public static double sI1S(double x, double b) {
		return (1 - Math.cos(b*x)) / b;
	}
	
	/**
	 * Integral of {@link #sI1S(double, double)}.
	 * 
	 * @param x Time
	 * @param b Initial angular velocity
	 * @return Integral of sine of a function
	 */
	public static double sI2S(double x, double b) {
		return (b*x - Math.sin(b*x) ) / (b*b);
	}
	
	/**
	 * As a result of simplification, the ending x and y positions are rotated around 
	 * the origin by some angle. In order to find correct x and y, the points need to be
	 * rotated back by that angle. This function finds the angle that x and y were offset by
	 * 
	 * @param a Angular acceleration
	 * @param b Initial angular velocity
	 * @return radians offset
	 */
	public static double offset(double a, double b) {
		if (a == 0) {
			return 0;
		} else {
			return b*b/(2*a);
		}
	}
	
	/**
	 * Calculates the new position of a point rotated in reverse around the origin by some amount.
	 * Angle is effectively multiplied by -1.
	 * Writes to the given points
	 * 
	 * @param x
	 * @param y
	 * @param a Angle in radians
	 * @param point Point that will be written to
	 */
	public static void rotatePoint(double x, double y, double a, Point point) {
		double cos = Math.cos(-a);
		double sin = Math.sin(-a);
		point.x = x*cos - y*sin;
		point.y = x*sin + y*cos;
	}
	
	public static long factorial(int n) {
		long result = 1;
		for (int i = 1; i <= n; i++) {
			result *= i;
		}
		
		return result;
	}
	
	/**
	 * Integrates to find position of a point with given path.
	 * Writes location into given point.
	 * Uses tabular integration.
	 * 
	 * @param t Time to integrate to
	 * @param tA Theta acceleration
	 * @param w Initial omega
	 * @param pA Position acceleration
	 * @param s Initial speed
	 * @param dest Point to be written to
	 */
	public static void integrate(double t, double tA, double w, double pA, double s, Point dest) {		
		if (tA == 0) {
			dest.x = (t*pA + s) * cI1S(t, w) + pA * cI2S(t, w);
			dest.y = (t*pA + s) * sI1S(t, w) + pA * sI2S(t, w);
		} else {
			cacheConstants(tA, w);
			dest.x = (t*pA + s) * cI1(t) - pA * cI2(t);
			dest.y = (t*pA + s) * sI1(t) - pA * sI2(t);
		}
	}
	
}
