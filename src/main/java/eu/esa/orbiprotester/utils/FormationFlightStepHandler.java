/**
 * 
 */
package eu.esa.orbiprotester.utils;

import java.util.LinkedList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * @author lucian
 *
 */
public class FormationFlightStepHandler implements OrekitFixedStepHandler {

    /**
     * The local frame.
     */
    private final LocalOrbitalFrame localFrame;
    
    /**
     * The position and velocity expressed in local frame 
     */
    private final List<TimeStampedPVCoordinates> tpvList;
    
    /** Constructor.
     * @param localFrame
     */
    public FormationFlightStepHandler(LocalOrbitalFrame localFrame) {
        this.localFrame = localFrame;
        this.tpvList = new LinkedList<>();
    }

    /**
     * {@inheritDoc}
     * @see org.orekit.propagation.sampling.OrekitFixedStepHandler#init(org.orekit.propagation.SpacecraftState, org.orekit.time.AbsoluteDate)
     */
    @Override
    public void init(SpacecraftState s0, AbsoluteDate t) {
        // Nothing to do.
    }

    /**
     * {@inheritDoc}
     * @see org.orekit.propagation.sampling.OrekitFixedStepHandler#handleStep(org.orekit.propagation.SpacecraftState, boolean)
     */
    @Override
    public void handleStep(SpacecraftState currentState, boolean isLast) {
        try {
            tpvList.add(currentState.getPVCoordinates(localFrame));
        } catch (OrekitException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the tpvList
     */
    public List<TimeStampedPVCoordinates> getTpvList() {
        return tpvList;
    }
    
    
}
