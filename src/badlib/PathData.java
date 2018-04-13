package badlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Takes periods of acceleration for angle and position and puts
 * this data into a form usable by PathMath (thru Path). Also has functions
 * for finding speed, omega, and angle at a given time. All indexes represent conditions when
 * the associated time slot begins. For example, the first element of {@link #t} is
 * 0, because the path begins at time 0. As a result, the first element of {@link #omega} is also 0,
 * because the path begins without any rotational speed.
 * 
 * @author andrew
 *
 */
public class PathData {

	public double[] tA, pA, t;
	public double T;

	public double[] speed, omega, angle;
	
	/**
	 * The periods of the time during which angle and position are changing.
	 * Linear periods must be ordered by time relative to the other linear periods.
	 * Angular periods must be ordered by time relative to the other angular periods.
	 * If the above condition is met, the periods may be mixed together in any way.
	 * 
	 * @param periods Acceleration periods
	 */
	public PathData(double initialSpeed, Period... periods) {
		ArrayList<Period> periodsList = new ArrayList<Period>(Arrays.asList(periods));
		for (int i = periodsList.size() - 1; i >= 0; i--) {
			if (periodsList.get(i).duration <= 0.0001) {
				periodsList.remove(i);
			}
		}
		
		double angularTime = 0;
		double linearTime = 0;
		
		for (Period period : periodsList) {
			double duration = period.duration;
			if (period.angular) {
				period.duration = angularTime;
				angularTime += duration;
			} else {
				period.duration = linearTime;
				linearTime += duration;
			}
		}
		
		Collections.sort(periodsList);
		
		periods = periodsList.toArray(new Period[periodsList.size()]);
				
		boolean[] hasTwo = new boolean[periods.length];
		int i = 0;		int segments = periods.length;
		while (i < periods.length-1) {
			if (Math.abs(periods[i].duration - periods[i+1].duration) < 0.0001) {
				hasTwo[i] = true;
				i++;
				segments--;
			} else {
				hasTwo[i] = false;
			}
			i++;
		}
		
		this.T = angularTime;
		this.t = new double[segments];
		this.tA = new double[segments];
		this.pA = new double[segments];
		this.omega = new double[segments];
		this.speed = new double[segments];
		this.speed[0] = initialSpeed;
		this.angle = new double[segments];
		
		int n = 0;
		for (i = 0; i < periods.length; i++) {
			Period period = periods[i];
			this.t[n] = periods[i].duration;
			
			if (hasTwo[i]) {
				if (period.angular) {
					this.tA[n] = period.rate;
				} else {
					this.pA[n] = period.rate;
				}
				i++;
				period = periods[i];
				if (period.angular) {
					this.tA[n] = period.rate;
				} else {
					this.pA[n] = period.rate;
				}
			} else {
				if (period.angular) {
					this.tA[n] = period.rate;
					this.pA[n] = this.pA[n-1];
				} else {
					this.pA[n] = period.rate;
					this.tA[n] = this.tA[n-1];
				}
			}
			n++;
		}
		
		for (i = 1; i < t.length; i++) {
			this.omega[i] = omega(i-1, this.t[i]);
			this.speed[i] = speed(i-1, this.t[i]);
			this.angle[i] = angle(i-1, this.t[i]);
		}

	}
	
	private static double area(double h1, double h2, double dt) {
		return (h1+h2)*dt/2;
	}
	
	public int indexForTime(double t) {
		for (int i = 0; i < this.t.length; i++) {
			if (this.t[i] > t) {
				return i-1;
			}
		}
		
		return this.t.length-1;
	}
	
	/**
	 * Returns angular velocity as a function of time
	 * 
	 * @param index Index of this t's time bracket
	 * @param t
	 * @return Omega (radians)
	 */
	public double omega(int index, double t) {
		if (t > T) {
			return this.omega[index] + this.tA[index] * (T - this.t[index]);
		} else {
			return this.omega[index] + this.tA[index] * (t - this.t[index]);
		}
	}
	
	/**
	 * Returns angle of the robot as a function of time
	 * 
	 * @param index Index of this t's time bracket
	 * @param t
	 * @return Angle in radians
	 */
	public double angle(int index, double t) {
		if (t > T) {
			return angle[index] + area(omega[index], omega(index, T), T - this.t[index]);
		} else { 
			return angle[index] + area(omega[index], omega(index, t), t - this.t[index]);
		}
	}
	
	/**
	 * Returns speed in the direction currently facing as a function of time
	 * 
	 * @param index Index of this t's time bracket
	 * @param t
	 * @return Speed
	 */
	public double speed(int index, double t) {
		if (t > T) {
			return this.speed[index] + this.pA[index] * (T - this.t[index]);
		} else {
			return this.speed[index] + this.pA[index] * (t - this.t[index]);
		}
	}
}
