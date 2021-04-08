
package eu.esa.orbiprotester.runs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import eu.esa.orbiprotester.utils.KeyValueFileParser;

/** Utility class used to build the tests.
 *
 * @author Bouleanu Daniel
 */
public final class TestRunFactory {

    /**
     * Private constructor. This class cannot be instantiated.
     */
    private TestRunFactory() {
        //Nothing to do
    }

    /** Build a test run.
     *
     * @param input the input test file
     * @param baseOutputFolder the root folder for the output
     * @param referenceFolder the folder containing the reference files.
     * @param logger the logger
     * @return the test run object
     * @throws IOException if the input file is invalid
     */
    public static TestRun buildTestRun(final File input, final File baseOutputFolder, final File referenceFolder, final Logger logger) throws IOException {
        TestRun testRun = null;

        //Check if the input file exists
        if (!input.exists()) {
            throw new IOException("Simulation input file " + input.getName() + ".in does not exists!");
        }

        // read input parameters
        final KeyValueFileParser<ParameterKey> parser = new KeyValueFileParser<ParameterKey>(
                ParameterKey.class);
        parser.parseInput(new FileInputStream(input));

        // Determine the type of the test
        TestType type = TestType.STANDARD;

        if(parser.containsKey(ParameterKey.ORBIT_CONVERSION) && parser.getBoolean(ParameterKey.ORBIT_CONVERSION)) {
            type = TestType.ORBITCONVERSION;
        } else if (parser.containsKey(ParameterKey.DATA_SERIES_COMPARE) && parser.getBoolean(ParameterKey.DATA_SERIES_COMPARE)) {
            type = TestType.DATASERIESCOMPARE;
        }else if (parser.containsKey(ParameterKey.XML_VALIDATION) && parser.getBoolean(ParameterKey.XML_VALIDATION)) {
        	type = TestType.XMLVALIDATION;
        }

        
        
        //TODO add the rest

        //Extract simulation name
        String testName = input.getName();
        if (testName.lastIndexOf('.') != -1) {
            testName = testName.substring(0, testName.lastIndexOf('.'));
        }

        // Build the output folder
        final File outputFolder = new File (baseOutputFolder, testName);
        if (!outputFolder.mkdirs()) {
            throw new IOException("Cannot create output folder " + outputFolder.getAbsolutePath());
        }

        switch (type) {
        case STANDARD:
        default:
            testRun = new StandardTestRun(parser, input, outputFolder, referenceFolder, testName, logger);
            break;
        }

        return testRun;
    }

    /** The type of the test.
     *
     * @author Lucian Barbulescu
     */
    private static enum TestType {
        /** The standard test where a Orekit run is compared with a Orbipro run. */
        STANDARD,
        /** Event occurence test. */
        EVENT,
        /** Formation flight test. */
        FORMATION,
        /** Orbit conversion test. */
        ORBITCONVERSION,
        /** Two or more external data sets comparison. */
        DATASERIESCOMPARE,
        /** XML file validation with XSD. */
        XMLVALIDATION,
    }
}
