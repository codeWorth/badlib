package badlib;

import java.util.ArrayList;
import java.util.Arrays;

public class PathData {

	public double[] tA, pA, t, offset;
	public Point[] point, wheelSpeed; 

	/**
	 * The periods of the time during which angle and position are changing.
	 * Linear periods must be ordered by time relative to the other linear periods.
	 * Angular periods must be ordered by time relative to the other angular periods.
	 * If the above condition is met, the periods may be mixed together in any way.
	 * 
	 * @param periods Acceleration periods
	 */
	public PathData(AccelerationPeriod... periods) {
		ArrayList<AccelerationPeriod> periodsList = new ArrayList<AccelerationPeriod>(Arrays.asList(periods));
		for (int i = periodsList.size() - 1; i >= 0; i--) {
			if (periodsList.get(i).duration <= 0) {
				periodsList.remove(i);
			}
		}
		periods = periodsList.toArray(new AccelerationPeriod[periodsList.size()]);
		
		double angularTime = 0;
		double linearTime = 0;
		
		for (AccelerationPeriod period : periods) {
			double duration = period.duration;
			if (period.angular) {
				period.duration += angularTime;
				angularTime += duration;
			} else {
				period.duration += linearTime;
				linearTime += duration;
			}
		}
		
		Arrays.sort(periods);
		
		
	}
	
}
