
public class PathMath {
	
	public static final int TAYLOR_TERMS = 7+1; //+1 for OBOB
	
	private static double SQRT_A, INV_SQRT_A, INV_A, ADD_CONSTANT;
	private static int SIGN;
	
	public static void cacheConstants(double a, double b) {
		SIGN = (int) Math.signum(a);
		a = Math.abs(a);
		SQRT_A = Math.sqrt(a/2);
		INV_SQRT_A = 1/SQRT_A;
		INV_A = 2 / a;
		ADD_CONSTANT = SIGN*b/(2*SQRT_A);
	}
	
	public static double cI1(double x) {
		double sum = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS; i++) {
			long outer = termSign * ((4*i+1) * factorial(2*i));
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+1) - Math.pow(ADD_CONSTANT, 4*i+1);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= INV_SQRT_A;
		return sum;
	}
	
	public static double cI2(double x) {
		double sum = 0;
		double c = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS; i++) {
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
	
	public static double sI1(double x) {
		double sum = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS; i++) {
			long outer = termSign * ((4*i+3) * factorial(2*i+1));
			termSign *= -1;
			
			double inner = Math.pow(SQRT_A * x + ADD_CONSTANT, 4*i+3) - Math.pow(ADD_CONSTANT, 4*i+3);
			
			double term = inner / outer;
			sum += term;
		}
		
		sum *= SIGN * INV_SQRT_A;
		return sum;
	}
	
	public static double sI2(double x) {
		double sum = 0;
		double c = 0;
		int termSign = 1;
		for (int i = 0; i < TAYLOR_TERMS; i++) {
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
	
	public static double cI1S(double x, double b) {
		return Math.sin(b*x) / b;
	}
	
	public static double cI2S(double x, double b) {
		return (1 - Math.cos(b*x) ) / (b*b);
	}
	
	public static double sI1S(double x, double b) {
		return (1 - Math.cos(b*x)) / b;
	}
	
	public static double sI2S(double x, double b) {
		return (b*x - Math.sin(b*x) ) / (b*b);
	}
	
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
