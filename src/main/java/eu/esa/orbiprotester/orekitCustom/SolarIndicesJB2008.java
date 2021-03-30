package eu.esa.orbiprotester.orekitCustom;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** This class reads and provides solar activity data needed by the 
 * Jacchia-Bowman 2008 atmosphere model. The data are furnished at the <a 
 * href="http://sol.spacenvironment.net/~JB2008/"> official JB2008 website.
 *
 */
@SuppressWarnings("serial")
public class SolarIndicesJB2008 implements JB2008InputParameters{
	
	/** Dates. */
    private ArrayList<Double> D1950 = new ArrayList<Double>();
    
    /** F10 data. */
    private ArrayList<Double> F10 = new ArrayList<Double>();

    /** F81c data. */
    private ArrayList<Double> F81c = new ArrayList<Double>();

    /** S10 data. */
    private ArrayList<Double> S10 = new ArrayList<Double>();

    /** S81c data. */
    private ArrayList<Double> S81c = new ArrayList<Double>();

    /** M10 data. */
    private ArrayList<Double> M10 = new ArrayList<Double>();

    /** M81c data. */
    private ArrayList<Double> M81c = new ArrayList<Double>();

    /** Y10 data. */
    private ArrayList<Double> Y10 = new ArrayList<Double>();

    /** Y10B data. */
    private ArrayList<Double> Y81c = new ArrayList<Double>();
    
    /** DSTDTC data. */
    private ArrayList<Double> DSTDTC = new ArrayList<Double>();
    
    /** The available data range minimum date */
    AbsoluteDate MinDate = null;
    
    /** The available data range maximum date */
    AbsoluteDate MaxDate = null;
    
    /** Default regular expression for JB2008 solar indices file from SET. */
    public static final String SOLAR_INDICES_FILENAME = "^(?:solfsmy|SOLFSMY)\\.(?:txt|TXT)$";
    
    /** Default regular expression for JB2008 geomagnetic storm indices file from SET. */
    public static final String GEOMAGNETIC_STORM_INDICES_FILENAME = "^(?:dtcfile|DTCFILE)\\.(?:txt|TXT)$";
    
    /** Simple constructor.
     * @exception OrekitException
     * @throws FileNotFoundException 
     */
    public SolarIndicesJB2008(DataProvidersManager dataProvidersManager) throws OrekitException, FileNotFoundException {
    	
    	SOLFSMYLoader SOLFSMYLoader = new SOLFSMYLoader();
    	dataProvidersManager.feed(SOLAR_INDICES_FILENAME, SOLFSMYLoader);
    	
    	DTCFileLoader DTCFileLoader = new DTCFileLoader();
    	dataProvidersManager.feed(GEOMAGNETIC_STORM_INDICES_FILENAME, DTCFileLoader);
    	
    	// The available data range minimum and maximum date
    	MinDate = SOLFSMYLoader.MinDate;
    	MaxDate = SOLFSMYLoader.MaxDate;
    	
    	D1950.addAll(SOLFSMYLoader.TC.subList(5, SOLFSMYLoader.TC.size()));
    	// 1 day lag for F10 and S10
    	F10.addAll(SOLFSMYLoader.F10.subList(4, SOLFSMYLoader.F10.size()-1));
    	F81c.addAll(SOLFSMYLoader.F81c.subList(4, SOLFSMYLoader.F81c.size()-1));
    	S10.addAll(SOLFSMYLoader.S10.subList(4, SOLFSMYLoader.S10.size()-1));
    	S81c.addAll(SOLFSMYLoader.S81c.subList(4, SOLFSMYLoader.S81c.size()-1));
    	// 2 day lag for M10
    	M10.addAll(SOLFSMYLoader.M10.subList(3, SOLFSMYLoader.M10.size()-2));
    	M81c.addAll(SOLFSMYLoader.M81c.subList(3, SOLFSMYLoader.M81c.size()-2));
    	// 5 day lag for Y10
    	Y10.addAll(SOLFSMYLoader.Y10.subList(0, SOLFSMYLoader.Y10.size()-5));
    	Y81c.addAll(SOLFSMYLoader.Y81c.subList(0, SOLFSMYLoader.Y81c.size()-5));
    	// delete first 5 days => 5 * 24 records
    	DSTDTC.addAll(DTCFileLoader.DSTDTC.subList(120, DTCFileLoader.DSTDTC.size()));
    }
    
	@Override
	public AbsoluteDate getMinDate() {
		return MinDate;
	}

	@Override
	public AbsoluteDate getMaxDate() {
		return MaxDate;
	}

	@Override
	public double getF10(AbsoluteDate date) throws OrekitException {
		return F10.get(computeSolIndex(date));
	}

	@Override
	public double getF10B(AbsoluteDate date) throws OrekitException {
		return F81c.get(computeSolIndex(date));
	}

	@Override
	public double getS10(AbsoluteDate date) throws OrekitException {
		return S10.get(computeSolIndex(date));
	}

	@Override
	public double getS10B(AbsoluteDate date) throws OrekitException {
		return S81c.get(computeSolIndex(date));
	}

	@Override
	public double getXM10(AbsoluteDate date) throws OrekitException {
		return M10.get(computeSolIndex(date));
	}

	@Override
	public double getXM10B(AbsoluteDate date) throws OrekitException {
		return M81c.get(computeSolIndex(date));
	}

	@Override
	public double getY10(AbsoluteDate date) throws OrekitException {
		return Y10.get(computeSolIndex(date));
	}

	@Override
	public double getY10B(AbsoluteDate date) throws OrekitException {
		return Y81c.get(computeSolIndex(date));
	}

	@Override
	public double getDSTDTC(AbsoluteDate date) throws OrekitException {

		return DSTDTC.get(computeDtcIndex(date));
	}
	
	private int computeSolIndex (AbsoluteDate date) throws OrekitException{
		
		// compute modified julian days date
        double dateMJD   = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY;
        double dateD1950 = dateMJD - 33281.0;
        
        int index = (int) (dateD1950 - D1950.get(0));
        if (index < 0){
        	throw new OrekitException(new DummyLocalizable("Time outside File Start-Stop Time !"));
        }
		
		return index;
	}
	
	private int computeDtcIndex (AbsoluteDate date) throws OrekitException{
		
		// compute modified julian days date
        double dateMJD   = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY;
        double dateD1950 = dateMJD - 33281.0;
        
        int index = (int) ((dateD1950 - D1950.get(0).intValue()) * 24);
        if (index < 0){
        	throw new OrekitException(new DummyLocalizable("Time outside File Start-Stop Time !"));
        }
		
		return index;
	}
	
	   
	/** Reader for JB2008 solar indices files from SET 
	 * */
    private class SOLFSMYLoader implements DataLoader{
    	
    	/** Dates. */
        ArrayList<Double> TC = new ArrayList<Double>();
        
        /** F10 data. */
        ArrayList<Double> F10 = new ArrayList<Double>();

        /** F81c data. */
        ArrayList<Double> F81c = new ArrayList<Double>();

        /** S10 data. */
        ArrayList<Double> S10 = new ArrayList<Double>();

        /** S81c data. */
        ArrayList<Double> S81c = new ArrayList<Double>();

        /** M10 data. */
        ArrayList<Double> M10 = new ArrayList<Double>();

        /** M81c data. */
        ArrayList<Double> M81c = new ArrayList<Double>();

        /** Y10 data. */
        ArrayList<Double> Y10 = new ArrayList<Double>();

        /** Y10B data. */
        ArrayList<Double> Y81c = new ArrayList<Double>();
        
        /** The available data range minimum date */
        AbsoluteDate MinDate = null;
        
        /** The available data range maximum date */
        AbsoluteDate MaxDate = null;

		@Override
		public boolean stillAcceptsData() {
			return TC.isEmpty();
		}

		@Override
		public void loadData(InputStream input, String name) throws IOException, ParseException, OrekitException {
			
			// Create reader from input stream
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
			// local variables to use for acquiring start date and end date of records
			boolean firstDate = true;
			int Year          = 0;
			int Day           = 0;
			
			// for each line of the file
			for (String line = reader.readLine(); line != null; line = reader.readLine()){
				
				// ignore lines starting with #
				if (line.trim().charAt(0) != '#'){
					String[] flux = line.trim().split("\\s+");
					
					Year = Integer.parseInt(flux[0]);
					Day  = Integer.parseInt(flux[1]);
					TC.add(yearDay2D1950(Year, Day));
		            F10.add(Double.parseDouble(flux[3]));
		            F81c.add(Double.parseDouble(flux[4]));
		            S10.add(Double.parseDouble(flux[5]));
		            S81c.add(Double.parseDouble(flux[6]));
		            M10.add(Double.parseDouble(flux[7]));
		            M81c.add(Double.parseDouble(flux[8]));
		            Y10.add(Double.parseDouble(flux[9]));
		            Y81c.add(Double.parseDouble(flux[10]));
		            
		            // first record
		            if (firstDate) {
		            	firstDate = false;
		            	MinDate = new AbsoluteDate(new DateComponents(Year, Day), new TimeComponents(0, 0, 0.0),
                                TimeScalesFactory.getTT());
		            }
				}
			}
			
			// last record
			MaxDate = new AbsoluteDate(new DateComponents(Year, Day), new TimeComponents(0, 0, 0.0),
                    TimeScalesFactory.getTT());
		}
    	
    }
    
    private class DTCFileLoader implements DataLoader{
    	
    	/** Dates. */
        ArrayList<Double> TC = new ArrayList<Double>();
        
        /** DSTDTC data. */
        ArrayList<Double> DSTDTC = new ArrayList<Double>();

		@Override
		public boolean stillAcceptsData() {
			return TC.isEmpty();
		}

		@Override
		public void loadData(InputStream input, String name) throws IOException, ParseException, OrekitException {

			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			
			for (String line = reader.readLine(); line != null; line = reader.readLine()){
				
				if (line.trim().charAt(0) != '#'){				
					
					String strYear = line.trim().substring(3, 6).trim();
					String strDay = line.trim().substring(6, 9).trim();
					TC.add(yearDay2D1950(Integer.parseInt(strYear), Integer.parseInt(strDay)));
					
					String[] flux = line.trim().substring(9).trim().split("\\s+");
					if (flux.length == 24){
						for(String fluxi:flux){
							DSTDTC.add(Double.parseDouble(fluxi));
						}
					}else{
						throw new OrekitException(new DummyLocalizable("Data is missing from DTC file."));
					}
				}
			}
		}
    }
    
    /** Convert time to days since 1950 JAN 0 12:00:00.000 
     * 
     * @param year
     * @param dayOfYear
     * @return
     */
    protected static double yearDay2D1950(int year, int dayOfYear){
        if (year > 1000){
            year = year - 1900;
        }
        if (year < 50){
            year = year + 100;
        }
        int IYY = (year - 1)/4 - 12;
        IYY     = (year - 50) * 365 + IYY;
    	
		return IYY + dayOfYear + 0.5;
    }

}
