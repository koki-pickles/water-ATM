package ke.wateratm.domain.model;

/** A GPS position, e.g. the pin a customer drops for delivery. */
public record GeoPoint(double lat, double lng) {

	private static final double EARTH_RADIUS_KM = 6371.0;

	public GeoPoint {
		if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
			throw new IllegalArgumentException("Invalid GPS position: " + lat + ", " + lng);
		}
	}

	/** Straight-line distance in kilometres (haversine formula). */
	public double distanceKmTo(GeoPoint other) {
		double dLat = Math.toRadians(other.lat - lat);
		double dLng = Math.toRadians(other.lng - lng);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat))
				* Math.cos(Math.toRadians(other.lat)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
	}

}
