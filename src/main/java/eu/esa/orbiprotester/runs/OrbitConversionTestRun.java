/**
 * 
 */
package eu.esa.orbiprotester.runs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.log4j.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;

import eu.esa.orbiprotester.utils.KeyValueFileParser;

/**
 * @author lucian
 *
 */
public class OrbitConversionTestRun extends AbstractTestRun {

    /** The target type for the conversion test. */
    OrbitType targetType;
    
    public OrbitConversionTestRun(KeyValueFileParser<ParameterKey> parser,
            File testFile, File outputFolder, File referenceFolder,
            String testName, Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);
    }

    
    
    /** Read the conversion test input data
     * @see eu.esa.orbiprotester.runs.AbstractTestRun#readInputData()
     */
    @Override
    public void readInputData() throws OrekitException, IOException {
        super.readInputData();
        
        // get the target type
        final String targetStr = getParser().getString(ParameterKey.ORBIT_CONVERSION_TARGET);
        targetType = OrbitType.valueOf(targetStr);
    }



    /** run the Conversion test
     * @see eu.esa.orbiprotester.runs.TestRun#runTest()
     */
    @Override
    public void runTest() throws IOException, OrekitException {
        
        // perform the Orekit conversion
        Orbit orekitOrbit = targetType.convertType(getOrbit());
        
        // load the reference file
        final File referenceFile = new File(getReferenceFolder(), getReferenceFileName());
        if (!referenceFile.exists()) {
            throw new IOException("The reference file (" + referenceFile.getAbsolutePath() + ") is unreachable!");
        }

        getLogger().info("Reading reference file " + getReferenceFileName());
        final double[] orbiproElements = loadReferenceOrbit(referenceFile);
        getLogger().info("Reference file " + getReferenceFileName() + " read succesfully!");        
        
        // compare the orbits
        // build the output file name
        final File outputFile = new File(getOutputFolder(), getTestName() + ".csv");
        final String[] labels = new String[6];
        
        final double[] orekitElements = extractOrbitalElements(orekitOrbit, labels);
        
        compareOrbits(orekitElements, orbiproElements, labels, outputFile);
    }
    
    
    
    /** Nothing to do.
     * @see eu.esa.orbiprotester.runs.AbstractTestRun#finalizeTest()
     */
    @Override
    public void finalizeTest() throws IOException {
        //Nothing to do.
    }



    /** Read the reference file and extract the orbit defined there
     * @param referenceFile the reference file
     * @return the orbital parameters for the orbit 
     * @throws IOException
     */
    private double[] loadReferenceOrbit(final File referenceFile) throws IOException {
        
        try(BufferedReader br = new BufferedReader(new FileReader(referenceFile))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                final StringTokenizer st = new StringTokenizer(line);
                if (st.countTokens() != 6) {
                    throw new IOException("Invalid file. It must contain one data line with 6 tokens!");
                }
                
                // extract the elements from the tokens
                final double[] elements = new double[] {
                        Double.parseDouble(st.nextToken()),
                        Double.parseDouble(st.nextToken()),
                        Double.parseDouble(st.nextToken()),
                        Double.parseDouble(st.nextToken()),
                        Double.parseDouble(st.nextToken()),
                        Double.parseDouble(st.nextToken())
                };
                
                return elements;
            }
            
        }
        
        return null;
    }

    /** Extract the orbital elements from the Orekit orbit based on the type.
     * @param orekitOrbit the orekit orbit
     * @return the orbital elements
     */
    private double[] extractOrbitalElements(final Orbit orekitOrbit, final String[] labels) {
        final double[] orekitElements = new double[6];
        
        switch(targetType) {
            case KEPLERIAN:
                KeplerianOrbit ko = (KeplerianOrbit) orekitOrbit;
                orekitElements[0] = ko.getA() / 1000.;
                orekitElements[1] = ko.getE();
                orekitElements[2] = FastMath.toDegrees(MathUtils.normalizeAngle(ko.getI(), FastMath.PI));
                orekitElements[3] = FastMath.toDegrees(MathUtils.normalizeAngle(ko.getRightAscensionOfAscendingNode(), FastMath.PI));
                orekitElements[4] = FastMath.toDegrees(MathUtils.normalizeAngle(ko.getPerigeeArgument(), FastMath.PI));
                orekitElements[5] = FastMath.toDegrees(MathUtils.normalizeAngle(ko.getAnomaly(PositionAngle.MEAN), FastMath.PI));
                
                labels[0] = "A (Km)";
                labels[1] = "E";
                labels[2] = "I (deg)";
                labels[3] = "RAAN (deg)";
                labels[4] = "PA (deg)";
                labels[5] = "MA (deg)";
            break;
            case EQUINOCTIAL:
                EquinoctialOrbit eo = (EquinoctialOrbit) orekitOrbit;
                orekitElements[0] = eo.getA() / 1000.;
                orekitElements[1] = eo.getEquinoctialEy();
                orekitElements[2] = eo.getEquinoctialEx();
                orekitElements[3] = eo.getHy();
                orekitElements[4] = eo.getHx();
                orekitElements[5] = FastMath.toDegrees(MathUtils.normalizeAngle(eo.getL(PositionAngle.MEAN), FastMath.PI));

                labels[0] = "A (Km)";
                labels[1] = "H/Ey";
                labels[2] = "K/Ex";
                labels[3] = "P/Hy";
                labels[4] = "Q/Hx";
                labels[5] = "LM (deg)";
            break;
            case CARTESIAN:
                CartesianOrbit co = (CartesianOrbit) orekitOrbit;
                orekitElements[0] = co.getPVCoordinates().getPosition().getX() / 1000.;
                orekitElements[1] = co.getPVCoordinates().getPosition().getY() / 1000.;
                orekitElements[2] = co.getPVCoordinates().getPosition().getZ() / 1000.;
                orekitElements[3] = co.getPVCoordinates().getVelocity().getX() / 1000.;
                orekitElements[4] = co.getPVCoordinates().getVelocity().getY() / 1000.;
                orekitElements[5] = co.getPVCoordinates().getVelocity().getZ() / 1000.;

                labels[0] = "Px (Km)";
                labels[1] = "Py (Km)";
                labels[2] = "Pz (Km)";
                labels[3] = "Vx (Km/s)";
                labels[4] = "Vy (Km/s)";
                labels[5] = "Vz (Km/s)";
                
            break;
        default:
            break;
        }
        return orekitElements;
    }
    
    /** Compare the orbital elements obtained with orekit and orbipro
     * @param orekitElements the orekit elements
     * @param orbiproElements the orbipro elements
     * @param labels the labels for the elements compared
     * @param outputFile the output csv file where the elements are printed
     * @throws IOException 
     */
    private void compareOrbits(final double[] orekitElements, final double[] orbiproElements, final String[] labels, final File outputFile) throws IOException {
        try(final BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));) {
            buffer.write(", Orekit, Orbipro, |Difference|");
            buffer.newLine();
            double diff;

            for (int i = 0; i < 6; i++) {
                diff = FastMath.abs(orekitElements[i] - orbiproElements[i]);
                buffer.write(labels[i] +", " + orekitElements[i] + ", " + orbiproElements[i] + ", " + diff);
                buffer.newLine();
            }
        }

    }
}
