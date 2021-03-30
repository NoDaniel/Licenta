/**
 * 
 */
package eu.esa.orbiprotester.runs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.utils.TimeStampedPVCoordinates;

import eu.esa.orbiprotester.utils.ChartDataHolder;
import eu.esa.orbiprotester.utils.FormationFlightStepHandler;
import eu.esa.orbiprotester.utils.KeyValueFileParser;
import eu.esa.orbiprotester.utils.Messages;

/**
 * @author lucian
 *
 */
public class FormationFlightTestRun extends AbstractTestRun {

    /** The type of the local frame. */
    private LOFType localFrameType;
    
    /** The initial orbit of the second satellite. */
    private Orbit secondaryOrbit;
    
    /** The mass of the second satellite. */
    private double secondaryMass;
    
    /** The secondary attitude. */
    private AttitudeProvider secondaryAttitude;

    /** The result of Orekit propagation as a list of time stamped PV coordinates. */
    private List<TimeStampedPVCoordinates> orekitCoordinates;

    /** The result of Orbipro propagation as a list of time stamped PV coordinates. */
    private ChartDataHolder.FileDataHolder orbiproData;    
    
    /**
     * Standard constructor.
     * 
     * @param parser
     *            the parser for the input test file
     * @param outputFolder
     *            the root folder for the output
     * @param referenceFolder
     *            the folder containing the reference files
     * @param testName
     *            the name of the test
     * @param logger
     *            the logger
     */
    public FormationFlightTestRun(KeyValueFileParser<ParameterKey> parser, File testFile, 
            File outputFolder, File referenceFolder, String testName,
            Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);
    }

    
    
    /** Read the local frame.
     * @see eu.esa.orbiprotester.runs.AbstractTestRun#readInputData()
     */
    @Override
    public void readInputData() throws OrekitException, IOException {
        super.readInputData();
        
        // get the local frame type
        localFrameType = getParser().getLocalFrameType(ParameterKey.FORMATION_FLIGHT_FRAME);
        
        // get the initial state of the second satellite
        secondaryOrbit = createOrbit(true);
        
        // get the mass of the second satellite
        secondaryMass = getParser().getDouble(ParameterKey.SECONDARY_MASS);
     
        // get the attitude of the second satellite
        secondaryAttitude = null;
        if (getParser().containsKey(ParameterKey.ORBIPRO_SECONDARY_ATTITUDE)) {
            secondaryAttitude = createAttitudeProvider(ParameterKey.ORBIPRO_SECONDARY_ATTITUDE);
        }
    }



    /**
     * Run the formation flight test.
     * 
     * <p>
     *  The propagation is performed by using an inertial frame and then converting 
     *  the results to the local frame
     * </p>
     * @see eu.esa.orbiprotester.runs.TestRun#runTest()
     */
    @Override
    public void runTest() throws IOException, OrekitException {
        // build the output file name
        final File output = new File(getOutputFolder(), getTestName() + "_Orekit.out");

        // Run the Orekit propagation
        orekitCoordinates = runNumericalPropagation();
        processCoordonates(orekitCoordinates, getStart(), output, getOrekitLabel(), getChartData());

        // check the reference file
        this.orbiproData = loadDataSeriesFile(getReferenceFileName(), getReferenceLabel(), true);

        // create the special charts
        createSpecialCharts(orekitCoordinates, orbiproData.getCoordinates(), getOrekitLabel(), getReferenceLabel());
    }
    

    /**
     * Perform a numerical propagation.
     *
     * @return the list of states obtained with orekit
     */
    protected List<TimeStampedPVCoordinates> runNumericalPropagation() {

        List<TimeStampedPVCoordinates> states = null;
        try {
            // Configure the numerical propagator for the reference state
            final NumericalPropagator referenceProp = createNumProp(getOrbit(), getMass(), getAttitudeProvider());
            // Set Force models
            setForceModel(referenceProp);
            // Set slave mode
            referenceProp.setSlaveMode();
            
            // Configure the numerical propagator for the second satellite
            final NumericalPropagator secondPropagator = createNumProp(secondaryOrbit, secondaryMass, secondaryAttitude);
            //Set Force models
            setForceModel(secondPropagator);
            

            // Create the local frame
            LocalOrbitalFrame localFrame = new LocalOrbitalFrame(getFrame(), localFrameType, referenceProp, localFrameType.name());
            // Add orbit handler
            final FormationFlightStepHandler stepHandler = new FormationFlightStepHandler(localFrame);
            secondPropagator.setMasterMode(getOutStep(), stepHandler);


            // Numerical Propagation
            getLogger().info(Messages.RUN_OREKIT_PROP);
            boolean endInError = false;
            final double numOn = System.currentTimeMillis();
            try {
                secondPropagator.propagate(getStart(), getStart().shiftedBy(getDuration()));
            } catch (PropagationException ex) {
                endInError = true;
                getLogger().error(ex.getLocalizedMessage(), ex);
            }
            final double numOff = System.currentTimeMillis();
            if (endInError) {
                getLogger().info(Messages.OREKIT_PROP_END_PREMATURELY);
            }
            getLogger().info(Messages.EXEC_OREKIT_TIME + (numOff - numOn) / 1000.);

            states = stepHandler.getTpvList();
        } catch (IOException ioex) {
            getLogger().error(ioex.getLocalizedMessage(), ioex);
        } catch (OrekitException oex) {
            getLogger().error(oex.getLocalizedMessage(), oex);
        }
        return states;
    }
    

}
