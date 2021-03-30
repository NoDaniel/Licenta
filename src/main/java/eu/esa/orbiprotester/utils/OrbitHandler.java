/**
 * 
 */
package eu.esa.orbiprotester.utils;

import java.util.ArrayList;
import java.util.List;

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

/** Specialized step handler catching the orbit at each step. */
public class OrbitHandler implements OrekitFixedStepHandler {

    /** List of states. */
    private final List<SpacecraftState> states;

    /** Standard constructor.
     * <p>
     * Initialise the internal list.
     * </p>
     */
    public OrbitHandler() {
        // initialise an empty list of orbit
        states = new ArrayList<SpacecraftState>();
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        //Nothing to do.
    }

    /** {@inheritDoc} */
    public void handleStep(final SpacecraftState currentState, final boolean isLast) {
        // fill in the list with the orbit from the current step
        this.states.add(currentState);
    }

    /**
     * Get the list of propagated states.
     *
     * @return the list of states
     */
    public List<SpacecraftState> getStates() {
        return states;
    }
}
