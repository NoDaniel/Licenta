
package eu.esa.orbiprotester.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.Localizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Predefined;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/** Simple parser for key/value files.
 *  <p>
 *  This class is a copy of the one used by the tutorials from the Orekit library,
 *  which is published under the terms of the Apache Software License V2.
 *  </p>
 * @param Key type of the parameter keys
 */
public class KeyValueFileParser<Key extends Enum<Key>> {

    /** Error message for unknown frame. */
    private static final Localizable UNKNOWN_FRAME =
        new DummyLocalizable("unknown frame {0}");

    /** Error message for not Earth frame. */
    private static final Localizable NOT_EARTH_FRAME =
        new DummyLocalizable("frame {0} is not an Earth frame");

    /** Enum type. */
    private final Class<Key> enumType;

    /** Key/value map. */
    private final Map<Key, String> map;

    /** Simple constructor.
     * @param enumType type of the parameters keys enumerate
     */
    public KeyValueFileParser(final Class<Key> enumType) {
        this.enumType = enumType;
        map = new HashMap<Key, String>();
    }

    /** Parse an input file.
     * <p>
     * The input file syntax is a set of key=value lines. Blank lines and lines
     * starting with '#' (after whitespace trimming) are silently ignored. The
     * equal sign may be surrounded by space characters. Keys must correspond to
     * the {@link Key} enumerate constants, given that matching is not
     * case sensitive and that '_' characters may appear as '.' characters in the
     * file. This means that the lines:
     * <pre>
     *   # this is the semi-major axis
     *   orbit.circular.a   = 7231582
     * </pre>
     * are perfectly right and correspond to key {@code Key#ORBIT_CIRCULAR_A} if
     * such a constant exists in the enumerate.
     * </p>
     * @param input input stream
     * @exception IOException if input file cannot be read
     * @exception IllegalArgumentException if a line cannot be read properly
     */
    public void parseInput(final InputStream input) throws IOException, IllegalArgumentException {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.trim();
            // we ignore blank lines and line starting with '#'
            if ((line.length() > 0) && !line.startsWith("#")) {
                final String[] fields = line.split("\\s*=\\s*");
                if (fields.length != 2) {
                    throw new IllegalArgumentException(line);
                }
                final Key key = Key.valueOf(enumType, fields[0].toUpperCase().replaceAll("\\.", "_"));
                map.put(key, fields[1]);
            }
        }
        reader.close();

    }

    /** Check if a key is contained in the map.
     * @param key parameter key
     * @return true if the key is contained in the map
     */
    public boolean containsKey(final Key key) {
        return map.containsKey(key);
    }

    /** Get a raw string value from a parameters map.
     * @param key parameter key
     * @return string value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public String getString(final Key key) throws NoSuchElementException {
        final String value = map.get(key);
        if (value == null) {
            throw new NoSuchElementException(key.toString());
        }
        return value.trim();
    }

    /** Get a raw double value from a parameters map.
     * @param key parameter key
     * @return double value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public double getDouble(final Key key) throws NoSuchElementException {
        return Double.parseDouble(getString(key));
    }

    /** Get a raw int value from a parameters map.
     * @param key parameter key
     * @return int value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public int getInt(final Key key) throws NoSuchElementException {
        return Integer.parseInt(getString(key));
    }

    /** Get a raw boolean value from a parameters map.
     * @param key parameter key
     * @return boolean value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public boolean getBoolean(final Key key) throws NoSuchElementException {
        return Boolean.parseBoolean(getString(key));
    }

    /** Get an angle value from a parameters map.
     * <p>
     * The angle is considered to be in degrees in the file, it will be returned in radians
     * </p>
     * @param key parameter key
     * @return angular value corresponding to the key, in radians
     * @exception NoSuchElementException if key is not in the map
     */
    public double getAngle(final Key key) throws NoSuchElementException {
        return FastMath.toRadians(getDouble(key));
    }

    /** Get a date value from a parameters map.
     * <p>
     * The date is considered to be in UTC in the file
     * </p>
     * @param key parameter key
     * @param scale time scale in which the date is to be parsed
     * @return date value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public AbsoluteDate getDate(final Key key, final TimeScale scale) throws NoSuchElementException {
        return new AbsoluteDate(getString(key), scale);
    }

    /** Get a time value from a parameters map.
     * @param key parameter key
     * @return time value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public TimeComponents getTime(final Key key) throws NoSuchElementException {
        return TimeComponents.parseTime(getString(key));
    }

    /** Get a vector value from a parameters map.
     * @param xKey parameter key for abscissa
     * @param yKey parameter key for ordinate
     * @param zKey parameter key for height
     * @return date value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public Vector3D getVector(final Key xKey, final Key yKey, final Key zKey)
        throws NoSuchElementException {
        return new Vector3D(getDouble(xKey), getDouble(yKey), getDouble(zKey));
    }

    /** Get an inertial frame from a parameters map.
     * @param key parameter key
     * @return inertial frame corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     * @exception OrekitException if frame cannot be built
     */
    public Frame getInertialFrame(final Key key) throws NoSuchElementException, OrekitException {

        // get the name of the desired frame
        final String frameName = getString(key);

        // check the name against predefined frames
        for (Predefined predefined : Predefined.values()) {
            if (frameName.equals(predefined.getName())) {
                if (FramesFactory.getFrame(predefined).isPseudoInertial()) {
                    return FramesFactory.getFrame(predefined);
                } else {
                    throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                              frameName);
                }
            }
        }

        // none of the frames match the name
        throw new OrekitException(UNKNOWN_FRAME, frameName);

    }

    /** Get an Earth frame from a parameters map.
     * <p>
     * We consider Earth frames are the frames with name starting with "ITRF".
     * </p>
     * @param key parameter key
     * @return Earth frame corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     * @exception OrekitException if frame cannot be built
     */
    public Frame getEarthFrame(final Key key)
        throws NoSuchElementException, OrekitException {

        // get the name of the desired frame
        final String frameName = getString(key);

        // check the name against predefined frames
        for (Predefined predefined : Predefined.values()) {
            if (frameName.equals(predefined.getName())) {
                return FramesFactory.getFrame(predefined);
            }
        }

        // none of the frames match the name
        throw new OrekitException(NOT_EARTH_FRAME, frameName);

    }

    /** Get the local frame type.
     * @param key parameter key
     * @return local frame type corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     * @exception OrekitException if frame cannot be built
     */
    public LOFType getLocalFrameType(final Key key)
        throws NoSuchElementException, OrekitException {

        // get the type of the local frame
        final String localFrameType = getString(key);

        // check the name against predefined frames
        LOFType type =  LOFType.valueOf(localFrameType);
        if (type != null) {
            return type;
        } else {
            throw new OrekitException(UNKNOWN_FRAME, localFrameType);  
        }
    }
    
    /** Get a list of celestial bodies
     * @param key the parameter key
     * @return the list of bodies
     * @throws OrekitException if the body cannot be returned
     */
    public List<CelestialBody> getCelectialBodies(final Key key) 
    		throws OrekitException {
    	final String[] bodyNames = getString(key).split("\\|");
    	
    	List<CelestialBody> bodies = new ArrayList<>(bodyNames.length);
    	for (String name : bodyNames) {
    		bodies.add(CelestialBodyFactory.getBody(name.trim()));
    	}
    	
    	return bodies;
    }
}
