package badlib;

/**
 * A period of time during which some amount is changing (or not if {@link #rate} is zero).
 * If {@link #angular} is true the acceleration is angular, if it is false the acceleration is linear.
 * @author andrew
 *
 */
public class AccelerationPeriod implements Comparable<AccelerationPeriod> {

	public double duration, rate;
	public boolean angular;
	
	public AccelerationPeriod(double duration, double rate, boolean angular) {
		this.duration = duration;
		this.rate = rate;
		this.angular = angular;
	}
	
	@Override
	public int compareTo(AccelerationPeriod o) {
		double delta = this.duration - o.duration;
		delta += Math.signum(delta);
		return (int)delta;
	}
	
}
