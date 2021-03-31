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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.apache.log4j.Logger;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import eu.esa.orbiprotester.orekitCustom.JB2008;
import eu.esa.orbiprotester.orekitCustom.SolarIndicesJB2008;
import eu.esa.orbiprotester.utils.ChartDataHolder;
import eu.esa.orbiprotester.utils.KeyValueFileParser;
import eu.esa.orbiprotester.utils.Messages;
import eu.esa.orbiprotester.utils.OrbitHandler;

/** Perform a standard propagation using the desired force model, attitude and event detectors.
 * @author lucian
 *
 */
public class StandardTestRun extends AbstractTestRun {

    /** The result of Orekit propagation as a list of orbital states. */
    private List<SpacecraftState> orekitStates;

    /** The result of Orbipro propagation as a list of time stamped PV coordinates and rotations. */
    private ChartDataHolder.FileDataHolder orbiproData;

    /** Standard constructor.
     * @param parser the parser for the input test file
     * @param outputFolder the root folder for the output
     * @param referenceFolder the folder containing the reference files
     * @param testName the name of the test
     * @param logger the logger
     */
    public StandardTestRun(final KeyValueFileParser<ParameterKey> parser, File testFile,
            final File outputFolder, final File referenceFolder, final String testName,
            final Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);

        this.orekitStates = null;
        this.orbiproData = null;
    }

    /** Perform a standard test.
     * @throws IOException if the output files cannot be created
     * @throws OrekitException if the test fails
     * @see eu.esa.dsst.runs.TestRun#runTest()
     */
    public void runTest() throws IOException, OrekitException {

        // build the output file name
        final File output = new File(getOutputFolder(), getTestName() + "_Orekit.out");

        // Run the Orekit propagation
        orekitStates = runNumericalPropagation();
        processData(orekitStates, getStart(), output, getOrekitLabel(), getChartData());

        // check the reference file
        this.orbiproData = loadDataSeriesFile(getReferenceFileName(), getReferenceLabel(), false);

        // create the special charts
        createSpecialCharts(orekitStates, orbiproData, getOrekitLabel(), getReferenceLabel());
                        
    }

    public double getLightingRatio(final Vector3D position, final Frame frame, final AbsoluteDate date,
    		double equatorialRadius, CelestialBody sun )
            throws OrekitException {

            // Compute useful angles
            final double[] angle = getEclipseAngles(position, frame, date,equatorialRadius,sun);

            // Sat-Sun / Sat-CentralBody angle
            final double sunEarthAngle = angle[0];

            // Central Body apparent radius
            final double alphaCentral = angle[1];

            // Sun apparent radius
            final double alphaSun = angle[2];

            double result = 1.0;

            // Is the satellite in complete umbra ?
            if (sunEarthAngle - alphaCentral + alphaSun <= 0.0) {
                result = 0.0;
            } else if (sunEarthAngle - alphaCentral - alphaSun < 0.0) {
                // Compute a lighting ratio in penumbra
                final double sEA2    = sunEarthAngle * sunEarthAngle;
                final double oo2sEA  = 1.0 / (2. * sunEarthAngle);
                final double aS2     = alphaSun * alphaSun;
                final double aE2     = alphaCentral * alphaCentral;
                final double aE2maS2 = aE2 - aS2;

                final double alpha1  = (sEA2 - aE2maS2) * oo2sEA;
                final double alpha2  = (sEA2 + aE2maS2) * oo2sEA;

                // Protection against numerical inaccuracy at boundaries
                final double a1oaS   = FastMath.min(1.0, FastMath.max(-1.0, alpha1 / alphaSun));
                final double aS2ma12 = FastMath.max(0.0, aS2 - alpha1 * alpha1);
                final double a2oaE   = FastMath.min(1.0, FastMath.max(-1.0, alpha2 / alphaCentral));
                final double aE2ma22 = FastMath.max(0.0, aE2 - alpha2 * alpha2);

                final double P1 = aS2 * FastMath.acos(a1oaS) - alpha1 * FastMath.sqrt(aS2ma12);
                final double P2 = aE2 * FastMath.acos(a2oaE) - alpha2 * FastMath.sqrt(aE2ma22);

                result = 1. - (P1 + P2) / (FastMath.PI * aS2);
            }

            return result;
        }
    
    private double[] getEclipseAngles(final Vector3D position,final Frame frame,
    		final AbsoluteDate date, double equatorialRadius, CelestialBody sun ) throws OrekitException {
    		final double[] angle = new double[3];

			final Vector3D satSunVector = sun.getPVCoordinates(date, frame).getPosition().subtract(position);

// 			Sat-Sun / Sat-CentralBody angle
			angle[0] = Vector3D.angle(satSunVector, position.negate());

			//Central body apparent radius
			final double r = position.getNorm();
			if (r <= equatorialRadius) {
				throw new OrekitException(OrekitMessages.TRAJECTORY_INSIDE_BRILLOUIN_SPHERE, r);
			}
			angle[1] = FastMath.asin(equatorialRadius / r);

			//Sun apparent radius
			angle[2] = FastMath.asin(Constants.SUN_RADIUS / satSunVector.getNorm());

			return angle;
    	}    
    
    

	/**
     * Perform a numerical propagation.
     *
     * @return the list of states obtained with orekit
     */
    protected List<SpacecraftState> runNumericalPropagation() {

        List<SpacecraftState> states = null;
        try {
            // Numerical propagator definition
            final NumericalPropagator numProp = createNumProp(getOrbit(), getMass(), getAttitudeProvider());

            // Set Force models
            setForceModel(numProp);

            // Add orbit handler
            final OrbitHandler numHandler = new OrbitHandler();
            numProp.setMasterMode(getOutStep(), numHandler);


            // Numerical Propagation
            getLogger().info(Messages.RUN_OREKIT_PROP);
            boolean endInError = false;
            final double numOn = System.currentTimeMillis();
            try {
                numProp.propagate(getStart(), getStart().shiftedBy(getDuration()));
            } catch (Exception ex) {
                endInError = true;
                getLogger().error(ex.getLocalizedMessage(), ex);
            }
            final double numOff = System.currentTimeMillis();
            if (endInError) {
                getLogger().info(Messages.OREKIT_PROP_END_PREMATURELY);
            }
            getLogger().info(Messages.EXEC_OREKIT_TIME + (numOff - numOn) / 1000.);

            states = numHandler.getStates();
        } catch (IOException ioex) {
            getLogger().error(ioex.getLocalizedMessage(), ioex);
        } catch (OrekitException oex) {
            getLogger().error(oex.getLocalizedMessage(), oex);
        }
        return states;
    }
}
