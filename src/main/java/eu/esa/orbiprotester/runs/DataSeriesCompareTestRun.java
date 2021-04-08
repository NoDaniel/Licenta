
package eu.esa.orbiprotester.runs;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;

import eu.esa.orbiprotester.utils.ChartDataHolder;
import eu.esa.orbiprotester.utils.KeyValueFileParser;
import eu.esa.orbiprotester.utils.Messages;

/** Compare two or more data series read from external files.
 * 
 * @author Bouleanu Daniel
 *
 */
public class DataSeriesCompareTestRun extends AbstractTestRun {

    /**
     * The list of other data files.
     */
    private String[] dataFiles;
    
    /**
     * The labels for the other series.
     */
    private String[] datalabels;
    
    
    public DataSeriesCompareTestRun(KeyValueFileParser<ParameterKey> parser,
            File testFile, File outputFolder, File referenceFolder,
            String testName, Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);
        this.dataFiles = new String[0];
        this.datalabels = new String[0];
    }
    
    /**
     * @see eu.esa.orbiprotester.runs.AbstractTestRun#readInputData()
     */
    @Override
    public void readInputData() throws OrekitException, IOException {

        super.readInputData();
        
        // read the other files, if any
        if (getParser().containsKey(ParameterKey.DATA_SERIES_FILES)) {
            dataFiles = getParser().getString(ParameterKey.DATA_SERIES_FILES).split("[|]");
            datalabels = getParser().getString(ParameterKey.DATA_SERIES_LABELS).split("[|]");
            
            if (dataFiles.length != datalabels.length) {
                throw new IOException(Messages.ERR_INCONSISTENT_SERIES);
            }
        }
    }

    /* (non-Javadoc)
     * @see eu.esa.orbiprotester.runs.TestRun#runTest()
     */
    @Override
    public void runTest() throws IOException, OrekitException {
        // get the main data series (defined as the reference)
        ChartDataHolder.FileDataHolder referenceData = loadDataSeriesFile(getReferenceFileName(), getReferenceLabel(), false);
        
        // loop thrugh the other data sets
        for (int i = 0; i<dataFiles.length; i++) {
            final String dataFile = dataFiles[i];
            final String dataLabel = datalabels[i];
            
            // add the data to the chart
            ChartDataHolder.FileDataHolder dataSeries = loadDataSeriesFile(dataFile, dataLabel, false);
            
            // create the special charts
            createSpecialCharts(referenceData, dataSeries, getReferenceLabel(), dataLabel);
        }
    }

    /** No initial orbit exists for this test. 
     *  This method returns null.
     */
    @Override
    protected Orbit createOrbit(boolean secondaryParameters)
            throws IOException, OrekitException {
        return null;
    }

}
