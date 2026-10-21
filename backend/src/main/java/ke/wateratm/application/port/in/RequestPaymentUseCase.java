package ke.wateratm.application.port.in;

import java.util.UUID;

import ke.wateratm.domain.model.Payment;

/** Sends the M-Pesa PIN prompt (STK push) to the customer's phone for an order. */
public interface RequestPaymentUseCase {

	Payment requestPayment(UUID orderId);

}
