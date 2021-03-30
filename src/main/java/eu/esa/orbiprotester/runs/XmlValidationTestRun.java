/**
 * 
 */
package eu.esa.orbiprotester.runs;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import eu.esa.orbiprotester.utils.KeyValueFileParser;

/** Compare two or more data series read from external files.
 * 
 * @author alin
 *
 */
public class XmlValidationTestRun extends AbstractTestRun {

    /**
     * File to validate.
     */
    private String xmlFile;
    
    /**
     * Schema for validation.
     */
    private String[] schema;
    
    
    public XmlValidationTestRun(KeyValueFileParser<ParameterKey> parser,
            File testFile, File outputFolder, File referenceFolder,
            String testName, Logger logger) {
        super(parser, testFile, outputFolder, referenceFolder, testName, logger);
        this.xmlFile = new String();
        this.schema = new String[0];
    }
    
    /**
     * @see eu.esa.orbiprotester.runs.AbstractTestRun#readInputData()
     */
    @Override
    public void readInputData() throws OrekitException, IOException {

        super.readInputData();
        
        // read the other files, if any
        if (getParser().containsKey(ParameterKey.XML_VALIDATION_FILE)) {
            xmlFile = getParser().getString(ParameterKey.XML_VALIDATION_FILE);
            final File xmlToValidate = new File(getReferenceFolder(), xmlFile);
            schema = getParser().getString(ParameterKey.XML_VALIDATION_SCHEMA).split("[|]");
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            Source[] sources = new Source[]{ 
                    // Add all schemas
                    new StreamSource(getClass().getResource("/EO_OPER_MPL_ORBPRE_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/EO_OPER_MPL_ORBPRE.HeaderTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/EO_OPER_MPL_ORBPRE.DataBlockTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/HeaderTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/OrbitTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/GeoLocationTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/TimeTypes_0300.XSD").toExternalForm()),
                    new StreamSource(getClass().getResource("/BasicTypes_0300.XSD").toExternalForm()),
            };
            
            try {
				factory.setSchema(schemaFactory.newSchema(sources));
			} catch (SAXException e) {
				e.printStackTrace();
			}

            try {
				SAXParser parser = factory.newSAXParser();
				parser.parse(xmlToValidate, new DefaultHandler() {
					  @Override
					  public void error(SAXParseException e) throws SAXException {
					    throw e;
					  }
					});
				
				System.out.println(xmlFile + " is valid.");
			} catch (ParserConfigurationException | SAXException e) {
				e.printStackTrace();
			}
        }
    }

    /* (non-Javadoc)
     * @see eu.esa.orbiprotester.runs.TestRun#runTest()
     */
    @Override
    public void runTest() throws IOException, OrekitException {
    	
    }

    /** No initial orbit exists for this test. 
     *  This method returns null.
     */
    @Override
    protected Orbit createOrbit(boolean secondaryParameters)
            throws IOException, OrekitException {
        return null;
    }
    
    @Override
    public void finalizeTest() throws IOException {
    	
    }

}
