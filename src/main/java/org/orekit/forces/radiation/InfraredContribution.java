package org.orekit.forces.radiation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;


public class InfraredContribution extends AbstractRadiationForceModel {

	/**
	 * Infrared contribution type
	 * radiationType@forces.radiation.RadiationType; - matlab
	 */
	public org.orekit.forces.radiation.RadiationType radiationType;
	
	/**
	 * The shape of the central body.
	 * bodyShape@bodies.OneAxisEllipsoid; - matlab
	 */
	public org.orekit.bodies.OneAxisEllipsoid bodyShape;
	
	/**
	 * The space craft model
	 * spacecraft@forces.spacecraft.AbstractSpacecraft; - matlab
	 * https://www.orekit.org/static/apidocs/org/orekit/forces/radiation/RadiationSensitive.html
	 */
	public RadiationSensitive spacecraft;
	
	/**
	 * Sun model
	 * sun@utils.PVCoordinatesProvider;  - matlab
	 */
	private final org.orekit.utils.ExtendedPVCoordinatesProvider sun;
	
	/**
	 * Grid spacing for latitude
	 * latitudeDeg@double;
	 */
	public double latituteDeg;
	
	
	/**
	 * Grid spacing for longitude
	 * longitudeDeg@double;
	 */
	public double longitudeDeg;
	
	/**
	 * Resolution of the model
	 * sx@double;  
	 */
	public double sx;
	
	/**
	 * Resolution of the model
	 * sy@double; 
	 */
	public double sy;
	
	/**
	 * solarPressure@double;
	 */
	public double solarPressure;
	
	/**
	 * rawAlbedo@double;
	 */
	public double rawAlbedo;
	
	/**
	 * INFRAREDCONTRIBUTION Constructors
	 * 
	 * EMISS = forces.radiation.InfraredContribution(radiationType,bodyShape,spacecraft,sun);
	 * 
	 * EMISS = forces.radiation.InfraredContribution(radiationType,bodyShape,spacecraft,sun,latitudeDegree,longitudeDegree);
	 * 
	 * 
	 * Parameters:
	 * @param radiationType - the planet for which the emissivity should be computed
	 * @param bodyShape - the shape of the central body
	 * @param spacecraft - the spacecraft model
	 * @param sun - the sun model
	 * @param latitudeDegree - grid spacing for latitude, only for 6 parameters constructor
	 * @param longituteDegree	- grid spacing for longitude, only for 6 parameters contructor
	 * 
	 * 
	 * If no grid spacing is provided for both latitude and longitude
	 * the constructor loads the default grid spacing(10 degrees).
	 * 
	 */
	public InfraredContribution( RadiationType radiationType, 
			OneAxisEllipsoid bodyShape, 
			RadiationSensitive spacecraft, 
			ExtendedPVCoordinatesProvider sun) 
	{
		super(sun, bodyShape.getEquatorialRadius());
		this.radiationType = radiationType;
		this.bodyShape = bodyShape;
		this.spacecraft = spacecraft;
		this.sun = sun;
		this.latituteDeg = org.orekit.forces.radiation.SolarRadiationConstants.LATITUDE_DEGREE;
		this.longitudeDeg = org.orekit.forces.radiation.SolarRadiationConstants.LONGITUDE_DEGREE;;
		this.sx = Math.toDegrees(2 * Math.PI / this.longitudeDeg);
		this.sy = Math.toDegrees(Math.PI / this.latituteDeg);
		
		double flux = 0.0;
		
		switch(this.radiationType)
		{
		case EARTH:
			this.solarPressure = org.orekit.forces.radiation.SolarRadiationConstants.P_REF;
			this.rawAlbedo = -1;
			break;
		case MERCURY:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_MERCURY);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_MERCURY;
			break;
		case VENUS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_VENUS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_VENUS;
			break;
		case MARS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_MARS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_MARS;
			break;
		case JUPITER:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_JUPITER);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_JUPITER;
			break;
		case SATURN:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_SATURN);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_SATURN;
			break;
		case URANUS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_URANUS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_URANUS;
			break;
		case NEPTUNE:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_NEPTUNE);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_NEPTUNE;
			break;
		}
	}
	
	public InfraredContribution( RadiationType radiationType, 
			OneAxisEllipsoid bodyShape, 
			RadiationSensitive spacecraft, 
			ExtendedPVCoordinatesProvider sun,
			double latitudeDeg,
			double longitudeDeg)
	{
		super(sun, bodyShape.getEquatorialRadius());
		
		this.radiationType = radiationType;
		this.bodyShape = bodyShape;
		this.spacecraft = spacecraft;
		this.sun = sun;
		this.latituteDeg = latitudeDeg;
		this.longitudeDeg = longitudeDeg;
		this.sx = Math.toDegrees(2 * Math.PI / this.longitudeDeg);
		this.sy = Math.toDegrees(Math.PI / this.latituteDeg);
		
		double flux = 0.0;
		
		switch(this.radiationType)
		{
		case EARTH:
			this.solarPressure = org.orekit.forces.radiation.SolarRadiationConstants.P_REF;
			this.rawAlbedo = -1;
			break;
		case MERCURY:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_MERCURY);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_MERCURY;
			break;
		case VENUS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_VENUS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_VENUS;
			break;
		case MARS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_MARS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_MARS;
			break;
		case JUPITER:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_JUPITER);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_JUPITER;
			break;
		case SATURN:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_SATURN);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_SATURN;
			break;
		case URANUS:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_URANUS);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_URANUS;
			break;
		case NEPTUNE:
			flux = org.orekit.forces.radiation.SolarRadiationConstants.SOLAR_RADIATION * 
					(1 / org.orekit.forces.radiation.SolarRadiationConstants.SUN_NEPTUNE);
			this.solarPressure = flux / org.orekit.utils.Constants.SPEED_OF_LIGHT;
			this.rawAlbedo = org.orekit.forces.radiation.SolarRadiationConstants.ALBEDO_NEPTUNE;
			break;
		}
	}
	
    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

	
	@Override
	public boolean dependsOnPositionOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Compute the contribution of the force model to the perturbing acceleration
	 * @param SpaceCraft
	 * @param parameters
	 */
	public Vector3D acceleration(final SpacecraftState spaceCraft, final double[] parameters) {
		// TODO Auto-generated method stub
		
		final AbsoluteDate date         = spaceCraft.getDate();
        final Frame        frame        = spaceCraft.getFrame();
        final Vector3D     position     = spaceCraft.getPVCoordinates().getPosition();
		Vector3D flux = Vector3D.ZERO;
		
		for(int i = 1; i <= this.sy; i++)
		{
			for(int j = 1; j <= this.sx; j++)
			{
				double emiss;
				//[phi, theta]
				double[] angles = indexToRadian(i, j, this.sy, this.sx);
				
				double R = this.bodyShape.getEquatorialRadius();
				Vector3D gridCartesianPoint = new Vector3D(R * Math.cos(angles[0]) * Math.cos(angles[1]),
														   R * Math.cos(angles[0]) * Math.sin(angles[1]),
														   R * Math.sin(angles[0]));
				double satGridAngle = Vector3D.angle(gridCartesianPoint, position);
				if( Math.cos(satGridAngle) >= 0)
				{
					if(this.radiationType == org.orekit.forces.radiation.RadiationType.EARTH)
					{
						emiss = this.computeEmissivity(date, angles[0]);
					}
					else
					{
						emiss = 1 - this.rawAlbedo;
					}
					double gridArea = computeArea(this.bodyShape.getEquatorialRadius(), i, this.sy, this.sx);
					double satGridDistance = Vector3D.distance(position, gridCartesianPoint);
					//double satGridDistance = Vector3D.distanceSq(position, gridCartesianPoint);
					double Eincident = this.solarPressure * gridArea;
					
					//Vector3D positionAux = position;
					flux = flux.add( emiss / (4 * Math.PI * Math.pow(satGridDistance, 2))*(
										  Eincident * Math.cos(satGridAngle)), position.subtract(gridCartesianPoint).normalize());
					
				}
			}
		}
        
		
        Vector3D acceleration = spacecraft.radiationPressureAcceleration(date, frame, position, spaceCraft.getAttitude().getRotation(),
                spaceCraft.getMass(), flux, parameters);
		return acceleration;
	}

	@Override
	public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	/**
	 * Same as SolarRadiationPressure
	 */
	public ParameterDriver[] getParametersDrivers() {
		
		return spacecraft.getRadiationParametersDrivers();
	}
	
	
	/**
	 * InfraredContribution private functions
	 * Added as private functions for performance only
	 */
	
	private double[] indexToRadian(int i, int j, double sy, double sx)
	{
		double[] retVect = new double[2];
		double ux = 2 * Math.PI / sx;
		double uy = Math.PI / sy;
		
		retVect[0] = (-1) * (Math.PI / 2) + (i - 0.5) * uy;
		retVect[1] = Math.PI - (j - 0.5) * ux;
		
		return retVect;
		
	}
	
	/**
	 * COMPUTEEMISSIVITY Compute the Earth emissivity using P. Knocke emissivity model 
	 * (Earth Radiation Pressure Effects on Satellites, P. Knocke, CSR-89-1, May 1989) 
	 * @param date
	 * @param phi
	 * @return 
	 */
	private double computeEmissivity(AbsoluteDate date, double phi)
	{
			
		double JD = date.durationFrom(org.orekit.forces.radiation.SolarRadiationConstants.BASE_EPOCH);
		double e0 = 0.68;
		double k0 = 0;
		double k1 = -0.07;
		double k2 = 0;
		double e2 = -0.18;
		
		/**
		 * Compute e1
		 */
		double e1 = k0 + k1 * Math.cos(org.orekit.forces.radiation.SolarRadiationConstants.ORBITAL_PULSATION * JD) +
			             k2 * Math.sin(org.orekit.forces.radiation.SolarRadiationConstants.ORBITAL_PULSATION * JD);
		/**
		 * Compute the earth albedo for the given latitude and date
		 */
		return e0 + e1 * Math.sin(phi) + e2 * 1 / 2 * (3 * Math.pow(Math.sin(phi), 2) - 1);
	}
	
	/**
	 * COMPUTEAREA Calculates the area of the surface
	 * @param radius
	 * @param i
	 * @param sy
	 * @param sx
	 * @return
	 */
	private double computeArea(double radius, int i, double sy, double sx)
	{
		return (4 * Math.PI * Math.pow(radius, 2))/ sx * Math.sin(Math.PI/2/sy) * Math.sin(((i - 0.5) * Math.PI) / sx);
	}

   
}
