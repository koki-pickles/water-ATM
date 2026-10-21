package ke.wateratm.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ke.wateratm.application.port.in.FindOrderUseCase;
import ke.wateratm.application.port.in.PlaceOrderUseCase;
import ke.wateratm.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import ke.wateratm.application.port.in.RequestPaymentUseCase;
import ke.wateratm.domain.model.Order;
import ke.wateratm.domain.model.OrderChannel;
import ke.wateratm.domain.model.Payment;

/**
 * REST API for the mobile app.
 *
 * ⚠️ No login yet (comes with the app in Phase 3b). Do NOT deploy this publicly before then:
 * anyone could send M-Pesa prompts to any number.
 */
@RestController
@RequestMapping("/api/orders")
class OrderController {

	private final PlaceOrderUseCase placeOrder;

	private final FindOrderUseCase findOrder;

	private final RequestPaymentUseCase requestPayment;

	OrderController(PlaceOrderUseCase placeOrder, FindOrderUseCase findOrder, RequestPaymentUseCase requestPayment) {
		this.placeOrder = placeOrder;
		this.findOrder = findOrder;
		this.requestPayment = requestPayment;
	}

	@PostMapping
	ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
		Order order = placeOrder.placeOrder(new PlaceOrderCommand(request.phone(), request.jerrycans(), request.lat(),
				request.lng(), OrderChannel.APP));
		return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
	}

	@GetMapping("/{id}")
	ResponseEntity<OrderResponse> get(@PathVariable UUID id) {
		return ResponseEntity.of(findOrder.findOrder(id).map(OrderResponse::from));
	}

	/** Sends the M-Pesa PIN prompt to the customer's phone. */
	@PostMapping("/{id}/payment")
	ResponseEntity<PaymentResponse> pay(@PathVariable UUID id) {
		Payment payment = requestPayment.requestPayment(id);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(PaymentResponse.from(payment));
	}

	record PlaceOrderRequest(@NotBlank String phone, @Min(1) @Max(Order.MAX_JERRYCANS) int jerrycans,
			@NotNull Double lat, @NotNull Double lng) {
	}

	record OrderResponse(UUID id, String status, int jerrycans, int totalKes, String deliveryCode, Instant createdAt) {

		static OrderResponse from(Order order) {
			return new OrderResponse(order.id(), order.status().name(), order.jerrycans(), order.totalKes(),
					order.deliveryCode(), order.createdAt());
		}

	}

	record PaymentResponse(UUID paymentId, String status, int amountKes, String message) {

		static PaymentResponse from(Payment payment) {
			return new PaymentResponse(payment.id(), payment.status().name(), payment.amountKes(),
					"Check your phone and enter your M-Pesa PIN");
		}

	}

}
