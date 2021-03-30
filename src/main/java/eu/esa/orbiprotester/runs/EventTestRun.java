/**
 * 
 */
package eu.esa.orbiprotester.runs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;

import eu.esa.orbiprotester.utils.EventDetectionHandler;
import eu.esa.orbiprotester.utils.KeyValueFileParser;
import eu.esa.orbiprotester.utils.Messages;

/** Check the occurence dates for an event
 * 
 * @author lucian
 *
 */
public class EventTestRun extends StandardTestRun {

    /** The configured event detector.  */
    private EventDetector eventDetector;
    
    /** The event handler. */
    private EventDetectionHandler handler;
    
    /** Constructor.
     * 
     * @param parser
     * @param outputFolder
     * @param referenceFolder
     * @param testName
     * @param logger
     */
    public EventTestRun(KeyValueFileParser<ParameterKey> parser, File testFile,
            File outputFolder, File referenceFolder, String testName,
            Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);
        this.eventDetector = null;
        this.handler = null;
    }
    
    

    /** Read the required event detector
     * @see eu.esa.dsst.runs.AbstractTestRun#readInputData()
     */
    @Override
    public void readInputData() throws OrekitException, IOException {
        // call the parent
        super.readInputData();
        
        this.handler = new EventDetectionHandler();
        this.eventDetector = getEventDetector(this.handler);
       
        //TODO: event shifting

        

    }



    /**
     * Perform a numerical propagation.
     *
     * @return the list of states obtained with orekit
     */
    @Override
    protected List<SpacecraftState> runNumericalPropagation() {

        List<SpacecraftState> states = null;
        try {
            // Numerical propagator definition
            final NumericalPropagator numProp = createNumProp(getOrbit(), getMass(), getAttitudeProvider());

            // Set Force models
            setForceModel(numProp);

            // Add event handler
            numProp.setSlaveMode();
            numProp.addEventDetector(this.eventDetector);


            // Numerical Propagation
            getLogger().info(Messages.RUN_OREKIT_PROP);
            boolean endInError = false;
            final double numOn = System.currentTimeMillis();
            try {
                numProp.propagate(getStart(), getStart().shiftedBy(getDuration()));
            } catch (PropagationException ex) {
                endInError = true;
                getLogger().error(ex.getLocalizedMessage(), ex);
            }
            final double numOff = System.currentTimeMillis();
            if (endInError) {
                getLogger().info(Messages.OREKIT_PROP_END_PREMATURELY);
            }
            getLogger().info(Messages.EXEC_OREKIT_TIME + (numOff - numOn) / 1000.);

            states = this.handler.getStates();
        } catch (IOException ioex) {
            getLogger().error(ioex.getLocalizedMessage(), ioex);
        } catch (OrekitException oex) {
            getLogger().error(oex.getLocalizedMessage(), oex);
        }
        return states;
    }

    
    
}
