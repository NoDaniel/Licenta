
package eu.esa.orbiprotester.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.EmptyBlock;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.CompositeTitle;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeVectorFunction;
import org.orekit.time.UT1Scale;
import org.orekit.time.UTCScale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Container for the data series used for chart printing.
 * <p>
 * It contains a list of XYSeries, one for each Keplerian parameter
 */
public class ChartDataHolder {

    /** The width of a chart. */
    private static final int CHARTWIDTH = 800;

    /** The height of a chart. */
    private static final int CHARTHEIGHT = 300;

    /** Logger. */
    private static Logger logger;

    /** The information used to create charts. */
    private static ChartInfo[] chartInfos;

    /** The XYSeriesCollection that will be plotted with JFreeChart. */
    private XYSeriesCollection[] chartSeries;

    /**    The type of he time axis. Can be one of 'seconds', 'hours' or 'days'. */
    private String timeAxisType;

    /** The folder where the images should be created. */
    private final File outputFolder;

    /** The simulation name. */
    private final String simulationName;

    static {
        ChartDataHolder.chartInfos = new ChartInfo[] {
            // Keplerian elements
            new ChartInfo(ElementType.A, "A", "Semi-Major axis (km)", "a", 0.0, true, false),
            new ChartInfo(ElementType.E, "E", "Eccentricity", "e", 0.0, true, false),
            new ChartInfo(ElementType.I, "I", "Inclination (deg)", "i", 360.0, true, true),
            new ChartInfo(ElementType.RAAN, "RAAN", "Right Ascension of Ascending Node (deg)", "raan", 360.0, true, true),
            new ChartInfo(ElementType.PA, "PA", "Perigee Argument (deg)", "pa", 360.0, true, true),
            new ChartInfo(ElementType.MA, "MA", "Mean Anomaly (deg)", "ma", 360.0, true, true),
            new ChartInfo(ElementType.PA_MA, "PA_MA", "Perigee Argument plus Mean Anomaly (deg)", "PA + MA", 360.0, true, true),
            new ChartInfo(ElementType.RAAN_PA, "RAAN_PA", "Right Ascension of Ascending Node plus Perigee Argument (deg)", "RAAN + PA", 360.0, true, true),
            new ChartInfo(ElementType.RAAN_PA_MA, "RAAN_PA_MA", "Right Ascension of Ascending Node plus Perigee Argument plus Mean Anomaly (deg)", "RAAN + PA + MA", 360.0, true, true),
            // Equinoctial elements
            new ChartInfo(ElementType.H, "H", "Ey/h component of eccentricity vector", "h", 0.0, true, false),
            new ChartInfo(ElementType.K, "K", "Ex/k component of eccentricity vector", "k", 0.0, true, false),
            new ChartInfo(ElementType.P, "P", "Hy/p component of inclination vector", "p", 0.0, true, false),
            new ChartInfo(ElementType.Q, "Q", "Hx/q component of inclination vector", "q", 0.0, true, false),
            new ChartInfo(ElementType.LM, "LM", "Mean Longitude Argument (deg)", "lm", 360.0, true, true),

            //Carthesian elements
            new ChartInfo(ElementType.POSITION, "POSITION", "Position vector size(km)", "Position", 0.0, true, false),
            new ChartInfo(ElementType.POSITION_X, "POSITION_X", "Position vector x(km)", "Position x", 0.0, true, false),
            new ChartInfo(ElementType.POSITION_Y, "POSITION_Y", "Position vector y(km)", "Position y", 0.0, true, false),
            new ChartInfo(ElementType.POSITION_Z, "POSITION_Z", "Position vector z(km)", "Position z", 0.0, true, false),
            new ChartInfo(ElementType.VELOCITY, "VELOCITY", "Velocity vector size(Km/s)", "Velocity", 0.0, true, false),
            new ChartInfo(ElementType.VELOCITY_X, "VELOCITY_X", "Velocity vector x(Km/s)", "Velocity x", 0.0, true, false),
            new ChartInfo(ElementType.VELOCITY_Y, "VELOCITY_Y", "Velocity vector y(Km/s)", "Velocity y", 0.0, true, false),
            new ChartInfo(ElementType.VELOCITY_Z, "VELOCITY_Z", "Velocity vector z(Km/s)", "Velocity z", 0.0, true, false),

            //Atitude
            new ChartInfo(ElementType.Q0, "Q0", "Scalar coordinate of the quaternion", "Q0", 0.0, true, false),
            new ChartInfo(ElementType.Q1, "Q1", "First coordinate of the vectorial part of the quaternion", "Q1", 0.0, true, false),
            new ChartInfo(ElementType.Q2, "Q2", "Second coordinate of the vectorial part of the quaternion", "Q2", 0.0, true, false),
            new ChartInfo(ElementType.Q3, "Q3", "Third coordinate of the vectorial part of the quaternion", "Q3", 0.0, true, false),
            new ChartInfo(ElementType.ANGLE, "ROTATION_ANGLE", "Rotation angle", "Angle", 360.0, true, true),

            new ChartInfo(ElementType.ROLL, "ROLL", "Roll angle", "Angle", 360.0, true, true),
            new ChartInfo(ElementType.PITCH, "PITCH", "Pitch angle", "Angle", 360.0, true, true),
            new ChartInfo(ElementType.YAW, "YAW", "Yaw angle", "Angle", 360.0, true, true),
            
            
            new ChartInfo(ElementType.ROTATION_RATE_X, "ROTATION_RATE_X", "Rotation Rate X (rad/s)", "Rotation Rate X", 0.0, true, false),
            new ChartInfo(ElementType.ROTATION_RATE_Y, "ROTATION_RATE_Y", "Rotation Rate Y (rad/s)", "Rotation Rate Y", 0.0, true, false),
            new ChartInfo(ElementType.ROTATION_RATE_Z, "ROTATION_RATE_Z", "Rotation Rate Z (rad/s)", "Rotation Rate Z", 0.0, true, false),

            new ChartInfo(ElementType.ROTATION_ACCELERATION_X, "ROTATION_ACCELERATION_X", "Rotation Acceleration X (rad/s^2)", "Rotation Acceleration X", 0.0, true, false),
            new ChartInfo(ElementType.ROTATION_ACCELERATION_Y, "ROTATION_ACCELERATION_Y", "Rotation Acceleration Y (rad/s^2)", "Rotation Acceleration Y", 0.0, true, false),
            new ChartInfo(ElementType.ROTATION_ACCELERATION_Z, "ROTATION_ACCELERATION_Z", "Rotation Acceleration Z (rad/s^2)", "Rotation Acceleration Z", 0.0, true, false),
            
            //Other charts
            new ChartInfo(ElementType.LTAN, "LTAN", "Local Time of Ascending Node (hours)", "Local Time of Ascending Node", 24.0, true, false),
            new ChartInfo(ElementType.PALT, "PALT", "Perigee Altitude (km)", "Perigee altitude", 0.0, true, false),
            new ChartInfo(ElementType.AALT, "AALT", "Apogee Altitude (km)", "Apogee altitude", 0.0, true, false),
            
            
            new ChartInfo(ElementType.POSITION_DIFF, "POSITION_DIFF", "Position difference (km)", "Position difference", 0.0, true, false),
            new ChartInfo(ElementType.VELOCITY_DIFF, "VELOCITY_DIFF", "Velocity difference (km/s)", "Velocity difference", 0.0, true, false),
            new ChartInfo(ElementType.OCCURENCE_DIFF, "OCCURRENCE_DIFF", "Event occurrence difference (s)", "Event occurrence difference", 0.0, false, false, "Number of events detected"),
            new ChartInfo(ElementType.QUATERNION_DIFFERENCE, "QUATERNION_DIFF", "Quaternion angle difference (deg)", "Quaternion angle difference", 360.0, true, true),
        };
    }

    /**
     * Standard constructor.
     *
     * @param timeAxisType
     *            the type of he time axis. Can be one of 'seconds', 'hours' or 'days'
     * @param logger the logger
     * @param outputFolder the folder where the images should be saved
     * @param simulationName the name of the simulation
     */
    public ChartDataHolder(final String timeAxisType, final Logger logger, final File outputFolder, final String simulationName) {
        this.timeAxisType = timeAxisType;
        ChartDataHolder.logger = logger;
        this.outputFolder = outputFolder;
        this.simulationName = simulationName;
        this.chartSeries = new XYSeriesCollection[ElementType.TOTAL];

        for (int i = 0; i < ElementType.TOTAL; i++) {
            this.chartSeries[i] = new XYSeriesCollection();
        }
    }

    /**
     * Parse a list of {@link SpacecraftState states} that represent the result of a propagation.
     *
     * @param states
     *            the list of {@link SpacecraftState states} elements obtained after a
     *            propagation
     * @param start
     *            the propagation start date
     * @param seriesName
     *               the name of the series
     * @param targetFrame the frame in which the PV must be represented.
     * @throws OrekitException in case of an error
     */
    public void parseOrbitList(final List<SpacecraftState> states,
            final AbsoluteDate start, final String seriesName, final Frame targetFrame) throws OrekitException {

        final XYSeries[] series = new XYSeries[ElementType.TOTAL];
        for (int i = 0; i < ElementType.TOTAL; i++) {
            series[i] = new XYSeries(seriesName);
        }

        final IERSConventions conventions = IERSConventions.IERS_2010;
        final UT1Scale ut1 = TimeScalesFactory.getUT1(conventions, true);
        //timescalar function from timefunction
        final TimeScalarFunction gmstfunction = conventions.getGMSTFunction(ut1);
        final UTCScale utc = TimeScalesFactory.getUTC();
        //final Frame earthFrame = earth.getBodyFrame();

        for (SpacecraftState s : states) {
            Orbit o = s.getOrbit();
            
            // Time from start (s)
            final double time = convertTime(o.getDate().durationFrom(start));

            // Semi-major axis (km)
            final double a = o.getA() / 1000.;
            series[ElementType.A].add(time,  a);

            // Keplerian elements
            // Eccentricity
            final double e = o.getE();
            series[ElementType.E].add(time,  e);

            // Inclination (degrees)
            final double i = Math.toDegrees(MathUtils.normalizeAngle(o.getI(),
                    FastMath.PI));
            series[ElementType.I].add(time,  i);

            // Right Ascension of Ascending Node (degrees)
            final KeplerianOrbit ko = new KeplerianOrbit(o);
            final double ra = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getRightAscensionOfAscendingNode(), FastMath.PI));
            series[ElementType.RAAN].add(time,  ra);

            // Perigee Argument (degrees)
            final double pa = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getPerigeeArgument(), FastMath.PI));
            series[ElementType.PA].add(time,  pa);

            // Mean Anomaly (degrees)
            final double ma = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getAnomaly(PositionAngle.MEAN), FastMath.PI));
            series[ElementType.MA].add(time,  ma);

            // Equinoctial elements
            // ey/h component of eccentricity vector
            final double h = o.getEquinoctialEy();
            series[ElementType.H].add(time,  h);

            // ex/k component of eccentricity vector
            final double k = o.getEquinoctialEx();
            series[ElementType.K].add(time,  k);

            // hy/p component of inclination vector
            final double p = o.getHy();
            series[ElementType.P].add(time,  p);

            // hx/q component of inclination vector
            final double q = o.getHx();
            series[ElementType.Q].add(time,  q);

            // Mean Longitude Argument (degrees)
            final double lm = Math.toDegrees(MathUtils.normalizeAngle(o.getLM(), FastMath.PI));
            series[ElementType.LM].add(time,  lm);

            // Local time of ascending node
            final double t = o.getDate().getComponents(utc).getTime().getSecondsInUTCDay();
            //deleted get value
            final double gmst = gmstfunction.value(o.getDate());
            final double sunAlpha = gmst + FastMath.PI * (1 - t / (Constants.JULIAN_DAY * 0.5));
            // angular offset between the two positions
            final double dAlpha = MathUtils.normalizeAngle(ko.getRightAscensionOfAscendingNode() - sunAlpha, 0);
            series[ElementType.LTAN].add(time, 12.0 * (1.0 + dAlpha / FastMath.PI));

            //Argument of perigee plus mean anomaly
            final double pa_ma = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getPerigeeArgument() + ko.getAnomaly(PositionAngle.MEAN),
                    FastMath.PI));
            series[ElementType.PA_MA].add(time,  pa_ma);

            //the altitude at perigee
            final double palt = a * (1 - e) - Constants.EGM96_EARTH_EQUATORIAL_RADIUS / 1000.0;
            series[ElementType.PALT].add(time, palt);

            //the altitude at apogee
            final double aalt = a * (1 + e) - Constants.EGM96_EARTH_EQUATORIAL_RADIUS / 1000.0;
            series[ElementType.AALT].add(time, aalt);

            //Right ascension of ascending node plus argument of perigee
            final double raan_pa = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getRightAscensionOfAscendingNode() + ko.getPerigeeArgument(),
                    FastMath.PI));
            series[ElementType.RAAN_PA].add(time,  raan_pa);

            //Right ascension of ascending node plus argument of perigee
            final double raan_pa_ma = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getRightAscensionOfAscendingNode() + ko.getPerigeeArgument() + ko.getAnomaly(PositionAngle.MEAN),
                    FastMath.PI));
            series[ElementType.RAAN_PA_MA].add(time,  raan_pa_ma);

            // Get the position/velocity elements
            TimeStampedPVCoordinates pvCoords = null;
            if (targetFrame != null) {
                pvCoords = o.getPVCoordinates(targetFrame);
            } else {
                pvCoords = o.getPVCoordinates();
            }
            //Get the size of the position vector (Km)
            final Vector3D position = pvCoords.getPosition();
            series[ElementType.POSITION].add(time, position.getNorm() / 1000.);

            //Get the coordinates of the position vector (Km)
            series[ElementType.POSITION_X].add(time, position.getX() / 1000.);
            series[ElementType.POSITION_Y].add(time, position.getY() / 1000.);
            series[ElementType.POSITION_Z].add(time, position.getZ() / 1000.);

            //Get the size of the speed vector (Km/s)
            final Vector3D velocity = pvCoords.getVelocity();
            series[ElementType.VELOCITY].add(time, velocity.getNorm() / 1000.);

            //Get the coordinates of the velocity vector (Km/s)
            series[ElementType.VELOCITY_X].add(time, velocity.getX() / 1000.);
            series[ElementType.VELOCITY_Y].add(time, velocity.getY() / 1000.);
            series[ElementType.VELOCITY_Z].add(time, velocity.getZ() / 1000.);
            
            // Add the Attitude data
            Rotation r = s.getAttitude().getRotation();
            series[ElementType.Q0].add(time, r.getQ0());
            series[ElementType.Q1].add(time, r.getQ1());
            series[ElementType.Q2].add(time, r.getQ2());
            series[ElementType.Q3].add(time, r.getQ3());
            series[ElementType.ANGLE].add(time, FastMath.toDegrees(r.getAngle()));

            // get the Euler angles. For singularities use the 0 value
            double[] angles;
            try {
            	angles = r.getAngles(RotationOrder.ZYX, RotationConvention.FRAME_TRANSFORM);
            } catch (Exception ex) {
            	angles = new double[] {0, 0, 0};
            }
            
            series[ElementType.YAW].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[0], FastMath.PI)));
            series[ElementType.PITCH].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[1], FastMath.PI)));
            series[ElementType.ROLL].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[2], FastMath.PI)));

            Vector3D spin = s.getAttitude().getSpin();
            series[ElementType.ROTATION_RATE_X].add(time, spin.getX());
            series[ElementType.ROTATION_RATE_Y].add(time, spin.getY());
            series[ElementType.ROTATION_RATE_Z].add(time, spin.getZ());
        
            Vector3D rotAcc = s.getAttitude().getRotationAcceleration();
            series[ElementType.ROTATION_ACCELERATION_X].add(time, rotAcc.getX());
            series[ElementType.ROTATION_ACCELERATION_Y].add(time, rotAcc.getY());
            series[ElementType.ROTATION_ACCELERATION_Z].add(time, rotAcc.getZ());
        }

        /* add the series to the collection if and only if they contain some data*/
        for (int i = 0; i < ElementType.TOTAL; i++) {
            if (series[i].getItemCount() > 0) {
                this.chartSeries[i].addSeries(series[i]);
            }
        }
    }

    /**
     * Parse a list of {@link TimeStampedPVCoordinates coordinates} that represent the result of a propagation.
     *
     * @param coordinates
     *            the list of {@link TimeStampedPVCoordinates coordinates} elements obtained after a
     *            propagation
     * @param start
     *            the propagation start date
     * @param seriesName
     *               the name of the series
     * @throws OrekitException in case of an error
     */
    public void parseCoordinatesList(final List<TimeStampedPVCoordinates> coordinates,
            final AbsoluteDate start, final String seriesName) throws OrekitException {

        final XYSeries[] series = new XYSeries[ElementType.TOTAL];
        for (int i = 0; i < ElementType.TOTAL; i++) {
            series[i] = new XYSeries(seriesName);
        }

        for (TimeStampedPVCoordinates pvCoords : coordinates) {
            // Time from start (s)
            final double time = convertTime(pvCoords.getDate().durationFrom(start));
            //Get the size of the position vector (Km)
            final Vector3D position = pvCoords.getPosition();
            series[ElementType.POSITION].add(time, position.getNorm() / 1000.);

            //Get the coordinates of the position vector (Km)
            series[ElementType.POSITION_X].add(time, position.getX() / 1000.);
            series[ElementType.POSITION_Y].add(time, position.getY() / 1000.);
            series[ElementType.POSITION_Z].add(time, position.getZ() / 1000.);

            //Get the size of the speed vector (Km/s)
            final Vector3D velocity = pvCoords.getVelocity();
            series[ElementType.VELOCITY].add(time, velocity.getNorm() / 1000.);

            //Get the coordinates of the velocity vector (Km/s)
            series[ElementType.VELOCITY_X].add(time, velocity.getX() / 1000.);
            series[ElementType.VELOCITY_Y].add(time, velocity.getY() / 1000.);
            series[ElementType.VELOCITY_Z].add(time, velocity.getZ() / 1000.);
        }

        /* add the series to the collection if and only if they contain some data*/
        for (int i = 0; i < ElementType.TOTAL; i++) {
            if (series[i].getItemCount() > 0) {
                this.chartSeries[i].addSeries(series[i]);
            }
        }
    }
    
    
    
     /**
     * Create the series containing the size of the position and velocity difference vectors.
     *
     * @param orekitStates
     *            the list of {@link SpacecraftState states} obtained after the Orekit
     *            propagation
     * @param orbiproCoords
     *            the list of {@link TimeStampedPVCoordinates coordinates} obtained after parsing 
     *            the Orbipro file
     * @param start
     *            the propagation start date
     * @param seriesName
     *               the name of the series
     * @return    the maximum absolute difference between the two vectors
     */
    public double[][] createVectorsDiff(final List<TimeStampedPVCoordinates> orekitCoords, final List<TimeStampedPVCoordinates> orbiproCoords,
            final AbsoluteDate start, final String seriesName) {

        final double[][] max = new double[][] {{0., 0.}, {0., 0.}, {0., -1}};
        
        final int size = FastMath.min(orekitCoords.size(), orbiproCoords.size());

        final XYSeries[] series = new XYSeries[3];
        for (int i = 0; i < 3; i++) {
            series[i] = new XYSeries(seriesName);
        }

        for (int i = 0; i < size; i++) {
            final TimeStampedPVCoordinates tspvOrekit = orekitCoords.get(i);
            final TimeStampedPVCoordinates tspvOrbipro = orbiproCoords.get(i);

            // Time from start (s)
            final double time = convertTime(tspvOrekit.getDate().durationFrom(start));

            // get the Position vectors
            final Vector3D posOrekit = tspvOrekit.getPosition();
            final Vector3D posOrbipro = tspvOrbipro.getPosition();

            // compute the difference
            final Vector3D posDiff = posOrekit.subtract(posOrbipro);
            final double posDiffSize = posDiff.getNorm() / 1000.;
            if (FastMath.abs(posDiffSize) > max[0][0]) {
                max[0][0] = FastMath.abs(posDiffSize);
            }
            if (FastMath.abs(posDiffSize) / posOrekit.getNorm() > max[0][1]) {
                max[0][1] = FastMath.abs(posDiffSize) / posOrekit.getNorm();
            }
            //Get the size of the difference position vector (Km)
            series[0].add(time, posDiffSize);

            // get the Velocity vectors
            final Vector3D velOrekit = tspvOrekit.getVelocity();
            final Vector3D velOrbipro = tspvOrbipro.getVelocity();

            // compute the difference
            final Vector3D velDiff = velOrekit.subtract(velOrbipro);
            final double velDiffSize = velDiff.getNorm() / 1000.;
            if (FastMath.abs(velDiffSize) > max[1][0]) {
                max[1][0] = FastMath.abs(velDiffSize);
            }
            if (FastMath.abs(velDiffSize) / velOrekit.getNorm() > max[1][1]) {
                max[1][1] = FastMath.abs(velDiffSize) / velOrekit.getNorm();
            }
            //Get the size of the difference velocity vector (Km/s)
            series[1].add(time, velDiffSize);
            
            //Add the time difference of the occurence moments.
            final double timeDiff = tspvOrekit.getDate().durationFrom(tspvOrbipro.getDate());
            if (FastMath.abs(timeDiff) > max[2][0]) {
                max[2][0] = FastMath.abs(timeDiff);
            }
            series[2].add(i+1, timeDiff);

        }
        /* add the series to the collection if and only if they contain some data*/
        if (size > 0) {
            this.chartSeries[ElementType.POSITION_DIFF].addSeries(series[0]);
            this.chartSeries[ElementType.VELOCITY_DIFF].addSeries(series[1]);
            this.chartSeries[ElementType.OCCURENCE_DIFF].addSeries(series[2]);
        }

        return max;
    }

    
    /**
    * Create the series containing the size of the position and velocity difference vectors.
    *
    * @param orekitRotations
    *            the list of {@link Rotation rotations} obtained after the Orekit
    *            propagation
    * @param orbiproRotations
    *            the list of {@link Rotation rotations} obtained after parsing 
    *            the Orbipro file
    * @param dates
    *            dates of the rotations. 
    * @param start
    *            the propagation start date
    * @param seriesName
    *               the name of the series
    * @return    the maximum absolute difference between the two vectors
    */
   public double[][] createRotationsDiff(final List<Rotation> orekitRotations, final List<Rotation> orbiproRotations,
           final List<AbsoluteDate> dates, final AbsoluteDate start, final String seriesName) {

       final double[][] max = new double[][] {{0., 0.}};
       
       final int size = FastMath.min(orekitRotations.size(), orbiproRotations.size());

       final XYSeries[] series = new XYSeries[1];
       for (int i = 0; i < 1; i++) {
           series[i] = new XYSeries(seriesName);
       }

       for (int i = 0; i < size; i++) {
           final Rotation rOrekit = orekitRotations.get(i);
           final Rotation rOrbipro = orbiproRotations.get(i);

           // Time from start (s)
           final double time = convertTime(dates.get(i).durationFrom(start));

           // Compute the rotation difference
           final double difference = FastMath.toDegrees(Rotation.distance(rOrekit, rOrbipro));
           
           series[0].add(time, difference);

           if (FastMath.abs(difference) > max[0][0]) {
               max[0][0] = FastMath.abs(difference);
           }
           if (FastMath.abs(difference) / FastMath.toDegrees(rOrekit.getAngle()) > max[0][1]) {
               max[0][1] = FastMath.abs(difference) / FastMath.toDegrees(rOrekit.getAngle());
           }
       }
       /* add the series to the collection if and only if they contain some data*/
       if (size > 0) {
           this.chartSeries[ElementType.QUATERNION_DIFFERENCE].addSeries(series[0]);
       }

       return max;
   }

    
    
    /** Parse a reference file.
     *
     * @param dataFile the file to parse
     * @param seriesName the name of the series
     * @param start the start date for the calculus
     * @return a list of time stamped PV coordinates and rotations
     * @throws IOException if a file related error occurs
     * @throws OrekitException 
     */
    public FileDataHolder parseDataFile(final File dataFile, final String seriesName, final AbsoluteDate start) throws IOException, OrekitException {
        
        final FileDataHolder result = new FileDataHolder();
        
        
        final XYSeries[] series = new XYSeries[ElementType.TOTAL];
        for (int i = 0; i < ElementType.TOTAL; i++) {
            series[i] = new XYSeries(seriesName);
        }
        
        final IERSConventions conventions = IERSConventions.IERS_2010;
        final UT1Scale ut1 = TimeScalesFactory.getUT1(conventions, true);
        final TimeScalarFunction gmstfunction = conventions.getGMSTFunction(ut1);
        final UTCScale utc = TimeScalesFactory.getUTC();
        
        
        //Open the file for line-by-line read mode

        try(final BufferedReader br = new BufferedReader( new FileReader(dataFile))) {
            String line;
            int lineNumber = 0;
            int currentIndex = -1;

            //Read the entire file
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.startsWith("#")) {
                    // skip comments
                    continue;
                }

                //Extract tokens
                final StringTokenizer st = new StringTokenizer(line);
                if (st.countTokens() != ElementType.TOTAL_FILE_FIELDS + 1) {
                    throw new IOException("Invalid file. Line number " + lineNumber + " does not contain " + (ElementType.TOTAL_FILE_FIELDS + 1) + " tokens!");
                }
                currentIndex++;

                // Time from start (s)
                final double seconds = Double.parseDouble(st.nextToken().replace('D', 'e'));
                double time = convertTime(seconds);

                // Extract the rest of the values.
                boolean stopLoop = false;
                for (int cnt = 0; cnt < ElementType.TOTAL_FILE_FIELDS; cnt++) {
                    final String strVal = st.nextToken().replace('D', 'e').trim();
                    if (strVal.compareToIgnoreCase("nan") == 0) {
                        // add a log info
                    	logger.warn("NaN found in file " + dataFile.getName() + " at line " + lineNumber + "!");
                    	//stop reading the file
                        stopLoop = true;
                        break;
                    }
                    final double value = Double.parseDouble(strVal);
                    series[cnt].add(time,  value);
                }

                if (stopLoop) {
                    //exit the while loop too
                    break;
                }
                
                // create an instance of Rotation and save it to the list
                Rotation r = new Rotation(series[ElementType.Q0].getY(currentIndex).doubleValue(), 
                                            series[ElementType.Q1].getY(currentIndex).doubleValue(),
                                            series[ElementType.Q2].getY(currentIndex).doubleValue(),
                                            series[ElementType.Q3].getY(currentIndex).doubleValue(),
                                            false);
                result.addRotation(r);
                series[ElementType.ANGLE].add(time, FastMath.toDegrees(r.getAngle()));
                
                // get the Euler angles. For singularities use the 0 value
                double[] angles;
                try {
                	angles = r.getAngles(RotationOrder.ZYX, RotationConvention.FRAME_TRANSFORM);
                } catch (Exception e) {
                	angles = new double[] {0, 0, 0};
                }
                
                series[ElementType.YAW].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[0], FastMath.PI)));
                series[ElementType.PITCH].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[1], FastMath.PI)));
                series[ElementType.ROLL].add(time, FastMath.toDegrees(MathUtils.normalizeAngle(angles[2], FastMath.PI)));
                
                // Add the values for the special charts
                final AbsoluteDate date = new AbsoluteDate(start, seconds);
                final double a = series[ElementType.A].getY(currentIndex).doubleValue();
                final double e = series[ElementType.E].getY(currentIndex).doubleValue();
                final double raan = FastMath.toRadians(series[ElementType.RAAN].getY(currentIndex).doubleValue());
                final double pa = FastMath.toRadians(series[ElementType.PA].getY(currentIndex).doubleValue());
                final double ma = FastMath.toRadians(series[ElementType.MA].getY(currentIndex).doubleValue());
                
                final double posX = series[ElementType.POSITION_X].getY(currentIndex).doubleValue() * 1000.;
                final double posY = series[ElementType.POSITION_Y].getY(currentIndex).doubleValue() * 1000.;
                final double posZ = series[ElementType.POSITION_Z].getY(currentIndex).doubleValue() * 1000.;
                final double velX = series[ElementType.VELOCITY_X].getY(currentIndex).doubleValue() * 1000.;
                final double velY = series[ElementType.VELOCITY_Y].getY(currentIndex).doubleValue() * 1000.;
                final double velZ = series[ElementType.VELOCITY_Z].getY(currentIndex).doubleValue() * 1000.;
                
                // Local time of ascending node
                final double t = date.getComponents(utc).getTime().getSecondsInUTCDay();
                final double gmst = gmstfunction.value(date);
                final double sunAlpha = gmst + FastMath.PI * (1 - t / (Constants.JULIAN_DAY * 0.5));
                // angular offset between the two positions
                final double dAlpha = MathUtils.normalizeAngle(raan - sunAlpha, 0);
                series[ElementType.LTAN].add(time, 12.0 * (1.0 + dAlpha / FastMath.PI));

                //Argument of perigee plus mean anomaly
                final double pa_ma = Math.toDegrees(MathUtils.normalizeAngle(
                        pa + ma,
                        FastMath.PI));
                series[ElementType.PA_MA].add(time,  pa_ma);

                //the altitude at perigee
                final double palt = a * (1 - e) - Constants.EGM96_EARTH_EQUATORIAL_RADIUS / 1000.0;
                series[ElementType.PALT].add(time, palt);

                //the altitude at apogee
                final double aalt = a * (1 + e) - Constants.EGM96_EARTH_EQUATORIAL_RADIUS / 1000.0;
                series[ElementType.AALT].add(time, aalt);

                //Right ascension of ascending node plus argument of perigee
                final double raan_pa = Math.toDegrees(MathUtils.normalizeAngle(
                        raan + pa,
                        FastMath.PI));
                series[ElementType.RAAN_PA].add(time,  raan_pa);

                //Right ascension of ascending node plus argument of perigee
                final double raan_pa_ma = Math.toDegrees(MathUtils.normalizeAngle(
                        raan + pa + ma,
                        FastMath.PI));
                series[ElementType.RAAN_PA_MA].add(time,  raan_pa_ma);
                
                //Get the size of the position vector (Km)
                final Vector3D position = new Vector3D(posX, posY, posZ);
                series[ElementType.POSITION].add(time, position.getNorm() / 1000.);

                //Get the size of the velocity vector (Km/s)
                final Vector3D velocity = new Vector3D(velX, velY, velZ);
                series[ElementType.VELOCITY].add(time, velocity.getNorm() / 1000.);
                
                // create an instance of TimeStampedPV coordinates and save it to the list
                result.addCoordinates(new TimeStampedPVCoordinates(date, position, velocity));
            }
        }
        
        /* add the series to the collection if and only if they contain some data*/
        for (int i = 0; i < ElementType.TOTAL; i++) {
            if (series[i].getItemCount() > 0) {
                this.chartSeries[i].addSeries(series[i]);
            }
        }
        
        return result;
    }

    
    /** Parse a reference file that contains local data.
    *
    * @param dataFile the file to parse
    * @param seriesName the name of the series
    * @param start the start date for the calculus
    * @return a list of time stamped PV coordinates 
    * @throws IOException if a file related error occurs
    * @throws OrekitException 
    */
   public FileDataHolder parseLocalDataFile(final File dataFile, final String seriesName, final AbsoluteDate start) throws IOException, OrekitException {
       
       final FileDataHolder result = new FileDataHolder();
       
       
       final XYSeries[] series = new XYSeries[ElementType.TOTAL];
       for (int i = 0; i < ElementType.TOTAL; i++) {
           series[i] = new XYSeries(seriesName);
       }
       
       //Open the file for line-by-line read mode

       try(final BufferedReader br = new BufferedReader( new FileReader(dataFile))) {
           String line;
           int lineNumber = 0;
           int currentIndex = -1;

           //Read the entire file
           while ((line = br.readLine()) != null) {
               lineNumber++;
               line = line.trim();
               if (line.startsWith("#")) {
                   // skip comments
                   continue;
               }

               //Extract tokens
               final StringTokenizer st = new StringTokenizer(line);
               if (st.countTokens() != ElementType.TOTAL_LOCAL_FILE_FIELDS + 1) {
                   throw new IOException("Invalid file. Line number " + lineNumber + " does not contain " + (ElementType.TOTAL_LOCAL_FILE_FIELDS + 1) + " tokens!");
               }
               currentIndex++;

               // Time from start (s)
               final double seconds = Double.parseDouble(st.nextToken().replace('D', 'e'));
               double time = convertTime(seconds);

               // Extract the rest of the values.
               boolean stopLoop = false;
               for (int cnt = 11; cnt < 11 + ElementType.TOTAL_LOCAL_FILE_FIELDS; cnt++) {
                   final String strVal = st.nextToken().replace('D', 'e').trim();
                   if (strVal.compareToIgnoreCase("nan") == 0) {
                       // add a log info
                	   logger.warn("NaN found in file " + dataFile.getName() + " at line " + lineNumber + "!");
                       //stop reading the file
                       stopLoop = true;
                       break;
                   }
                   final double value = Double.parseDouble(strVal);
                   series[cnt].add(time,  value);
               }

               if (stopLoop) {
                   //exit the while loop too
                   break;
               }
               
               // Add the values for the special charts
               final AbsoluteDate date = new AbsoluteDate(start, seconds);
               
               final double posX = series[ElementType.POSITION_X].getY(currentIndex).doubleValue() * 1000.;
               final double posY = series[ElementType.POSITION_Y].getY(currentIndex).doubleValue() * 1000.;
               final double posZ = series[ElementType.POSITION_Z].getY(currentIndex).doubleValue() * 1000.;
               final double velX = series[ElementType.VELOCITY_X].getY(currentIndex).doubleValue() * 1000.;
               final double velY = series[ElementType.VELOCITY_Y].getY(currentIndex).doubleValue() * 1000.;
               final double velZ = series[ElementType.VELOCITY_Z].getY(currentIndex).doubleValue() * 1000.;
               
               
               //Get the size of the position vector (Km)
               final Vector3D position = new Vector3D(posX, posY, posZ);
               series[ElementType.POSITION].add(time, position.getNorm() / 1000.);

               //Get the size of the velocity vector (Km/s)
               final Vector3D velocity = new Vector3D(velX, velY, velZ);
               series[ElementType.VELOCITY].add(time, velocity.getNorm() / 1000.);
               
               // create an instance of TimeStampedPV coordinates and save it to the list
               result.addCoordinates(new TimeStampedPVCoordinates(date, position, velocity));
           }
       }
       
       /* add the series to the collection if and only if they contain some data*/
       for (int i = 0; i < ElementType.TOTAL; i++) {
           if (series[i].getItemCount() > 0) {
               this.chartSeries[i].addSeries(series[i]);
           }
       }
       
       return result;
   }

    
    /**
     * Build the charts and save them as images.
     *
     * @throws IOException if the images cannot be created
     */
    public void createCharts()
        throws IOException {

        for (int i = 0; i < ElementType.TOTAL; i++) {
            createChart(ChartDataHolder.chartInfos[i]);
        }
    }

    /** Add the maximum difference to the log.
     *
     * @param label the label
     * @param max the maximum difference
     * @param degree if true, the degree character is also printed
     */
    public void logMaxDiff(final String label, final double max[], final boolean degree) {
        NumberFormat nf = null;

        if (max[0] < 0.001) {
            nf = new DecimalFormat("0.000E0");
        } else {
            nf = new DecimalFormat("#0.000");
        }

        //Print the maximum absolute error to the console if this is a difference plot
        String logMessage = label + ": " + nf.format(max[0]);
        if (degree) {
            logMessage += "\u00B0";
        }
        logger.info(logMessage);

        if (max[1] >= 0) {
            if (max[1] / 100. < 0.001) {
                nf = new DecimalFormat("0.000E0");
            } else {
                nf = new DecimalFormat("#0.000");
            }

            //Print the maximum absolute error to the console if this is a difference plot
            logMessage = label + ": " + nf.format(max[1] * 100.) + "%";
            logger.info(logMessage);
        }

    }

    /**
     * Create a chart and save it as a png file.
     *
     * @param chartInfo the chart information
     * @throws IOException if the chart cannot be saved to the image file
     */
    private void createChart(final ChartInfo chartInfo) throws IOException {

        // build the main plot
        final CombinedDomainXYPlot chartPlot = createMainPlot(chartInfo);
        if (chartPlot == null) {
            // the chart cannot be created
            logger.warn("Cannot create the chart " + chartInfo.title + "!");
            return;
        }

        // get the data set
        final XYSeriesCollection dataSet = this.chartSeries[chartInfo.getElementID()];

        // add the difference plots
        for (int i = 0; i < dataSet.getSeriesCount() - 1; i++) {
            final XYSeries refference = dataSet.getSeries(i);
            if (refference != null && refference.getItemCount() > 0) {
                for (int j = i + 1; j < dataSet.getSeriesCount(); j++) {
                    final XYSeries s = dataSet.getSeries(j);
                    if (s != null) {
                        //add the difference between the two series
                        final double[] max = addDifferencePlot(chartPlot, refference, s, new Color(0, 0, 0), chartInfo.getBoundary());

                        // add the maximum difference to the log
                        logMaxDiff(chartInfo.getTitle(), max, chartInfo.isDegree());
                    }
                }
            }
        }

        //save the chart to file
        saveChart(chartInfo, chartPlot);
    }

    /**
     * Create the main plot area for a chart.
     *
     * @param chartInfo the chart information
     * @return the plot area.
     */
    private CombinedDomainXYPlot createMainPlot(final ChartInfo chartInfo) {

        // get the data set to use
        final XYSeriesCollection dataSet = this.chartSeries[chartInfo.getElementID()];
        if (dataSet.getSeriesCount() == 0) {
            // no data available, no chart can be created
            return null;
        }

        //Build a plot that allows multiple sub-plots that share the time axis
        final NumberAxis domainAxis = new NumberAxis(chartInfo.getxAxisLabel() == null ? this.timeAxisType : chartInfo.getxAxisLabel());
        domainAxis.setAutoRangeIncludesZero(false);
        final CombinedDomainXYPlot chartPlot = new CombinedDomainXYPlot(domainAxis);
        chartPlot.setOrientation(PlotOrientation.VERTICAL);

        //Create the renderer for the data set
        XYItemRenderer mainItemRenderer;
        if (chartInfo.isLineChart()) {
            // create a standard line renderer
            mainItemRenderer = new StandardXYItemRenderer();
        } else {
            // create a dot renderer
            mainItemRenderer = new XYDotRenderer();
            ((XYDotRenderer) mainItemRenderer).setDotWidth(3);
            ((XYDotRenderer) mainItemRenderer).setDotHeight(3);
        }
        //Create the range axis for the dataSet
        final NumberAxis mainRangeAxis = new NumberAxis(chartInfo.getLabel());

        //Update the range axis bounds
        final Range r = dataSet.getRangeBounds(true);
        double length = r.getLength();
        double minValue = r.getLowerBound();
        double maxValue = r.getUpperBound();

        // Set the minimum length to twice the smallest printable label
        //length = FastMath.max(length, 2.0e-6);
        if (length == 0) {
            length = 2.0e-6;
        }

        minValue = minValue - length * 0.1;
        maxValue = maxValue + length * 0.1;

        mainRangeAxis.setRange(new Range(minValue, maxValue));

        //Create the sub-plot that will contain the data sets
        final XYPlot mainPlot = new XYPlot(dataSet, null, mainRangeAxis, mainItemRenderer);

        //Set the colors for the grid lines
        mainPlot.setDomainGridlinePaint(Color.DARK_GRAY);
        mainPlot.setRangeGridlinePaint(Color.DARK_GRAY);

        //add the sub-plot to the chart plot
        chartPlot.add(mainPlot, 1);

        return chartPlot;
    }

    /**
     * Save a chart to file.
     *
     * @param chartInfo the chart info
     * @param chartPlot the plot to save
     * @throws IOException if the image file cannot be created
     */
    private void saveChart(final ChartInfo chartInfo, final CombinedDomainXYPlot chartPlot) throws IOException {

        // compute the chart dimension
        final int width = CHARTWIDTH;
        final int height = CHARTHEIGHT * chartPlot.getSubplots().size();

        // build the chart
        final JFreeChart chart = new JFreeChart(chartInfo.getTitle(), JFreeChart.DEFAULT_TITLE_FONT, chartPlot, false);

        //Position the main legend to the right
        final LegendTitle legend = new LegendTitle((XYPlot) chartPlot.getSubplots().get(0));
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setVerticalAlignment(VerticalAlignment.TOP);
        legend.setMargin(100.0, 5.0, 5.0, 10.0);
        legend.setBackgroundPaint(Color.WHITE);
        legend.setFrame(new BlockBorder(Color.BLACK));

        final BlockContainer blockcontainer = new BlockContainer(new BorderArrangement());
        blockcontainer.add(legend, RectangleEdge.TOP);

        blockcontainer.add(new EmptyBlock(0.0, height));
        final CompositeTitle compositetitle = new CompositeTitle(blockcontainer);
        compositetitle.setPosition(RectangleEdge.RIGHT);
        chart.addSubtitle(compositetitle);

        //save the chart to file (maximum quality)
        final File output = new File(this.outputFolder, this.simulationName + "_" + chartInfo.getFileNameSuffix() + ".png");
        ChartUtilities.saveChartAsPNG(output, chart, width, height);
    }

    /** Add a difference plot to the chart.
     *
     * @param chartPlot the main plot of the chart
     * @param mainSeries the first series
     * @param secondarySeries the second series
     * @param color the color of the plot
     * @param boundary the boundary value
     * @return the maximum absolute difference between the two series as well as the maximum relative error.
     */
    private double[] addDifferencePlot(final CombinedDomainXYPlot chartPlot, final XYSeries mainSeries, final XYSeries secondarySeries,
            final Color color, final double boundary) {
        //Create the series to display
        final String label = "\u0394 = " + mainSeries.getKey() + " - " + secondarySeries.getKey();

        // Build the difference data
        final XYSeries diff = new XYSeries(label);
        double value;
        double[] max =  {0., 0.};
        for (int i = 0; i < mainSeries.getItemCount(); i++) {
            final XYDataItem mainItem = mainSeries.getDataItem(i);
            if (secondarySeries.getItemCount() > i) {
                final XYDataItem secondaryItem = secondarySeries.getDataItem(i);
                value = mainItem.getYValue() - secondaryItem.getYValue();

                if (boundary != 0) {
                    // Remove the 0.0 <-> boundary errors
                    if (value >= 0) {
                        if (FastMath.abs(value) > FastMath.abs(value - boundary)) {
                            value -= boundary;
                        }
                    } else {
                        if (FastMath.abs(value) > FastMath.abs(value + boundary)) {
                            value += boundary;
                        }
                    }
                }
                // compute the maximum absolute difference 
                if (max[0] < FastMath.abs(value)) {
                    max[0] = FastMath.abs(value);
                }
                // compute the maximum relative difference 
                if (mainItem.getYValue() != 0.0) {
                    if (max[1] < FastMath.abs(value / mainItem.getYValue())) {
                        max[1] = FastMath.abs(value / mainItem.getYValue());
                    }
                }
                diff.add(mainItem.getXValue(), value);
            }
        }

        // build the range for the data
        // Ensure at least the minimum tick is visible
        final double axisMax = FastMath.max(max[0], 1.0e-20);
        final Range displayRange = new Range(-axisMax * 1.2, axisMax * 1.2);

        //create the plot
        final XYPlot diffPlot = createSecondaryPlot(new XYSeriesCollection(diff), displayRange, max, label, color);

        //add the difference plot to the main plot
        chartPlot.add(diffPlot, 1);
        return max;
    }

    /**
     * Create a plot that will be displayed under the main plot.
     *
     * @param diffSeries the data to display in the plot
     * @param displayRange the range enforced for the data
     * @param maxDifference the maximum absolute and relative differences to be displayed in an annotation
     * @param label the label for the data
     * @param color the color used to draw the line
     * @return the secondary plot
     */
    private XYPlot createSecondaryPlot(final XYSeriesCollection diffSeries, final Range displayRange,
            final double[] maxDifference, final String label, final Color color) {

        //Create the renderer for the data set
        final StandardXYItemRenderer diffItemRenderer = new StandardXYItemRenderer();
        //set the series color
        diffItemRenderer.setSeriesPaint(0, color);
        //remove series from legend
        diffItemRenderer.setSeriesVisibleInLegend(0, false);
        //Create the range axis for the dataSet
        final NumberAxis diffRangeAxis = new NumberAxis(label);
        diffRangeAxis.setLabelFont(getAxisFont(diffRangeAxis.getLabelFont(), label));
        //diffRangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits(Locale.ENGLISH));

        //Update the range axis bounds
        diffRangeAxis.setRange(displayRange);

        //Create the sub-plot that will contain the data sets
        final XYPlot diffPlot = new XYPlot(diffSeries, null, diffRangeAxis, diffItemRenderer);

        //Set the colors for the grid lines
        diffPlot.setDomainGridlinePaint(Color.DARK_GRAY);
        diffPlot.setRangeGridlinePaint(Color.DARK_GRAY);

        //Add horizontal line at 0.0
        final ValueMarker valuemarker = new ValueMarker(0.0);
        final BasicStroke dashed =
                new BasicStroke(1.0f,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER,
                                10.0f, new float[]{2.0f}, 0.0f);
        valuemarker.setStroke(dashed);
        valuemarker.setPaint(Color.BLACK);
        diffPlot.addRangeMarker(valuemarker);

        //Create an annotation that contains the maximum absolute error
        final BigDecimal bdA = new BigDecimal(maxDifference[0], MathContext.DECIMAL64);
        String title = "|\u0394max| = " + bdA.setScale(20, RoundingMode.HALF_UP).toEngineeringString();
        if (maxDifference[1] >= 0) {
            final BigDecimal bdR = new BigDecimal(maxDifference[1] * 100., MathContext.DECIMAL64);
            title += "        /       |\u03B4max| = " + bdR.setScale(20, RoundingMode.HALF_UP).toEngineeringString() + " %";
        }
        final TextTitle tt = new TextTitle(title);
        tt.setPosition(RectangleEdge.BOTTOM);

        final XYTitleAnnotation titleAnnotation = new XYTitleAnnotation(0.02, 0.001, tt, RectangleAnchor.BOTTOM_LEFT);

        // Add the annotation to the chart
        diffPlot.addAnnotation(titleAnnotation);

        return diffPlot;
    }

    /** Convert the time from seconds to the value required by the axis type.
     *
     * @param value the initial time, in seconds
     * @return the time expressed as seconds, hours or days.
     */
    private double convertTime(final double value) {
        double result;

        if (this.timeAxisType.equalsIgnoreCase("days")) {
            result = value / 86400.0;
        } else if (this.timeAxisType.equalsIgnoreCase("hours")) {
            result = value / 3600.0;
        } else {
            result = value;
        }

        return result;
    }

    /** Get the font to be used to display the axis label.
     * <p>
     * The size of the font is based on the width of the label text
     * </p>
     *
     * @param currentFont the current font used by the axis
     * @param label the text to display
     * @return the new font
     */
    private Font getAxisFont(final Font currentFont, final String label) {
        Font font = currentFont;
        boolean done = false;
        //build a dummy image to get the graphics context
        final BufferedImage bi = new BufferedImage(CHARTWIDTH, CHARTHEIGHT, BufferedImage.TYPE_INT_ARGB);

        //get the graphics object
        final Graphics2D g2 = bi.createGraphics();
        //get the current font size
        float currentSize = font.getSize2D();

        while (!done) {
            //compute the font metrics
            final FontMetrics fontMetrics = g2.getFontMetrics(font);

            // stop if the text can be displayed in the given height
            if (fontMetrics.stringWidth(label) <= CHARTHEIGHT - 10) {
                done = true;
            } else {
                // decrease font size
                currentSize -= 0.5f;
                font = font.deriveFont(currentSize);
            }
        }
        g2.dispose();
        return font;
    }

    /**
     * The identifier of the keplerian elements.
     */
    private interface ElementType {
        /** The semi-major axis identifier. */
        int A = 0;

        /** The eccentricity identifier. */
        int E = 1;

        /** The inclination identifier. */
        int I = 2;

        /** The right ascension of ascending node identifier. */
        int RAAN = 3;

        /** The Argument of Perigee identifier. */
        int PA = 4;

        /** The mean anomaly identifier. */
        int MA = 5;

        /**    The ey/h component of eccentricity vector. */
        int H = 6;

        /**    The ex/k component of eccentricity vector. */
        int K = 7;

        /**    The hy/p component of inclination vector. */
        int P = 8;

        /**    The hx/q component of inclination vector. */
        int Q = 9;

        /**    The Mean Longitude Argument (degrees). */
        int LM = 10;

        /** The x component of the position vector. */
        int POSITION_X = 11;

        /** The y component of the position vector. */
        int POSITION_Y = 12;

        /** The z component of the position vector. */
        int POSITION_Z = 13;

        /** The x component of the speed vector. */
        int VELOCITY_X = 14;

        /** The y component of the speed vector. */
        int VELOCITY_Y = 15;

        /** The z component of the speed vector. */
        int VELOCITY_Z = 16;
        
        /** The q0 quaternion of the attitude. */
        int Q0 = 17;

        /** The q1 quaternion of the attitude. */
        int Q1 = 18;

        /** The q2 quaternion of the attitude. */
        int Q2 = 19;

        /** The q3 quaternion of the attitude. */
        int Q3 = 20;

        /** The x component of the rotation rate vector. */
        int ROTATION_RATE_X = 21;
        
        /** The y component of the rotation rate vector. */
        int ROTATION_RATE_Y = 22;

        /** The z component of the rotation rate vector. */
        int ROTATION_RATE_Z = 23;

        /** The x component of the rotation acceleration vector. */
        int ROTATION_ACCELERATION_X = 24;
        
        /** The y component of the rotation acceleration vector. */
        int ROTATION_ACCELERATION_Y = 25;

        /** The z component of the rotation acceleration vector. */
        int ROTATION_ACCELERATION_Z = 26;

        
        /** The local time of ascending node. */
        int LTAN = 27;

        /** The sum between the Argument of Perigee and the Mean Anomaly. */
        int PA_MA = 28;

        /** The altitude at perigee. */
        int PALT = 29;

        /** The altitude at apogee. */
        int AALT = 30;

        /** The sum between the Right Ascension of Ascending Node and the Argument of Perigee. */
        int RAAN_PA = 31;

        /** The sum between the Right Ascension of Ascending Node, the Argument of Perigee and the mean anomaly.*/
        int RAAN_PA_MA = 32;

        /** The size of the position vector. */
        int POSITION = 33;

        /** The size of the speed vector. */
        int VELOCITY = 34;

        /** The position difference charts. */
        int POSITION_DIFF = 35;

        /** The velocity difference charts. */
        int VELOCITY_DIFF = 36;

        /** The time occurence difference charts.. */
        int OCCURENCE_DIFF = 37;

        /** The rotation angle.*/
        int ANGLE = 38;
        
        /** The roll angle. */
        int ROLL = 39;
        
        /** The pitch angle. */
        int PITCH = 40;
        
        /** The yaw angle. */
        int YAW = 41;
        
        /** The quaternion difference. */
        int QUATERNION_DIFFERENCE = 42;
        
        /** The number of fields extracted from a normal file. */
        int TOTAL_FILE_FIELDS = 27;

        /** The number of fields extracted from a local frame file. */
        int TOTAL_LOCAL_FILE_FIELDS = 6;
        
        /** The total number of identifiers. */
        int TOTAL = 43;
    }

    /**
     * The information related to the creation of a chart.
     */
    private static class ChartInfo {
        /** The id of the series used. */
        private final int elementID;
        /** The suffix of the file. */
        private final String fileNameSuffix;
        /** The title of the chart. */
        private final String title;
        /** The label used of the data axis. */
        private final String label;
        /** The boundary value used to remove spikes in charts or 0.0 for no boundary. */
        private final double boundary;
        /** True if the chart should be printed as line, false if the chart should be printed as dots. */
        private final boolean lineChart;
        /** True if the parameter is expressed in degrees. */
        private final boolean degree;
        /** The label for the x axis if not related to the time*/
        private final String xAxisLabel;
        
        /** Constructor.
         *
         * @param elementID the id of the series used
         * @param fileNameSuffix the suffix of the file
         * @param title the title of the chart
         * @param label the label of the data axis
         * @param boundary the boundary value used to remove spikes or 0.0 for no boundary
         * @param lineChart true if the chart is a line chart, false if it is a points chart
         * @param degree true if the parameter is expressed in degrees
         */
        public ChartInfo(final int elementID, final String fileNameSuffix, final String title,
                final String label, final double boundary, final boolean lineChart,
                final boolean degree) {
            this(elementID, fileNameSuffix, title, label, boundary, lineChart, degree, null);
        }
        
        /** Constructor.
        *
        * @param elementID the id of the series used
        * @param fileNameSuffix the suffix of the file
        * @param title the title of the chart
        * @param label the label of the data axis
        * @param boundary the boundary value used to remove spikes or 0.0 for no boundary
        * @param lineChart true if the chart is a line chart, false if it is a points chart
        * @param degree true if the parameter is expressed in degrees
        * @param xAxisLabel the label for the x axis if not related to the time
        */
        public ChartInfo(int elementID, String fileNameSuffix, String title,
                String label, double boundary, boolean lineChart,
                boolean degree, String xAxisLabel) {
            this.elementID = elementID;
            this.fileNameSuffix = fileNameSuffix;
            this.title = title;
            this.label = label;
            this.boundary = boundary;
            this.lineChart = lineChart;
            this.degree = degree;
            this.xAxisLabel = xAxisLabel;
        }

        /**
         * @return the elementID
         */
        public int getElementID() {
            return elementID;
        }

        /**
         * @return the fileNameSuffix
         */
        public String getFileNameSuffix() {
            return fileNameSuffix;
        }

        /**
         * @return the title
         */
        public String getTitle() {
            return title;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return the boundary
         */
        public double getBoundary() {
            return boundary;
        }

        /**
         * @return the lineChart
         */
        public boolean isLineChart() {
            return lineChart;
        }

        /**
         * @return the degree
         */
        public boolean isDegree() {
            return degree;
        }

        /**
         * @return the xAxisLabel
         */
        public String getxAxisLabel() {
            return xAxisLabel;
        }
    }

    /**
     * Class holding the list of {@link TimeStampedPVCoordinates} and {@link Rotation} read from a file
     *
     */
    public static class FileDataHolder {
        /** The list of extracted coordinates. */
        private final List<TimeStampedPVCoordinates> coordinates;
        
        /** The list of extracted rotations. */
        private final List<Rotation> rotations;

        /** 
         * Constructor.
         */
        public FileDataHolder() {
            this.coordinates = new ArrayList<>();
            this.rotations = new ArrayList<>();
        }

        /** Add a new element to the list.
         * @param c the element to add
         */
        public void addCoordinates(TimeStampedPVCoordinates c) {
            this.coordinates.add(c);
        }

        /** Add a new element to the list.
         * @param r the element to add
         */
        public void addRotation(Rotation r) {
            this.rotations.add(r);
        }

        /**
         * @return the coordinates
         */
        public List<TimeStampedPVCoordinates> getCoordinates() {
            return coordinates;
        }

        /**
         * @return the rotations
         */
        public List<Rotation> getRotations() {
            return rotations;
        }
    }
}
