package badlib;

public class PathGradientDescent {

	public static final double CLOSE_ENOUGH_RADIUS = 2;
	public static final double DESCENT_SPEED = 0.01;
	
	private Point wantedPoint;
	private Point resultPoint = new Point();
	private Path path;
	
	private double omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta;
	
	public PathGradientDescent(double omega1, double omega2, double middleTime, double startSpeed, double wantedSpeed, double endSpeed, double deltaTheta, Point wantedPoint) {
		this.omega1 = omega1;
		this.omega2 = omega2;
		this.middleTime = middleTime;
		this.startSpeed = startSpeed;
		this.wantedSpeed = wantedSpeed;
		this.endSpeed = endSpeed;
		this.deltaTheta = deltaTheta;
		this.path = new Path(omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta);
		this.wantedPoint = wantedPoint;
	}
	
	public Path tuneOnce() {
		double dParam = 0.05; // delta for each parameter
		double currentCost = cost();
		
		path.initialize(omega1+dParam, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta);
		double o1Slope = (cost() - currentCost) / dParam;
		
		path.initialize(omega1-dParam, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta);
		double o2Slope = (cost() - currentCost) / dParam;
		
		path.initialize(omega1, omega2, middleTime+dParam, startSpeed, wantedSpeed, endSpeed, deltaTheta);
		double mTSlope = (cost() - currentCost) / dParam;
		
		path.initialize(omega1, omega2, middleTime, startSpeed, wantedSpeed+dParam, endSpeed, deltaTheta);
		double wSSlope = (cost() - currentCost) / dParam;
		
		path.initialize(omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed+dParam, deltaTheta);
		double eSSlope = (cost() - currentCost) / dParam;
		
		this.omega1 -= o1Slope * DESCENT_SPEED;
		//this.omega2 += o2Slope * DESCENT_SPEED;
		//this.middleTime += mTSlope * DESCENT_SPEED;
		//this.wantedSpeed += wSSlope * DESCENT_SPEED;
		//this.endSpeed += eSSlope * DESCENT_SPEED;
		
		if (Path.validParameters(omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta)) {
			path.initialize(omega1, omega2, middleTime, startSpeed, wantedSpeed, endSpeed, deltaTheta);
		}
		
		return path;
	}
	
	public Path findCorrectPath() {
		return path;
	}
	
	private double cost() {
		this.path.position(this.path.duration(), resultPoint);
		
		double dX = resultPoint.x - wantedPoint.x;
		double dY = resultPoint.y - wantedPoint.y;
		
		return Math.sqrt(dX*dX + dY*dY);
	}
	
}
