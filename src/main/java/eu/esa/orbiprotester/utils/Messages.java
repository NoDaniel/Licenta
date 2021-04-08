
package eu.esa.orbiprotester.utils;

/** Log messages and other common texts.
 * @author Bouleanu Daniel
 *
 */
public interface Messages {
    /** Orekit data series label. */
    String OREKIT_LABEL = "Actual";

    /** Orbipro data series label. */
    String ORBIPRO_LABEL = "Reference";

    /** Potential order cannot be higher than potential degree. */
    String ORDER_BIGGER_THAN_DEGREE = "Potential order cannot be higher than potential degree.";

    /** Orbit definition is incomplete. */
    String ORBIT_INCOMPLETE = "Orbit definition is incomplete.";

    /** Printing charts ... */
    String PRINTING_CHARTS = "Printing charts ...";

    /** Charts printed!. */
    String CHARTS_PRINTED_OK = "Charts printed!";

    /** Cannot create chart because at least one data series is null!. */
    String CHARTS_PRINTED_ERR = "Cannot create chart because at least one data series is null!";

    /** Running Orekit propagation .... */
    String RUN_OREKIT_PROP = "Running Orekit propagation ....";

    /** Orekit propagation ended prematurely. Only partial results may be available. */
    String OREKIT_PROP_END_PREMATURELY = "Orekit propagation ended prematurely. Only partial results may be available.";

    /** Orekit execution time (us):. */
    String EXEC_OREKIT_TIME = "Orekit execution time (s): ";

    /** Cannot create output folder. */
    String ERR_CREATE_OUT_FOLDER = "Cannot create output folder";
    
    /** A central body must be defined. */
    String ERR_NO_CENTRAL_BODY = "A central body must be defined";

    /** A required parameter is not set inside the configuration file. */
    String ERR_REQUIRED_PARAMETER_NOT_SET = "The parameter {0} is required due to configuration but was not set!";
    
    /** The maneuver file doesn't contain exactly two dates. */
    String ERR_DATE_LINES = "The maneuver file doesn't contain exactly two dates.";
    
    /** The configured filter for the tabulated attitude is not corectThe configured filter for the tabulated attitude is not correct. */
    String ERR_UNKNOWN_FILTER = "The configured filter for the tabulated attitude is not correct";
    
    /** The number of additional data series files must be equal to the number of labels. */
    String ERR_INCONSISTENT_SERIES = "The number of additional data series files must be equal to the number of labels";
    
    /** The flatness and equatorial radius of the central body must be defined!. */
    String ERR_INCOMPLETE_CENTRAL_BODY = "The flatness and equatorial radius of the central body must be defined!";
}
