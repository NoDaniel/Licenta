/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.esa.orbiprotester.runs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.log4j.Logger;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.CelestialBodyPointed;
import org.orekit.attitudes.FixedRate;
import org.orekit.attitudes.GroundPointing;
import org.orekit.attitudes.InertialProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.SpinStabilized;
import org.orekit.attitudes.TabulatedLofOffset;
import org.orekit.attitudes.TabulatedProvider;
import org.orekit.attitudes.YawCompensation;
import org.orekit.attitudes.YawSteering;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.EarthStandardAtmosphereRefraction;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AlignmentDetector;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EclipseDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventShifter;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import eu.esa.orbiprotester.OrbiproTesterLauncher;
import eu.esa.orbiprotester.orekitCustom.JB2008;
import eu.esa.orbiprotester.orekitCustom.SolarIndicesJB2008;
import eu.esa.orbiprotester.utils.ChartDataHolder;
import eu.esa.orbiprotester.utils.EventDetectionHandler;
import eu.esa.orbiprotester.utils.EventDetectionHandler.Direction;
import eu.esa.orbiprotester.utils.KeyValueFileParser;
import eu.esa.orbiprotester.utils.Messages;

/** Abstract implementation for a test.
 *
 * @author Lucian Barbulescu
 */
public abstract class AbstractTestRun implements TestRun {

    /** The time step used for interpolation in variable thrust maneuver simulation. */
    private static final double INTERPOLATION_STEP = 0.1; 
    
    /** The logger. */
    private final Logger logger;

    /** The parser for the input test file. */
    private final KeyValueFileParser<ParameterKey> parser;

    /** The folder for the output. */
    private final File outputFolder;

    /** The folder containing the reference files.*/
    private final File referenceFolder;
    
    /** The input file used by the current test. */
    private final File testFile;

    /** The name of the test. */
    private final String testName;

    /** The timescale. */
    private TimeScale ts;
    
    /** The attitude provider */
    private AttitudeProvider attitudeProvider;
    
    /** The degree. */
    private int degree;

    /** The order. */
    private int order;

    /** The duration of the propagation. */
    private double duration;

    /** The output step. */
    private double outStep;

    /** The gravity provider. */
    private NormalizedSphericalHarmonicsProvider normalized;

    /** The central body. */
    private CelestialBody centralBody;
    
    /** Central body attraction coefficient (m³/s²). */
    private double mu;
    
    /** Central body equatorial radius. */
    private double ae;

    /** Orbital frame definition. */
    private Frame frame;

    /** The frame used to display the position and velocity, if different from the propgation frame. */
    private Frame pvFrame;
    
    /** The initial orbit of the propagation. */
    private Orbit orbit;

    /** The start date of the propagation. */
    private AbsoluteDate start;

    /** The mass of the satellite. */
    private double mass;

    /** The Central body shape . */
    protected OneAxisEllipsoid centralBodyShape;

    /** The type of the chart time axis. */
    private String timeAxisType;

    /** The JFreeChart compatible data. */
    private ChartDataHolder chartData;

    /** The label for the data series obtained with Orekit. */
    private String orekitLabel;
    
    /** The label for the data read from the reference file. */ 
    private String referenceLabel;
    
    /** The name of the file containing the Orbipro data. */
    private String referenceFileName;
    
    /** Minimum step size for variable step integrator (s). */
    private double minStep;
    
    /** Maximum step size for variable step integrator (s). */
    private double maxStep;
    
    /** Initial step size for variable step integrator (s). */
    private double startStep;
    
    /** Spacecraft  */
    
    protected BoxAndSolarArraySpacecraft spacecraft;
    
    
    /** Standard constructor.
     * @param parser the parser for the input test file
     * @param outputFolder the root folder for the output
     * @param referenceFolder the folder containing the reference files
     * @param testName the name of the test
     * @param logger the logger
     */
    public AbstractTestRun(final KeyValueFileParser<ParameterKey> parser, final File testFile, 
            final File outputFolder, final File referenceFolder, final String testName, final Logger logger) {
        this.parser = parser;
        this.testFile = testFile;
        this.outputFolder = outputFolder;
        this.referenceFolder = referenceFolder;
        this.testName = testName;
        this.logger = logger;
    }

    /** Read the mandatory parameters and create the initial orbit.
     *
     * @throws OrekitException in case of an error.
     * @throws IOException if the input file cannot be read correctly.
     *
     * @see eu.esa.dsst.runs.TestRun#readInputData()
     */
    public void readInputData() throws OrekitException, IOException {

    	String dataFolder = "./data/orekit-data";
    	if (parser.containsKey(ParameterKey.OREKIT_DATA_FOLDER)) {
    		dataFolder = parser.getString(ParameterKey.OREKIT_DATA_FOLDER);
    	}
    	
        // configure Orekit
        final File orekitData = getResourceFile(dataFolder);
        DataProvidersManager.getInstance().clearProviders();
        DataProvidersManager.getInstance().addProvider(new DirectoryCrawler(orekitData));
    	
        // Get the timescale
        String timeScaleStr = "UTC"; // By default UTC is used
        if (parser.containsKey(ParameterKey.TIMESCALE)) {
            timeScaleStr = parser.getString(ParameterKey.TIMESCALE);
        }
        
        switch(timeScaleStr) {
            case "UTC":
                this.ts = TimeScalesFactory.getUTC();
                break;
            case "TT":
                this.ts = TimeScalesFactory.getTT();
                break;
            case "TAI":
                this.ts = TimeScalesFactory.getTAI();
                break;
            default:
                throw new IOException("Invalid timescale " + timeScaleStr +". Accepted values are UTC, TT or TAI");
        }
        
        // Check if a central body is defined.
        this.frame = FramesFactory.getEME2000();
        this.pvFrame = null;
        this.centralBody = null;
        if (parser.containsKey(ParameterKey.CENTRAL_BODY)) {
        	// get the central body
        	this.centralBody = parser.getCelectialBodies(ParameterKey.CENTRAL_BODY).get(0);
        	
        	// use the inertial frame from the central body as propagation frame
        	this.frame = this.centralBody.getInertiallyOrientedFrame();
        	
        	// get the central body GM coefficient
        	this.mu = this.centralBody.getGM();
        } else if (parser.containsKey(ParameterKey.ORBIT_FRAME)) {
            // Orbital frame definition
            try {
                this.frame = parser.getInertialFrame(ParameterKey.ORBIT_FRAME);
            } catch (OrekitException e) {
                // try to get an earth frame
                this.frame = parser.getEarthFrame(ParameterKey.ORBIT_FRAME);
            }

            if (!this.frame.isPseudoInertial()) {
                // The required frame is not inertial. 
                // The propagation will use EME2000 while the requested frame will be used only
                // for representing the position and velocity.
                this.pvFrame = this.frame;
                this.frame = FramesFactory.getEME2000();
            }
        }

        Frame bodyFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        if(parser.containsKey(ParameterKey.CENTRAL_BODY_FRAME)){
        	bodyFrame = parser.getEarthFrame(ParameterKey.CENTRAL_BODY_FRAME);
        }

        // Check if the central body is defined and is not earth
        if (this.centralBody != null && this.centralBody != CelestialBodyFactory.getEarth()) {
            this.degree = 0;
            this.order = 0;
            this.normalized = null;
            this.mu = this.centralBody.getGM();

            // The flattening and equatorial radius MUST be defined if the central body is not the Earth
            if (!parser.containsKey(ParameterKey.CENTRAL_BODY_FLATTENING) || !parser.containsKey(ParameterKey.CENTRAL_BODY_AE)) {
            	throw new IOException(Messages.ERR_INCOMPLETE_CENTRAL_BODY);
            }
            // get the central body flattening
            final double flattening = parser.getDouble(ParameterKey.CENTRAL_BODY_FLATTENING);
            
            // get the central body equatorial radius
            this.ae = parser.getDouble(ParameterKey.CENTRAL_BODY_AE);
            
            // create the central body
            this.centralBodyShape = new OneAxisEllipsoid(this.ae, flattening, bodyFrame);
        	
        } else if (parser.containsKey(ParameterKey.CENTRAL_BODY_DEGREE) && parser.containsKey(ParameterKey.CENTRAL_BODY_ORDER)) {
            // Check if the degree and order are set
            // Get the degree and order
            this.degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
            this.order = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));

            // Potential coefficients providers
            this.normalized = GravityFieldFactory.getConstantNormalizedProvider(degree, order);

            // Central body attraction coefficient (m³/s²)
            this.mu = this.normalized.getMu();
            
            //Central body equatorial radius
            this.ae = this.normalized.getAe();
            
            // get the central body flattening, if set
            double flattening = Constants.WGS84_EARTH_FLATTENING;
            if (parser.containsKey(ParameterKey.CENTRAL_BODY_FLATTENING)) {
                flattening = parser.getDouble(ParameterKey.CENTRAL_BODY_FLATTENING);
            }
            
            // create the central body
            this.centralBodyShape = new OneAxisEllipsoid(this.ae, flattening, bodyFrame);
        } else if (parser.containsKey(ParameterKey.CENTRAL_BODY_MU)) {
            
            this.degree = 0;
            this.order = 0;
            this.normalized = null;
            this.mu = parser.getDouble(ParameterKey.CENTRAL_BODY_MU);

            // get the central body flattening, if set
            double flattening = Constants.WGS84_EARTH_FLATTENING;
            if (parser.containsKey(ParameterKey.CENTRAL_BODY_FLATTENING)) {
                flattening = parser.getDouble(ParameterKey.CENTRAL_BODY_FLATTENING);
            }
            
            // get the central body equatorial radius, if set
            this.ae = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
            if (parser.containsKey(ParameterKey.CENTRAL_BODY_AE)) {
                this.ae = parser.getDouble(ParameterKey.CENTRAL_BODY_AE);
            }
            
            // create the central body
            this.centralBodyShape = new OneAxisEllipsoid(this.ae, flattening, bodyFrame);
        } else {
            throw new IOException(Messages.ERR_NO_CENTRAL_BODY);
        }

        // Read the satelite mass
        this.mass = 1000.0;
        if (parser.containsKey(ParameterKey.MASS)) {
            this.mass = parser.getDouble(ParameterKey.MASS);
        }
        
        // Initial orbit definition
        this.orbit = createOrbit(false);
        
        // Check if the attitude profile is defined
        this.attitudeProvider = null;
        if (parser.containsKey(ParameterKey.ORBIPRO_ATTITUDE)) {
            this.attitudeProvider = createAttitudeProvider(ParameterKey.ORBIPRO_ATTITUDE);
        }
        

        // Start date (identical with orbit date if not defined)
        if (parser.containsKey(ParameterKey.START_DATE)) {
            this.start = parser.getDate(ParameterKey.START_DATE, ts);
        } else {
            this.start = parser.getDate(ParameterKey.ORBIT_DATE, ts);
        }

        // the duration (if defined)
        this.duration = 0.;
        if (parser.containsKey(ParameterKey.DURATION)) {
            this.duration = parser.getDouble(ParameterKey.DURATION);
        } else if (parser.containsKey(ParameterKey.DURATION_IN_DAYS)) {
            this.duration = parser.getDouble(ParameterKey.DURATION_IN_DAYS) * Constants.JULIAN_DAY;
        } else if (parser.containsKey(ParameterKey.DURATION_IN_ORBITS)) {
            this.duration = parser.getDouble(ParameterKey.DURATION_IN_ORBITS) * this.orbit.getKeplerianPeriod();
        }

        // the output step (if defined)
        this.outStep = 0.;
        if (parser.containsKey(ParameterKey.OUTPUT_STEP)) {
            this.outStep = parser.getDouble(ParameterKey.OUTPUT_STEP);
        }

        //Get the Orekit series label, if defined
        this.orekitLabel = Messages.OREKIT_LABEL;
        if (getParser().containsKey(ParameterKey.OREKIT_LABEL)) {
        	this.orekitLabel = getParser().getString(ParameterKey.OREKIT_LABEL);
        }
        
        //Get the Reference series label, if defined
        this.referenceLabel = Messages.ORBIPRO_LABEL;
        if (getParser().containsKey(ParameterKey.ORBIPRO_REFERENCE_LABEL)) {
            this.referenceLabel = getParser().getString(ParameterKey.ORBIPRO_REFERENCE_LABEL);
        }
        
        // Check the reference file
        if (!getParser().containsKey(ParameterKey.ORBIPRO_REFERENCE_FILE)) {
            throw new IOException("No reference file declared!");
        }

        //Read the reference file name
        referenceFileName = parser.getString(ParameterKey.ORBIPRO_REFERENCE_FILE);
        
        // Get the time axis type
        this.timeAxisType = "days";
        if (parser.containsKey(ParameterKey.TIME_AXIS_TYPE)) {
            timeAxisType = parser.getString(ParameterKey.TIME_AXIS_TYPE);
        }
        
        // Get the integrator settings
        this.minStep = 1e-4;
        if (parser.containsKey(ParameterKey.INTEGRATOR_MIN_STEP)) {
            this.minStep = parser.getDouble(ParameterKey.INTEGRATOR_MIN_STEP);
        }
        this.maxStep = 1000d;
        if (parser.containsKey(ParameterKey.INTEGRATOR_MAX_STEP)) {
            this.maxStep = parser.getDouble(ParameterKey.INTEGRATOR_MAX_STEP);
        }
        this.startStep = this.minStep * 10.;
        if (parser.containsKey(ParameterKey.INTEGRATOR_START_STEP)) {
            this.startStep = parser.getDouble(ParameterKey.INTEGRATOR_START_STEP);
        }
        
        // Build JFreeChart compliant data holder
        this.chartData = new ChartDataHolder(timeAxisType, logger, outputFolder, testName);

    }

    /** Create the charts.
     *
     * @throws IOException if the chart images cannot be written.
     *
     * @see eu.esa.dsst.runs.TestRun#finalizeTest()
     */
    public void finalizeTest() throws IOException {
        // print charts
        logger.info(Messages.PRINTING_CHARTS);
        try {
            chartData.createCharts();
            logger.info(Messages.CHARTS_PRINTED_OK);
        }
        catch (NullPointerException ex) {
            logger.error(Messages.CHARTS_PRINTED_ERR);
        }
    }

    /** Create an orbit from input parameters.
     *
     * @param secondaryParameters
     *               if true the orbital parameters are extracted from the seondary parameters section
     * @return the orbit created
     * @throws IOException
     *             if input parameters are invalid
     * @throws OrekitException
     *             if orbit frame is not inertial
     */
    protected Orbit createOrbit(final boolean secondaryParameters)
        throws IOException, OrekitException {

        // Orbit definition
        if (secondaryParameters) {
            if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new KeplerianOrbit(
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_A) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_E),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_I),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_PA),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_RAAN),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_KEPLERIAN_ANOMALY),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new EquinoctialOrbit(
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_A) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_EX),
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_EY),
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_HX),
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_HY),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_EQUINOCTIAL_LAMBDA),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_CIRCULAR_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.SECONDARY_ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new CircularOrbit(
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CIRCULAR_A) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CIRCULAR_EX),
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CIRCULAR_EY),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_CIRCULAR_I),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_CIRCULAR_RAAN),
                        parser.getAngle(ParameterKey.SECONDARY_ORBIT_CIRCULAR_ALPHA),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.SECONDARY_ORBIT_CARTESIAN_PX)) {
                final double[] pos = {
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_PX) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_PY) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_PZ) * 1000. };
                final double[] vel = {
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_VX) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_VY) * 1000.,
                        parser.getDouble(ParameterKey.SECONDARY_ORBIT_CARTESIAN_VZ) * 1000. };
                return new CartesianOrbit(new PVCoordinates(new Vector3D(pos),
                        new Vector3D(vel)), frame, parser.getDate(ParameterKey.ORBIT_DATE, ts), mu);
            } 
        } else  {
            if (parser.containsKey(ParameterKey.ORBIT_KEPLERIAN_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new KeplerianOrbit(
                        parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_A) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_KEPLERIAN_E),
                        parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_I),
                        parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_PA),
                        parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_RAAN),
                        parser.getAngle(ParameterKey.ORBIT_KEPLERIAN_ANOMALY),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.ORBIT_EQUINOCTIAL_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new EquinoctialOrbit(
                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_A) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EX),
                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_EY),
                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HX),
                        parser.getDouble(ParameterKey.ORBIT_EQUINOCTIAL_HY),
                        parser.getAngle(ParameterKey.ORBIT_EQUINOCTIAL_LAMBDA),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.ORBIT_CIRCULAR_A)) {
                PositionAngle angleType = PositionAngle.MEAN;
                if (parser.containsKey(ParameterKey.ORBIT_ANGLE_TYPE)) {
                    angleType = PositionAngle.valueOf(parser.getString(
                            ParameterKey.ORBIT_ANGLE_TYPE).toUpperCase());
                }
                return new CircularOrbit(
                        parser.getDouble(ParameterKey.ORBIT_CIRCULAR_A) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EX),
                        parser.getDouble(ParameterKey.ORBIT_CIRCULAR_EY),
                        parser.getAngle(ParameterKey.ORBIT_CIRCULAR_I),
                        parser.getAngle(ParameterKey.ORBIT_CIRCULAR_RAAN),
                        parser.getAngle(ParameterKey.ORBIT_CIRCULAR_ALPHA),
                        angleType, frame, parser.getDate(ParameterKey.ORBIT_DATE,
                                ts), mu);
            } else if (parser.containsKey(ParameterKey.ORBIT_CARTESIAN_PX)) {
                final double[] pos = {
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PX) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PY) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_PZ) * 1000. };
                final double[] vel = {
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VX) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VY) * 1000.,
                        parser.getDouble(ParameterKey.ORBIT_CARTESIAN_VZ) * 1000. };
                return new CartesianOrbit(new PVCoordinates(new Vector3D(pos),
                        new Vector3D(vel)), frame, parser.getDate(ParameterKey.ORBIT_DATE, ts), mu);
            } 
        }
        
        throw new IOException(Messages.ORBIT_INCOMPLETE);
    }

    /** Create the attitude provider from the given key.
     * @param attitudeKey the key.
     * @return the attitude provider
     * @throws OrekitException if the provider cannot be initialised
     * @throws IOException if the attitude profile file cannot be parsed
     */
    protected AttitudeProvider createAttitudeProvider(ParameterKey attitudeKey) throws OrekitException, IOException {
        
        AttitudeProvider provider = null;
        final String attitudeProviderName = parser.getString(attitudeKey);
        
        switch(attitudeProviderName) {
            case "BODYCENTER":
                provider = new BodyCenterPointing(this.frame, this.centralBodyShape);
                break;
            case "NADIRPOINTING":
                provider = new NadirPointing(this.frame, this.centralBodyShape);
                break;
            case "LVLH":
                provider = new LofOffset(this.frame, LOFType.LVLH);
                break;
            case "VVLH":
                provider = new LofOffset(this.frame, LOFType.VVLH);
                break;
            case "VNC":
                provider = new LofOffset(this.frame, LOFType.VNC);
                break;
            case "INERTIALPROVIDER":
                // get the rotation data
                final double q0 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q0);
                final double q1 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q1);
                final double q2 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q2);
                final double q3 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q3);
                final boolean normalize = parser.getBoolean(ParameterKey.ORBIPRO_ATTITUDE_NORMALIZE);
                
                final Rotation r = new Rotation(q0, q1, q2, q3, normalize);
                provider = new InertialProvider(r);
                break;
                
            case "FIXEDRATE":
                // get the rotation data
                final double fq0 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q0);
                final double fq1 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q1);
                final double fq2 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q2);
                final double fq3 = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_Q3);
                final boolean fnormalize = parser.getBoolean(ParameterKey.ORBIPRO_ATTITUDE_NORMALIZE);
                
                final Rotation fr = new Rotation(fq0, fq1, fq2, fq3, fnormalize);
                
                // get the reference attitude date
                final AbsoluteDate attDate = parser.getDate(ParameterKey.ORBIPRO_ATTITUDE_DATE, ts);
                final double rate = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_ROTATIONRATE);
                
                Vector3D rotationRate = parser.getVector(ParameterKey.ORBIPRO_ATTITUDE_ROTATIONAXIS_X, ParameterKey.ORBIPRO_ATTITUDE_ROTATIONAXIS_Y, ParameterKey.ORBIPRO_ATTITUDE_ROTATIONAXIS_Z);
                rotationRate = rotationRate.scalarMultiply(rate);
                
                TimeStampedAngularCoordinates orientation = new TimeStampedAngularCoordinates(attDate, fr, rotationRate, Vector3D.ZERO);
                
                Attitude attitude = new Attitude(frame, orientation);
                
                provider = new FixedRate(attitude);
                
                break;
                
            case "TABULATED":
                // get the file containing the attitude profile
                final String attitudeFileName = parser.getString(ParameterKey.ORBIPRO_ATTITUDE_TABULATED_FILE);
                
                // Get the attitude file
                final File attitudeFile = new File(testFile.getParentFile(), attitudeFileName);
                
                List<TimeStampedAngularCoordinates> tsaList = parseAttitudesFile(attitudeFile);
                
                // get the configured filter
                final String filterStr = parser.getString(ParameterKey.ORBIPRO_ATTITUDE_FILTER);
                AngularDerivativesFilter filter = AngularDerivativesFilter.valueOf(filterStr);
                if (filter == null) {
                    throw new IOException(Messages.ERR_UNKNOWN_FILTER);
                }
                
                // get n
                final int n = parser.getInt(ParameterKey.ORBIPRO_ATTITUDE_N);
                
                // check if a local frame is set
                if (parser.containsKey(ParameterKey.ORBIPRO_ATTITUDE_TABULATED_LOF)) {
                    LOFType lofType = parser.getLocalFrameType(ParameterKey.ORBIPRO_ATTITUDE_TABULATED_LOF);
                    
                    provider = new TabulatedLofOffset(this.frame, lofType, tsaList, n, filter);
                } else {
                    provider = new TabulatedProvider(this.frame, tsaList, n, filter); 
                }
                
                break;
            case "CELESTIALBODYPOINTING":
            	final CelestialBody pointedBody = parser.getCelectialBodies(ParameterKey.ORBIPRO_ATTITUDE_POINTED_BODY).get(0);
            	final Vector3D phasingCel = parser.getVector(
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGCEL_X,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGCEL_Y,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGCEL_Z);
            	final Vector3D pointingSat = parser.getVector(
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_POINTINGSAT_X,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_POINTINGSAT_Y,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_POINTINGSAT_Z);
            	final Vector3D phasingSat = parser.getVector(
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGSAT_X,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGSAT_Y,
            			ParameterKey.ORBIPRO_ATTITUDE_POINTED_PHASINGSAT_Z);
            	
            	provider = new CelestialBodyPointed(this.frame, pointedBody, phasingCel, pointingSat, phasingSat);
            	break;
            default:
                provider = null;
                break;
        }
        
        // check for any wrapper
        if (parser.containsKey(ParameterKey.ORBIPRO_ATTITUDE_WRAPPER)) {
            final String attitudeWrapper = parser.getString(ParameterKey.ORBIPRO_ATTITUDE_WRAPPER);
            switch(attitudeWrapper) {
                case "YAWCOMPENSATION":
                    provider = new YawCompensation(this.frame, (GroundPointing)provider);
                    break;
                case "YAWSTEERING":
                	final CelestialBody body = parser.getCelectialBodies(ParameterKey.ORBIPRO_ATTITUDE_YAWSTEERING_BODY).get(0);
                	final Vector3D phasingAxis = parser.getVector(
                			ParameterKey.ORBIPRO_ATTITUDE_YAWSTEERING_PHASINGAXIS_X,
                			ParameterKey.ORBIPRO_ATTITUDE_YAWSTEERING_PHASINGAXIS_Y,
                			ParameterKey.ORBIPRO_ATTITUDE_YAWSTEERING_PHASINGAXIS_Z);
                	provider = new YawSteering(this.frame, (GroundPointing)provider, body, phasingAxis);
                	break;
                case "SPIN":
                	final Vector3D rotationAxis = parser.getVector(
                			ParameterKey.ORBIPRO_ATTITUDE_SPIN_ROTATIONAXIS_X, 
                			ParameterKey.ORBIPRO_ATTITUDE_SPIN_ROTATIONAXIS_Y,
                			ParameterKey.ORBIPRO_ATTITUDE_SPIN_ROTATIONAXIS_Z);
                	final double rotationRate = parser.getDouble(ParameterKey.ORBIPRO_ATTITUDE_SPIN_ROTATIONRATE);
                	final AbsoluteDate rotationStartDate = parser.getDate(ParameterKey.ORBIPRO_ATTITUDE_SPIN_DATE, this.ts);
                	provider = new SpinStabilized(provider, rotationStartDate, rotationAxis, rotationRate);
                	
                	break;
                default:
                    break;
            }
        }
        
        return provider;
    }
    
    
    /** Create the numerical propagator.
     *
     * @param startOrbit
     *            initial orbit
     * @param startMass
     *            S/C mass (kg)
     * @param attitudeProvider
     *            the attitude provider used
     * @return an instance of the numerical propagator
     *               the numerical propagator
     * @throws OrekitException if the propagator cannot be created
     */
    protected NumericalPropagator createNumProp(final Orbit startOrbit, final double startMass,
            final AttitudeProvider attitudeProvider) throws OrekitException {
        AbstractIntegrator integrator;
		OrbitType propType = OrbitType.EQUINOCTIAL;
        if(this.parser.containsKey(ParameterKey.PROPAGATION_ORBIT_TYPE)) {
            propType = OrbitType.valueOf(this.parser.getString(ParameterKey.PROPAGATION_ORBIT_TYPE));
        }
		
        final double[][] tol = NumericalPropagator.tolerances(1.0, startOrbit, propType);
        integrator = new DormandPrince54Integrator(this.minStep, this.maxStep,
                tol[0], tol[1]);
        ((AdaptiveStepsizeIntegrator) integrator).setInitialStepSize(this.startStep);
        final NumericalPropagator numProp = new NumericalPropagator(integrator);
        if (attitudeProvider != null) {
            numProp.setAttitudeProvider(attitudeProvider);
            
            Attitude startAttitude = attitudeProvider.getAttitude(startOrbit, startOrbit.getDate(), startOrbit.getFrame());
            numProp.setInitialState(new SpacecraftState(startOrbit, startAttitude, startMass));
        } else {
            numProp.setInitialState(new SpacecraftState(startOrbit, startMass));
        }
        
        numProp.setOrbitType(propType);
        numProp.setPositionAngleType(PositionAngle.MEAN);

        return numProp;
    }

    /** Set numerical propagator force models.
     *
     * @param numProp
     *            numerical propagator
     * @throws IOException if there is an error accessing a force model file
     * @throws OrekitException if there is an error creating the force objects
     */
    protected void setForceModel(final NumericalPropagator numProp)
        throws IOException, OrekitException {

        // Add the perturbation forces

    	// Central Body (normalized coefficients)
        if (this.normalized != null) {
            ForceModel centralBody = new HolmesFeatherstoneAttractionModel(centralBodyShape.getBodyFrame(), this.normalized);
            numProp.addForceModel(centralBody);
        }

       
        
        // Solar radiation pressure
        
        if (parser.containsKey(ParameterKey.SOLAR_RADIATION_PRESSURE)){
        	CelestialBody sun = CelestialBodyFactory.getSun();
        	
        	Vector3D referenceNormal   = parser.getVector(ParameterKey.SPACECRAFT_REFERENCE_NORMAL_X,
        								                  ParameterKey.SPACECRAFT_REFERENCE_NORMAL_Y,ParameterKey.SPACECRAFT_REFERENCE_NORMAL_Z);
        	double xLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_X);
        	double yLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_Y);
        	double zLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_Z);
        	double solarArrayArea      = parser.getDouble(ParameterKey.SOLAR_ARRAY_AREA);
        	AbsoluteDate referenceDate = parser.getDate(ParameterKey.SPACECRAFT_REFERENCE_DATE, this.ts);
        	double absorptionCoeff     = parser.getDouble(ParameterKey.SPACECRAFT_ABSORPTION_COEFF);
        	double reflectionCoeff     = parser.getDouble(ParameterKey.SPACECRAFT_REFLECTION_COEFF); 
        	double dragCoeff           = 0.0;
        	double rotationRate        = parser.getDouble(ParameterKey.SPACECRAFT_ROTATION_RATE);
        	
        	this.spacecraft = new BoxAndSolarArraySpacecraft(xLength,yLength,zLength,sun,solarArrayArea,Vector3D.PLUS_J,referenceDate,
        												     referenceNormal,rotationRate,dragCoeff,absorptionCoeff,reflectionCoeff);
           SolarRadiationPressure SRP = new SolarRadiationPressure(sun, this.ae,this.spacecraft);
           numProp.addForceModel(SRP);
        }
        	
        // drag force
        if (parser.containsKey(ParameterKey.DRAG) && parser.getBoolean(ParameterKey.DRAG)){
        	
        	Vector3D referenceNormal   = parser.getVector(ParameterKey.SPACECRAFT_REFERENCE_NORMAL_X,
            							                  ParameterKey.SPACECRAFT_REFERENCE_NORMAL_Y,ParameterKey.SPACECRAFT_REFERENCE_NORMAL_Z);
            double xLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_X);
            double yLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_Y);
            double zLength 		       = parser.getDouble(ParameterKey.SPACECRAFT_LENGTH_Z);
            double solarArrayArea      = parser.getDouble(ParameterKey.SOLAR_ARRAY_AREA);
            AbsoluteDate referenceDate = parser.getDate(ParameterKey.SPACECRAFT_REFERENCE_DATE, this.ts);
            double absorptionCoeff     = parser.getDouble(ParameterKey.SPACECRAFT_ABSORPTION_COEFF);
            double reflectionCoeff     = parser.getDouble(ParameterKey.SPACECRAFT_REFLECTION_COEFF); 
            double dragCoeff           = parser.getDouble(ParameterKey.DRAG_CD);
            double rotationRate        = parser.getDouble(ParameterKey.SPACECRAFT_ROTATION_RATE);
        	
        	CelestialBody sun            = CelestialBodyFactory.getSun();
        	SolarIndicesJB2008 inJB2008  = new SolarIndicesJB2008(DataProvidersManager.getInstance());
        	final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 
        			                                            Constants.WGS84_EARTH_FLATTENING, 
        			                                            centralBodyShape.getBodyFrame());
            final Atmosphere atm         = new JB2008(inJB2008, sun, earth);
        	this.spacecraft              = new BoxAndSolarArraySpacecraft(xLength, yLength, zLength, 
        	                                                              sun, solarArrayArea, Vector3D.PLUS_J, 
        																  referenceDate, referenceNormal, 
        																  rotationRate, dragCoeff, 
        																  absorptionCoeff, reflectionCoeff);
        	
        	numProp.addForceModel(new DragForce(atm, this.spacecraft));
        }
        
        // Third body
        if (parser.containsKey(ParameterKey.THIRD_BODY)) {
        	List<CelestialBody> bodies = parser.getCelectialBodies(ParameterKey.THIRD_BODY);
        	
        	for (CelestialBody body : bodies) {
        		ForceModel thirdBody = new ThirdBodyAttraction(body);
        		numProp.addForceModel(thirdBody);
        	}
        }
            
        // TODO add the rest of perturbations
    }

    /** Parse a file containing a variable thrust maneuver and convert it to several constant maneuvers. 
     * @param maneuverFile the file containing the maneuver data
     * @param numProp the propagator
     * @param isp the engine isp
     * @param step the time interval for which the maneuver can be considered constant 
     * @throws IOException if the maneuver file cannot be accessed 
     */
    private void parseManeuverFile(final File maneuverFile, final NumericalPropagator numProp, final double isp, final double step) throws IOException {
        // define the regular epressions
        final Pattern dateRegexp     = Pattern.compile("^((?:-?\\p{Digit}{4})-?(?:\\p{Digit}{2})-?(?:\\p{Digit}{2})T(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}(?:[.,]\\p{Digit}+)?)?(?:Z|[-+]00(?::00)?)?)$");
        final Pattern sampleRegexp   = Pattern.compile("^((?:-?\\p{Digit}{4})-?(?:\\p{Digit}{2})-?(?:\\p{Digit}{2})T(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}(?:[.,]\\p{Digit}+)?)?(?:Z|[-+]00(?::00)?)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+\\[(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\]$");
        
        AbsoluteDate sDate = null;
        AbsoluteDate eDate = null;
        
        final LinearInterpolator linearInterpolator = new LinearInterpolator();
        
        List<Double> times = new LinkedList<>();
        List<Double> thrust = new LinkedList<>();
        List<Double> directionX = new LinkedList<>();
        List<Double> directionY = new LinkedList<>();
        List<Double> directionZ = new LinkedList<>();
        
        try(final BufferedReader manReader = new BufferedReader(new FileReader(maneuverFile))) {
            String line = null;
            while ((line = manReader.readLine()) != null) {
                line = line.trim();
                // ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
              
                final Matcher dateMatcher = dateRegexp.matcher(line);
                if (dateMatcher.matches()) {
                    // this is either startDate or endDate
                    if (sDate == null) {
                        sDate = new AbsoluteDate(dateMatcher.group(1), ts);
                    } else if (eDate == null) {
                        eDate = new AbsoluteDate(dateMatcher.group(1), ts);
                        // switch dates if in incorrect order
                        if (eDate.durationFrom(sDate) < 0) {
                            AbsoluteDate tDate = sDate;
                            sDate = eDate;
                            eDate = tDate;
                        }
                    } else {
                        throw new IOException(Messages.ERR_DATE_LINES);
                    }
                } else {
                    final Matcher sampleMatcher = sampleRegexp.matcher(line);
                    if (sampleMatcher.matches()) {
                        // validate dates
                        if (sDate == null || eDate == null) {
                            throw new IOException(Messages.ERR_DATE_LINES);
                        }
                        final AbsoluteDate dt = new AbsoluteDate(sampleMatcher.group(1), ts);
                        
                        times.add(dt.durationFrom(sDate));
                        thrust.add(Double.parseDouble(sampleMatcher.group(2)));
                        directionX.add(Double.parseDouble(sampleMatcher.group(3)));
                        directionY.add(Double.parseDouble(sampleMatcher.group(4)));
                        directionZ.add(Double.parseDouble(sampleMatcher.group(5)));
                    }
                }
            }
        }
        
        // create a Continuous maneuver for each step interval 
        AbsoluteDate manDate = sDate;
        
        PolynomialSplineFunction thrustFunction = linearInterpolator.interpolate(times.stream().mapToDouble(d -> d).toArray(), thrust.stream().mapToDouble(d -> d).toArray());
        PolynomialSplineFunction directionXFunction = linearInterpolator.interpolate(times.stream().mapToDouble(d -> d).toArray(), directionX.stream().mapToDouble(d -> d).toArray());
        PolynomialSplineFunction directionYFunction = linearInterpolator.interpolate(times.stream().mapToDouble(d -> d).toArray(), directionY.stream().mapToDouble(d -> d).toArray());
        PolynomialSplineFunction directionZFunction = linearInterpolator.interpolate(times.stream().mapToDouble(d -> d).toArray(), directionZ.stream().mapToDouble(d -> d).toArray());
        
        while (eDate.durationFrom(manDate) >= step) {
            final double thrustData = thrustFunction.value(manDate.durationFrom(sDate));
            final double dirXData = directionXFunction.value(manDate.durationFrom(sDate));
            final double dirYData = directionYFunction.value(manDate.durationFrom(sDate));
            final double dirZData = directionZFunction.value(manDate.durationFrom(sDate));
            final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(manDate, step, thrustData, isp, new Vector3D(dirXData, dirYData, dirZData));
            //System.out.println(manDate.durationFrom(sDate) + "," + thrustData + ", " + dirXData + ", " + dirYData + ", " + dirZData);
            numProp.addForceModel(maneuver);
            manDate = manDate.shiftedBy(step);
        }
        
        if (eDate.durationFrom(manDate) != 0) {
            final double thrustData = thrustFunction.value(manDate.durationFrom(sDate));
            final double dirXData = directionXFunction.value(manDate.durationFrom(sDate));
            final double dirYData = directionYFunction.value(manDate.durationFrom(sDate));
            final double dirZData = directionZFunction.value(manDate.durationFrom(sDate));
            final ConstantThrustManeuver maneuver = new ConstantThrustManeuver(manDate, step, thrustData, isp, new Vector3D(dirXData, dirYData, dirZData));
            //System.out.println(manDate.durationFrom(sDate) + "," + thrustData + ", " + dirXData + ", " + dirYData + ", " + dirZData);
            numProp.addForceModel(maneuver);
        }
    }

    /** Parse a file containing a list of attitude values. 
     * @param attitudeFile the file containing the attitude data
     * @returns a list of attitudes
     * @throws IOException if the attitude file cannot be accessed 
     */
    private List<TimeStampedAngularCoordinates> parseAttitudesFile(final File attitudeFile) throws IOException {
        // define the regular epressions
        final Pattern sampleRegexp   = Pattern.compile("^((?:-?\\p{Digit}{4})-?(?:\\p{Digit}{2})-?(?:\\p{Digit}{2})T(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}):?(?:\\p{Digit}{2}(?:[.,]\\p{Digit}+)?)?(?:Z|[-+]00(?::00)?)?)\\p{Space}+\\[(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\]\\p{Space}+\\[(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\]\\p{Space}+\\[(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\p{Space}+(\\p{Digit}+(?:\\.\\p{Digit}+)?)\\]$");
        List<TimeStampedAngularCoordinates> result = new ArrayList<>();
        
        try(final BufferedReader manReader = new BufferedReader(new FileReader(attitudeFile))) {
            String line = null;
            while ((line = manReader.readLine()) != null) {
                line = line.trim();
                // ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
              
                final Matcher sampleMatcher = sampleRegexp.matcher(line);
                if (sampleMatcher.matches()) {
                    final AbsoluteDate dt = new AbsoluteDate(sampleMatcher.group(1), ts);
                    final Rotation rotation = new Rotation(
                            Double.parseDouble(sampleMatcher.group(2)), 
                            Double.parseDouble(sampleMatcher.group(3)), 
                            Double.parseDouble(sampleMatcher.group(4)), 
                            Double.parseDouble(sampleMatcher.group(5)),
                            true);
                    
                    final Vector3D rotationRate = new Vector3D(
                            Double.parseDouble(sampleMatcher.group(6)), 
                            Double.parseDouble(sampleMatcher.group(7)), 
                            Double.parseDouble(sampleMatcher.group(8)));

                    final Vector3D rotationAcceleration = new Vector3D(
                            Double.parseDouble(sampleMatcher.group(9)), 
                            Double.parseDouble(sampleMatcher.group(10)), 
                            Double.parseDouble(sampleMatcher.group(11)));
                    
                    TimeStampedAngularCoordinates tsa = new TimeStampedAngularCoordinates(dt, rotation, rotationRate, rotationAcceleration);
                    
                    result.add(tsa);
                    
                }
            }
        }
        
        return result;
    }
    
    
    
    /** Print the results in the output file.
     *
     * @param output
     *            output file
     * @param states
     *            the list of states
     * @param startDate
     *            start date of propagation
     * @throws IOException if the output file cannot be created
     * @throws OrekitException if the conversion to the required non-intertial frame cannot be performed
     */
    private void printOutput(final File output, final List<SpacecraftState> states,
            final AbsoluteDate startDate) throws IOException, OrekitException {
        // Output format:
        // time_from_start, a, e, i, raan, pa, aM, h, k, p, q, lM, px, py, pz,
        // vx, vy, vz, q0, q1, q2, q3, rrx, rry, rrz, rax, ray, raz
        final String format = new String(
                " %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e");
        final BufferedWriter buffer = new BufferedWriter(new FileWriter(output));
        buffer.write("##   time_from_start(s)            a(km)                      e                      i(deg)         ");
        buffer.write("         raan(deg)                pa(deg)              mean_anomaly(deg)              ey/h          ");
        buffer.write("           ex/k                    hy/p                     hx/q             mean_longitude_arg(deg)");
        buffer.write("       Xposition(km)           Yposition(km)             Zposition(km)           Xvelocity(km/s)    ");
        buffer.write("      Yvelocity(km/s)         Zvelocity(km/s)                  Q0                       Q1          ");
        buffer.write("            Q2                       Q3               XRotationRate(rad/s)     YRotationRate(rad/s) ");
        buffer.write("    ZRotationRate(rad/s)   XRotationAcc(rad/s^2)      YRotationAcc(rad/s^2)    ZRotationAcc(rad/s^2)");
        buffer.newLine();
        for (SpacecraftState s : states) {
            Orbit o = s.getOrbit();
            final Formatter f = new Formatter(new StringBuilder(),
                    Locale.ENGLISH);
            // Time from start (s)
            final double time = o.getDate().durationFrom(startDate);
            // Semi-major axis (km)
            final double a = o.getA() / 1000.;
            // Keplerian elements
            // Eccentricity
            final double e = o.getE();
            // Inclination (degrees)
            final double i = Math.toDegrees(MathUtils.normalizeAngle(o.getI(),
                    FastMath.PI));
            // Right Ascension of Ascending Node (degrees)
            final KeplerianOrbit ko = new KeplerianOrbit(o);
            final double raan = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getRightAscensionOfAscendingNode(), FastMath.PI));
            // Perigee Argument (degrees)
            final double pa = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getPerigeeArgument(), FastMath.PI));
            // Mean Anomaly (degrees)
            final double am = Math.toDegrees(MathUtils.normalizeAngle(
                    ko.getAnomaly(PositionAngle.MEAN), FastMath.PI));
            // Equinoctial elements
            // ey/h component of eccentricity vector
            final double h = o.getEquinoctialEy();
            // ex/k component of eccentricity vector
            final double k = o.getEquinoctialEx();
            // hy/p component of inclination vector
            final double p = o.getHy();
            // hx/q component of inclination vector
            final double q = o.getHx();
            // Mean Longitude Argument (degrees)
            final double lm = Math.toDegrees(MathUtils.normalizeAngle(
                    o.getLM(), FastMath.PI));
            // Cartesian elements
            // Check if the same frame as the one used in propagation should be used
            PVCoordinates pvCoords = null;
            if (this.pvFrame != null) {
                pvCoords = o.getPVCoordinates(this.pvFrame);
            } else {
                pvCoords = o.getPVCoordinates();
            }
            // Position along X in inertial frame (km)
            final double px = pvCoords.getPosition().getX() / 1000.;
            // Position along Y in inertial frame (km)
            final double py = pvCoords.getPosition().getY() / 1000.;
            // Position along Z in inertial frame (km)
            final double pz = pvCoords.getPosition().getZ() / 1000.;
            // Velocity along X in inertial frame (km/s)
            final double vx = pvCoords.getVelocity().getX() / 1000.;
            // Velocity along Y in inertial frame (km/s)
            final double vy = pvCoords.getVelocity().getY() / 1000.;
            // Velocity along Z in inertial frame (km/s)
            final double vz = pvCoords.getVelocity().getZ() / 1000.;
            
            // Rotation quaternions
            Rotation r = s.getAttitude().getRotation();
            // Q0
            final double q0 = r.getQ0();
            // Q1
            final double q1 = r.getQ1();
            // Q2
            final double q2 = r.getQ2();
            // Q3
            final double q3 = r.getQ3();
            
            // Rotation rate
            Vector3D rr = s.getAttitude().getSpin();
            // Rortation rate x
            final double rrx = rr.getX();
            // Rotation rate y
            final double rry = rr.getY();
            // Rotation rate z
            final double rrz = rr.getZ();
            
            // Rotation acceleration
            Vector3D ra = s.getAttitude().getRotationAcceleration();
            // Rortation rate x
            final double rax = ra.getX();
            // Rotation rate y
            final double ray = ra.getY();
            // Rotation rate z
            final double raz = ra.getZ();
            
            buffer.write(f.format(format, time, a, e, i, raan, pa, am, h, k, p,
                    q, lm, px, py, pz, vx, vy, vz, q0, q1, q2, q3, rrx, rry, rrz, rax, ray, raz).toString());
            buffer.newLine();
            f.close();
        }
        buffer.close();
        
        // if the save results flag is set the file is also copied to the reference folder.
        if (parser.containsKey(ParameterKey.SAVE_RESULTS) && parser.getBoolean(ParameterKey.SAVE_RESULTS)) {
            copyFile(output);
        }
    }
    

    /** Print the results in the output file.
     *
     * @param output
     *            output file
     * @param coordonates
     *            the list of position/velocity pairs
     * @param startDate
     *            start date of propagation
     * @throws IOException if the output file cannot be created
     */
    private void printCoordonates(final File output, final List<TimeStampedPVCoordinates> coordonates,
            final AbsoluteDate startDate) throws IOException {
        // Output format:
        // time_from_start, px, py, pz, vx, vy, vz
        final String format = new String(
                " %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e %24.16e");
        final BufferedWriter buffer = new BufferedWriter(new FileWriter(output));
        buffer.write("##   time_from_start(s)        Xposition(km)             Yposition(km)            Zposition(km)     ");
        buffer.write("      Xvelocity(km/s)         Yvelocity(km/s)           Zvelocity(km/s) ");
        buffer.newLine();
        for (TimeStampedPVCoordinates pvCoords : coordonates) {
            final Formatter f = new Formatter(new StringBuilder(),
                    Locale.ENGLISH);
            // Time from start (s)
            final double time = pvCoords.getDate().durationFrom(startDate);
            // Cartesian elements
            // Position along X in inertial frame (km)
            final double px = pvCoords.getPosition().getX() / 1000.;
            // Position along Y in inertial frame (km)
            final double py = pvCoords.getPosition().getY() / 1000.;
            // Position along Z in inertial frame (km)
            final double pz = pvCoords.getPosition().getZ() / 1000.;
            // Velocity along X in inertial frame (km/s)
            final double vx = pvCoords.getVelocity().getX() / 1000.;
            // Velocity along Y in inertial frame (km/s)
            final double vy = pvCoords.getVelocity().getY() / 1000.;
            // Velocity along Z in inertial frame (km/s)
            final double vz = pvCoords.getVelocity().getZ() / 1000.;
            
            
            buffer.write(f.format(format, time, px, py, pz, vx, vy, vz).toString());
            buffer.newLine();
            f.close();
        }
        buffer.close();
        
        // if the save results flag is set the file is also copied to the reference folder.
        if (parser.containsKey(ParameterKey.SAVE_RESULTS) && parser.getBoolean(ParameterKey.SAVE_RESULTS)) {
            copyFile(output);
        }
    }

    /** Copy a data file to the reference folder.
     * @param f the source data file
     * @throws IOException
     */
    private void copyFile(File f) throws IOException {
        final String fileName = f.getName();
        final File targetFile = new File(referenceFolder, fileName);

        // copy the current file to the destination
        Files.copy(f.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
    }

    /** Add propagation data to the chart.
     *
     * @param states the list of states
     * @param startDate the start date
     * @param output the output file
     * @param seriesName the name to display in the chart
     * @param chartHolder the chart data older where the data will be added
     * @throws OrekitException in case of an error
     * @throws IOException if the files cannot be created
     */
    protected void processData(final List<SpacecraftState> states, final AbsoluteDate startDate, final File output, final String seriesName,
            final ChartDataHolder chartHolder) throws IOException, OrekitException {

        if (states.size() > 0) {
            logger.info("Saving " + seriesName + " results to file " + output);
            printOutput(output, states, startDate);
            logger.info(seriesName + " results saved as file " + output);

            logger.info("Adding " + seriesName + " data to chart.");
            chartHolder.parseOrbitList(states, startDate, seriesName, this.pvFrame);
            logger.info(seriesName + " data added to chart.");

        } else {
            // No orbit data generated
            logger.info("No " + seriesName + " data available!");
        }
    }


    /** Add propagation data to the chart.
     *
     * @param coordonates the list of position/velodity pairs
     * @param startDate the start date
     * @param output the output file
     * @param seriesName the name to display in the chart
     * @param chartHolder the chart data older where the data will be added
     * @throws OrekitException in case of an error
     * @throws IOException if the files cannot be created
     */
    protected void processCoordonates(final List<TimeStampedPVCoordinates> coordonates, final AbsoluteDate startDate, final File output, final String seriesName,
            final ChartDataHolder chartHolder) throws IOException, OrekitException {

        if (coordonates.size() > 0) {
            logger.info("Saving " + seriesName + " results to file " + output);
            printCoordonates(output, coordonates, startDate);
            logger.info(seriesName + " results saved as file " + output);

            logger.info("Adding " + seriesName + " data to chart.");
            chartHolder.parseCoordinatesList(coordonates, startDate, seriesName);
            logger.info(seriesName + " data added to chart.");

        } else {
            // No orbit data generated
            logger.info("No " + seriesName + " data available!");
        }
    }

    /** Create the special charts containing the difference between the position and speed vectors.
     *  <br />
     *  Additionally, the rotation difference is displayed.
     * @param orekitCoordinates The position/velocities obtained with Orekit
     * @param orbiproCoordinates The position/velocities obtained with Orbipro
     * @throws OrekitException in case of an error
     * @throws IOException if the images cannot be created
     */
    protected void createSpecialCharts(ChartDataHolder.FileDataHolder referenceDataSeries, ChartDataHolder.FileDataHolder dataSeries, String referenceLabel, String dataLabel) throws OrekitException, IOException {

        // Print the charts for position and velocity
        createSpecialCharts(referenceDataSeries.getCoordinates(), dataSeries.getCoordinates(), referenceLabel, dataLabel);

        // Print the charts for rotations
        if (!referenceDataSeries.getRotations().isEmpty()) {
            final double [][] max = chartData.createRotationsDiff(referenceDataSeries.getRotations(), dataSeries.getRotations(), convertList(dataSeries.getCoordinates(), elem -> elem.getDate()), start, referenceLabel + " - " + dataLabel); 
            //add the maximum values to the log
            chartData.logMaxDiff("Quaternion angle difference " + referenceLabel + " - " + dataLabel + " (deg)", max[0], false);
        }
    }     
    

    /** Create the special charts containing the difference between the position and speed vectors.
     *  <br />
     *  Additionally, the rotation difference is displayed.
     * @param orekitCoordinates The position/velocities obtained with Orekit
     * @param orbiproCoordinates The position/velocities obtained with Orbipro
     * @throws OrekitException in case of an error
     * @throws IOException if the images cannot be created
     */
    protected void createSpecialCharts(List<SpacecraftState> orekitStates, ChartDataHolder.FileDataHolder orbiproData, String referenceLabel, String dataLabel) throws OrekitException, IOException {

        // Print the charts for position and velocity
        createSpecialCharts(convertList(orekitStates, state -> extractPV(state)), orbiproData.getCoordinates(), referenceLabel, dataLabel);

        // Print the charts for rotations
        if (!orbiproData.getRotations().isEmpty()) {
            final double [][] max = chartData.createRotationsDiff(
                    convertList(orekitStates, state -> state.getAttitude().getRotation()), 
                    orbiproData.getRotations(), 
                    convertList(orekitStates, state -> state.getDate()), 
                    start, referenceLabel + " - " + dataLabel); 
            //add the maximum values to the log
            chartData.logMaxDiff("Quaternion angle difference " + referenceLabel + " - " + dataLabel + " (deg)", max[0], false);
        }
    } 
    
    /** Extract the Rotation from a spacecraft state using the required target frame
     * @param state the state
     * @return the PV coordinates
     */
    private TimeStampedPVCoordinates extractPV(final SpacecraftState state) {
        TimeStampedPVCoordinates ret = null;
        try {
            ret = getPVFrame() != null ? state.getPVCoordinates(getPVFrame()) : state.getPVCoordinates();
        } catch (OrekitException e) {
            //Nothing to do.
        }
        
        return ret;
    }    
    
    /** Create the special charts containing the difference between the position and speed vectors.
     * @param orekitCoordinates The position/velocities obtained with Orekit
     * @param orbiproCoordinates The position/velocities obtained with Orbipro
     * @throws OrekitException in case of an error
     * @throws IOException if the images cannot be created
     */
    protected void createSpecialCharts(List<TimeStampedPVCoordinates> orekitCoordinates, List<TimeStampedPVCoordinates> orbiproCoordinates, String referenceLabel, String dataLabel) throws OrekitException, IOException {

        // Add a comparison between Orekit and Cefola, if available
        if (orekitCoordinates != null && orbiproCoordinates != null) {
            final double[][] max = chartData.createVectorsDiff(orekitCoordinates, orbiproCoordinates, start, referenceLabel + " - " + dataLabel);
            //add the maximum values to the log
            this.chartData.logMaxDiff("Position difference " + referenceLabel + " - " + dataLabel + " (Km)", max[0], false);
            this.chartData.logMaxDiff("Velocity difference " + referenceLabel + " - " + dataLabel + " (Km/s)", max[1], false);
            this.chartData.logMaxDiff("Time occurence difference " + referenceLabel + " - " + dataLabel + " (s)", max[2], false);
        }
    }    

    /** Read the values from an Orbipro generated file
     * @param localDataFile if true the file contains the position/velocity expressed in local frame.
     * @return the list of Position/velocity pairs obtained with Orbipro
     * @throws IOException
     * @throws OrekitException
     */
    protected ChartDataHolder.FileDataHolder loadDataSeriesFile(final String fileName, final String label, final boolean localDataFile) throws IOException, OrekitException {
        ChartDataHolder.FileDataHolder orbiproData = null;
        final File referenceFile = new File(referenceFolder, fileName);
        if (!referenceFile.exists()) {
            throw new IOException("The data series file (" + referenceFile.getAbsolutePath() + ") is unreachable!");
        }

        getLogger().info("Reading data series file " + fileName);
        // parse the reference file
        if (localDataFile) {
            orbiproData = chartData.parseLocalDataFile(referenceFile, label, start);
        } else {
            orbiproData = chartData.parseDataFile(referenceFile, label, start);
        }
        getLogger().info("Dataq series file " + fileName + " read succesfully!");
        
        return orbiproData;
    }
    
    
    /** Get the logger.
     * @return the logger
     */
    protected Logger getLogger() {
        return logger;
    }

    /** Get the orbit.
     * @return the orbit
     */
    protected Orbit getOrbit() {
        return orbit;
    }

    /** Set the start orbit.
     * @param orbit the orbit to set
     */
    protected void setOrbit(final Orbit orbit) {
        this.orbit = orbit;
    }

    /** Get the mass.
     * @return the mass
     */
    protected double getMass() {
        return mass;
    }

    /** Get the test name.
     * @return the testName
     */
    protected String getTestName() {
        return testName;
    }

    /** Get the test duration.
     * @return the duration
     */
    protected double getDuration() {
        return duration;
    }

    /** Set the test duration.
     * @param duration the duration to set
     */
    protected void setDuration(final double duration) {
        this.duration = duration;
    }

    /** Get the start date.
     * @return the start
     */
    protected AbsoluteDate getStart() {
        return start;
    }

    /** Set the start date.
     * @param start the start to set
     */
    protected void setStart(final AbsoluteDate start) {
        this.start = start;
    }

    /** Get the output step.
     * @return the outStep
     */
    protected double getOutStep() {
        return outStep;
    }

    /** Set the output step.
     * @param outStep the outStep to set
     */
    protected void setOutStep(final double outStep) {
        this.outStep = outStep;
    }

    /** Get the chart data.
     * @return the chartData
     */
    protected ChartDataHolder getChartData() {
        return chartData;
    }

    /** Get the output folder.
     * @return the outputFolder
     */
    protected File getOutputFolder() {
        return outputFolder;
    }

    /** Get the parser.
     * @return the parser
     */
    protected KeyValueFileParser<ParameterKey> getParser() {
        return parser;
    }

    /** Get the refference folder.
     * @return the referenceFolder
     */
    protected File getReferenceFolder() {
        return referenceFolder;
    }

    /** Get the value of &mu;.
     * @return the mu
     */
    protected double getMu() {
        return mu;
    }

    /** Get the frame.
     * @return the frame
     */
    protected Frame getFrame() {
        return frame;
    }
    
    /** Get the frame used to represent the position and velocity vectors.
     * @return the frame
     */
    protected Frame getPVFrame() {
        return pvFrame;
    }

    /** get the axis type.
     * @return the timeAxisType
     */
    protected String getTimeAxisType() {
        return timeAxisType;
    }

    /**
     * @return the attitudeProvider
     */
    protected AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /**
     * @return the orekitLabel
     */
    protected String getOrekitLabel() {
        return orekitLabel;
    }

    /**
     * @return the referenceLabel
     */
    protected String getReferenceLabel() {
        return referenceLabel;
    }

    /**
     * @return the referenceFileName
     */
    protected String getReferenceFileName() {
        return referenceFileName;
    }

    /**
     * @return the centralBodyShape
     */
    protected OneAxisEllipsoid getCentralBodyShape() {
        return centralBodyShape;
    }

    /**
     * @return the ts
     */
    protected TimeScale getTs() {
        return ts;
    }
    
    /** COnvert between two type of lists
     * @param from the source list
     * @param func the conversion function
     * @return the destination list
     */
    protected static <T, U> List<U> convertList(List<T> from, Function<T, U> func) throws OrekitException {
        return from.stream().map(func).collect(Collectors.toList());
    }    
    

    /**
     * Get a resource file.
     *
     * @param name
     *            resource file name
     * @return resource file
     */
    private static File getResourceFile(final String name) {
        try {

            // try to find the resource alongside with the application jar
            // (useful for production)
            final String className = "/" + OrbiproTesterLauncher.class.getName().replaceAll("\\.", "/") + ".class";
            final Pattern pattern = Pattern.compile("jar:file:([^!]+)!" + className + "$");
            final Matcher matcher = pattern.matcher(OrbiproTesterLauncher.class
                    .getResource(className).toURI().toString());
            if (matcher.matches()) {
                final File resourceFile = new File(new File(matcher.group(1)).getParentFile(), name);
                if (resourceFile.exists()) {
                    return resourceFile;
                }
            }

            // try to find the resource in the classpath (useful for development
            // in an IDE)
            final URL resourceURL = OrbiproTesterLauncher.class.getResource(name);
            if (resourceURL != null) {
                return new File(resourceURL.toURI().getPath());
            }

            // Try to find the file in the fixed location relative to current
            // folder
            final File f = new File(name);
            if (f.exists()) {
                return f;
            }
            throw new IOException("Unable to find orekit-data directory");

        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
