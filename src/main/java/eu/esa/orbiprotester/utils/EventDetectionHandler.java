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
package eu.esa.orbiprotester.utils;

import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;

/**
 * This counter keeps track of an event occurrence.
 *
 */
public class EventDetectionHandler implements EventHandler<EventDetector> {

    /** The list of spacecraft states where the event was triggered. */
    private List<SpacecraftState> states;

    /** The direction of the variation of the event function. */
    private Direction direction;
    
    /** Constructor.
     */
    public EventDetectionHandler(Direction direction) {

        this.direction = direction;
        this.states = new ArrayList<>();
    }

    /** Constructor.
     */
    public EventDetectionHandler() {

        this.direction = Direction.ALL;
        this.states = new ArrayList<>();
    }    
    
    /**
     * @param direction the direction to set
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /** Add the state to the internal list.
     *
     * @param s the current state
     * @param detector the event detector
     * @param increasing the sign of the variation
     * @return always CONTINUE
     * @throws OrekitException in case of an error
     *
     * @see org.orekit.propagation.events.EclipseDetector#eventOccurred(org.orekit.propagation.SpacecraftState, boolean)
     */
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing)
        throws OrekitException {

        // Save the state if required
        if ((this.direction == Direction.ALL) ||
             (increasing && (this.direction == Direction.INCREASING)) ||
             (!increasing && (this.direction == Direction.DECREASING))) {
            this.states.add(s);
        }
        
        // Always continue
        return Action.CONTINUE;
    }

    /**
     * Return the old state.
     *
     * @param detector
     *            the detector
     * @param oldState
     *            the old state
     * @return return the old state
     * @see org.orekit.propagation.events.handlers.EventHandler#resetState(org.orekit.propagation.events.EventDetector,
     *      org.orekit.propagation.SpacecraftState)
     */
    public SpacecraftState resetState(final EventDetector detector,
            final SpacecraftState oldState) {
        return oldState;
    }

    /**
     * Get the states where the event took place.
     *
     * @return the list of states where the event was triggered
     */
    public List<SpacecraftState> getStates() {
        return this.states;
    }
    
    
    /**
     * The direction of the variation of the function used for event detection.
     */
    public static enum Direction {
        INCREASING,
        DECREASING,
        ALL
    }
    
}
