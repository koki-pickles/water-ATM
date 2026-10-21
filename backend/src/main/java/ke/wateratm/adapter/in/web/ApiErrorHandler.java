package ke.wateratm.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ke.wateratm.application.port.out.PaymentGatewayPort.PaymentGatewayException;
import ke.wateratm.domain.exception.IllegalOrderTransitionException;
import ke.wateratm.domain.exception.NoShopNearbyException;
import ke.wateratm.domain.exception.OrderNotFoundException;
import ke.wateratm.domain.exception.PaymentNotAllowedException;

/** Turns domain errors into clear HTTP answers for the app. */
@RestControllerAdvice
class ApiErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ApiErrorHandler.class);

	@ExceptionHandler(OrderNotFoundException.class)
	ProblemDetail notFound(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail badRequest(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(NoShopNearbyException.class)
	ProblemDetail noShop(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
	}

	@ExceptionHandler({ IllegalOrderTransitionException.class, PaymentNotAllowedException.class })
	ProblemDetail conflict(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(PaymentGatewayException.class)
	ProblemDetail gateway(RuntimeException ex) {
		LOG.warn("M-Pesa request failed: {}", ex.getMessage());
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "M-Pesa is not answering, try again shortly");
	}

}
