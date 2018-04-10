import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class XYChartPlotter extends ApplicationFrame {

	private static final long serialVersionUID = -4407905459601128510L;

	public XYChartPlotter(final String title) {

		super(title);
		
		double[][] params = new double[30][];
		
		int n = 0;
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 3; j++) {
				params[n] = new double[]{1.35, 1.35 - j*0.05, 0.2, 0, 20, 20, Math.PI/8 * i + Math.PI/4};
				n++;
				params[n] = new double[]{0.75, 1.35 - j*0.05, 0.2, 0, 20, 20, Math.PI/8 * i + Math.PI/4};
				n++;
			}
		}

		/*final XYDataset dataset = createMultiPathDataset(
			new double[]{1.35, 1.5, 0.2, 0, 20, 20, Math.PI/4}, 
			new double[]{1.35, 1.45, 0.2, 0, 20, 20, Math.PI/4},
			new double[]{1.35, 1.4, 0.2, 0, 20, 20, Math.PI/4},
			new double[]{1.35, 1.35, 0.2, 0, 20, 20, Math.PI/4},
			new double[]{1.35, 1.3, 0.2, 0, 20, 20, Math.PI/4},
			new double[]{1.35, 1.25, 0.2, 0, 20, 20, Math.PI/4}
		);*/
		final XYDataset dataset = createMultiPathDataset(params);
		final JFreeChart chart = createChart(dataset);
		
		/*final XYDataset[] datasets = createSinglePathDataset(2.5, -2.5, 1, 0, 20, 20, 0);
		final XYDataset left = datasets[0];
		final XYDataset right = datasets[1];
		final JFreeChart chart = createChart(left, right);*/
		
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(700, 700));
		setContentPane(chartPanel);

	}

	private XYDataset[] createSinglePathDataset(double w1, double w2, double b, double s, double h, double e, double c) {

		final XYSeries leftPos = new XYSeries("Left", false);
		final XYSeries rightPos = new XYSeries("Right", false);
		final XYSeries leftDist = new XYSeries("Left Distance");
		final XYSeries rightDist = new XYSeries("Right Distance");
		final XYSeries leftSpeed = new XYSeries("Left Speed");
		final XYSeries rightSpeed = new XYSeries("Right Speed");
		
		final XYSeries angle = new XYSeries("Angle");
		final XYSeries omega = new XYSeries("Omega");

		Path path = new Path(w1, w2, b, s, h, e, c);
		Point left = new Point();
		Point right = new Point();
		Point speed = new Point();
		Point dist = new Point();
		
		int samples = 200;
		for (int i = 0; i <= samples; i ++) {
			double t = ((double)i) * path.duration() / samples;
			path.wheelPositions(t, left, right);
			path.wheelDistances(t, dist);
			path.wheelSpeeds(t, speed);
			
			leftPos.add(left.x, left.y);
			rightPos.add(right.x, right.y);
			leftSpeed.add(t/path.duration() * 20, speed.x/2);
			rightSpeed.add(t/path.duration() * 20, speed.y/2);
			leftDist.add(t/path.duration() * 20, dist.x);
			rightDist.add(t/path.duration() * 20, dist.y);
			angle.add(t/path.duration() * 20, path.angle(t)*6);
			omega.add(t/path.duration() * 20, path.omega(t));
		}

		final XYSeriesCollection leftDataset = new XYSeriesCollection();
		final XYSeriesCollection rightDataset = new XYSeriesCollection();
		leftDataset.addSeries(leftPos);
		rightDataset.addSeries(rightPos);
		leftDataset.addSeries(leftSpeed);
		rightDataset.addSeries(rightSpeed);
		leftDataset.addSeries(leftDist);
		rightDataset.addSeries(rightDist);
		//leftDataset.addSeries(angle);
		//rightDataset.addSeries(omega);

		return new XYSeriesCollection[]{leftDataset, rightDataset};

	}
	
	private XYDataset createMultiPathDataset(double[]... parameters) {

		final XYSeries[] serieses = new XYSeries[parameters.length];
		final Path[] paths = new Path[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			serieses[i] = new XYSeries("Robot " + String.valueOf(i+1), false);
			paths[i] = new Path(parameters[i][0], parameters[i][1], parameters[i][2], parameters[i][3], parameters[i][4], parameters[i][5], parameters[i][6]);
		}

		Point pos = new Point();
		final XYSeriesCollection dataset = new XYSeriesCollection();
		
		int samples = 100;
		for (int j = 0; j < parameters.length; j++) {
			for (int i = 0; i <= samples; i ++) {
				double t = ((double)i) * paths[j].duration() / samples;
				paths[j].position(t, pos);
				double x = pos.x;
				double y = pos.y;
				x = Math.floor(x * 10000) / 10000;
				y = Math.floor(y * 10000) / 10000;
				
				serieses[j].add(x, y);
			}
			paths[j].position(paths[j].duration(), pos);
			serieses[j].add(pos.x, pos.y);
			dataset.addSeries(serieses[j]);
			System.out.println(pos.x + "\t" + pos.y);
		}

		return dataset;

	}

	private JFreeChart createChart(final XYDataset left, final XYDataset right) {

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Robot Path",      // chart title
				"X",                      // x axis label
				"Y",                      // y axis label
				null,                  // data
				PlotOrientation.VERTICAL,
				true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);
		
		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);

		//      final StandardLegend legend = (StandardLegend) chart.getLegend();
		//      legend.setDisplaySeriesShapes(true);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDataset(0, left);
		plot.setDataset(1, right);
		//    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		final XYLineAndShapeRenderer rendererLeft = new XYLineAndShapeRenderer();
		final XYLineAndShapeRenderer rendererRight = new XYLineAndShapeRenderer();
		for (int i = 0; i < left.getSeriesCount(); i++) {
			rendererLeft.setSeriesLinesVisible(i, true);
			rendererLeft.setSeriesShapesVisible(i, false);
			rendererLeft.setSeriesPaint(i, Color.red);
			rendererRight.setSeriesLinesVisible(i, true);
			rendererRight.setSeriesShapesVisible(i, false);
			rendererRight.setSeriesPaint(i, Color.blue);
		}
		plot.setRenderer(0, rendererLeft);
		plot.setRenderer(1, rendererRight);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;

	}
	
	private JFreeChart createChart(final XYDataset data) {

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Robot Positions",      // chart title
				"X",                      // x axis label
				"Y",                      // y axis label
				data,                  // data
				PlotOrientation.VERTICAL,
				true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);
		
		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);

		//      final StandardLegend legend = (StandardLegend) chart.getLegend();
		//      legend.setDisplaySeriesShapes(true);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		//    plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		for (int i = 0; i < data.getSeriesCount(); i++) {
			renderer.setSeriesLinesVisible(i, true);
			renderer.setSeriesShapesVisible(i, false);
		}
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;

	}

	public static void main(final String[] args) {

		XYChartPlotter demo = new XYChartPlotter("Robot Path");
		demo.pack();
		RefineryUtilities.centerFrameOnScreen(demo);
		demo.setVisible(true);

	}
}
