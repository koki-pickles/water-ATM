package ke.wateratm.domain.exception;

public class NoShopNearbyException extends RuntimeException {

	public NoShopNearbyException() {
		super("No partner shop delivers to this location yet");
	}

}
