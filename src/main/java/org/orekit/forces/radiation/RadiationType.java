package org.orekit.forces.radiation;

public enum RadiationType{
	EARTH(0), 
	MERCURY(1),
	VENUS(2),
	MARS(3),
	JUPITER(4),
	SATURN(5), 
	URANUS(6),
	NEPTUNE(7);
	
	private final Integer value;
	
	RadiationType(int value)
	{
		this.value = value;
	}
	
	public Integer getValue()
	{
		return value;
	}
}