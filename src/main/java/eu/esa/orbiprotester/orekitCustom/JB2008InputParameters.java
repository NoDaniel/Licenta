
package eu.esa.orbiprotester.orekitCustom;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


/** Interface for solar activity and magnetic activity data.
 * <p>Those data are needed by the JB2008 atmosphere model.</p>
 * @author Pascal Parraud
 */
public interface JB2008InputParameters extends Serializable {

    /** Gets the available data range minimum date.
     * @return the minimum date.
     */
    AbsoluteDate getMinDate();

    /** Gets the available data range maximum date.
     * @return the maximum date.
     */
    AbsoluteDate getMaxDate();

    /** Get the value of the instantaneous solar flux index
     *  (1e<sup>-22</sup>*Watt/(m²*Hertz)).
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the instantaneous F10.7 index
     * @exception OrekitException if the date is out of range of available data
     */
    double getF10(AbsoluteDate date) throws OrekitException;

    /** Get the value of the mean solar flux.
     * Averaged 81-day centered F10.7 B index on the input time.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the mean solar flux F10.7B index
     * @exception OrekitException if the date is out of range of available data
     */
    double getF10B(AbsoluteDate date) throws OrekitException;

    /** Get the EUV index (26-34 nm) scaled to F10.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the the EUV S10 index
     * @exception OrekitException if the date is out of range of available data
     */
    double getS10(AbsoluteDate date) throws OrekitException;

    /** Get the EUV 81-day averaged centered index.
     * <p>Tabular time 1.0 day earlier.</p>
     * @param date the current date
     * @return the the mean EUV S10B index
     * @exception OrekitException if the date is out of range of available data
     */
    double getS10B(AbsoluteDate date) throws OrekitException;

    /** Get the MG2 index scaled to F10.
     * <p>Tabular time 2.0 days earlier.</p>
     * @param date the current date
     * @return the the MG2 index
     * @exception OrekitException if the date is out of range of available data
     */
    double getXM10(AbsoluteDate date) throws OrekitException;

    /** Get the MG2 81-day average centered index.
     * <p>Tabular time 2.0 days earlier.</p>
     * @param date the current date
     * @return the the mean MG2 index
     * @exception OrekitException if the date is out of range of available data
     */
    double getXM10B(AbsoluteDate date) throws OrekitException;

    /** Get the Solar X-Ray & Lya index scaled to F10.
     * <p>Tabular time 5.0 days earlier.</p>
     * @param date the current date
     * @return the Solar X-Ray & Lya index scaled to F10
     * @exception OrekitException if the date is out of range of available data
     */
    double getY10(AbsoluteDate date) throws OrekitException;

    /** Get the Solar X-Ray & Lya 81-day ave. centered index.
     * <p>Tabular time 5.0 days earlier.</p>
     * @param date the current date
     * @return the Solar X-Ray & Lya 81-day ave. centered index
     * @exception OrekitException if the date is out of range of available data
     */
    double getY10B(AbsoluteDate date) throws OrekitException;

    /** Get the temperature change computed from Dst index.
     * @param date the current date
     * @return the temperature change computed from Dst index
     * @exception OrekitException if the date is out of range of available data
     */
    double getDSTDTC(AbsoluteDate date) throws OrekitException;

}
