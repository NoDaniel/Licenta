package org.orekit.forces.radiation;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;

public interface SolarRadiationConstants {

	/** 
	 * Orbital pulsation
	 */
	double ORBITAL_PULSATION = 2 * Math.PI / 365.25;
	
	/**
	 * December 22, 1981
	 */
	org.orekit.time.AbsoluteDate BASE_EPOCH = new AbsoluteDate(new DateComponents(1981, 12, 22), TimeScalesFactory.getUTC());
	
	/**
	 *  Solar Radiation
	 */
	double SOLAR_RADIATION = 1366;
	
	/** 
	 * Average albedo for Mercury 
	 */
	double ALBEDO_MERCURY = 0.106;
	
	/** 
	 * Average albedo for Venus 
	 */
	double ALBEDO_VENUS = 0.65;
	
	/** 
	 * Average albedo for Mars 
	 */
	double ALBEDO_MARS = 0.15;
	
	/** 
	 * Average albedo for Jupiter 
	 */
	double ALBEDO_JUPITER = 0.52;
	
	/** 
	 * Average albedo for Saturn 
	 */
	double ALBEDO_SATURN = 0.47;
	
	/** 
	 * Average albedo for Uranus 
	 */
	double ALBEDO_URANUS = 0.51;
	
	/** 
	 * Average albedo forNeptune 
	 */
	double ALBEDO_NEPTUNE = 0.41;
	
	
	/** 
	 * Average emissivity for Mercury 
	 */
	double IR_MERCURY = 442;
	
	/** 
	 * Average emissivity for Venus 
	 */
	double IR_VENUS = 231.7;
	
	/** 
	 * Average emissivity for Mars 
	 */
	double IR_MARS = 210.1;
	
	/** 
	 * Average emissivity for Jupiter 
	 */
	double IR_JUPITER = 110.0;
	
	/** 
	 * Average emissivity for Saturn 
	 */
	double IR_SATURN = 81.1;
	
	/** 
	 * Average emissivity for Uranus 
	 */
	double IR_URANUS = 58.2;
	
	/** 
	 * Average emissivity for Neptune 
	 */
	double IR_NEPTUNE = 46.6;
	
	
	/** 
	 * Reference solar radiation pressure at 1AU (Earth-Sun distance) 
	 */
	double P_REF = 4.56e-6;
	
	/** 
	 * Default grid spacing 
	 */
	double LATITUDE_DEGREE = 10;
	
	/** 
	 * Default grid spacing 
	 */
	double LONGITUDE_DEGREE = 10;
	
	
	/** 
	 * Sun - Mercury distance (AU) 
	 */
	double SUN_MERCURY = 0.387;
	
	/** 
	 * Sun - Venus distance (AU) 
	 */
	double SUN_VENUS = 0.723;
	
	/** 
	 * Sun - Mars distance (AU) 
	 */
	double SUN_MARS = 1.524;
	
	/** 
	 * Sun - Jupiter distance (AU) 
	 */
	double SUN_JUPITER= 5.203;
	
	/** 
	 * Sun - Saturn distance (AU) 
	 */
	double SUN_SATURN = 9.523;
	
	/** 
	 * Sun - Uranus distance (AU) 
	 */
	double SUN_URANUS = 19.208;
	
	/** 
	 * Sun - Neptune distance (AU) 
	 */
	double SUN_NEPTUNE = 39.746;
	
}
