package ke.wateratm.application.port.in;

/** Housekeeping: give up on payments M-Pesa never answered, and on orders never paid. */
public interface ExpireStalePaymentsUseCase {

	/** @return how many payments + orders were closed */
	int expireStale();

}
