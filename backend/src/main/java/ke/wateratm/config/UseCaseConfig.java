package ke.wateratm.config;

import java.time.Clock;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import ke.wateratm.application.port.in.ExpireStalePaymentsUseCase;
import ke.wateratm.application.port.in.FindOrderUseCase;
import ke.wateratm.application.port.in.HandlePaymentResultUseCase;
import ke.wateratm.application.port.in.PlaceOrderUseCase;
import ke.wateratm.application.port.in.RequestPaymentUseCase;
import ke.wateratm.application.port.in.UpdateOrderStatusUseCase;
import ke.wateratm.application.port.out.CustomerRepositoryPort;
import ke.wateratm.application.port.out.OrderRepositoryPort;
import ke.wateratm.application.port.out.PartnerShopRepositoryPort;
import ke.wateratm.application.port.out.PaymentGatewayPort;
import ke.wateratm.application.port.out.PaymentRepositoryPort;
import ke.wateratm.application.service.OrderService;
import ke.wateratm.application.service.PaymentService;

/**
 * Wires the hexagon together: plugs the adapters into the application's ports and wraps every
 * use case in a database transaction. The services themselves are not beans, so nobody can
 * call them without a transaction by accident.
 */
@Configuration(proxyBeanMethods = false)
class UseCaseConfig {

	private final OrderService orderService;

	private final PaymentService paymentService;

	UseCaseConfig(OrderRepositoryPort orders, CustomerRepositoryPort customers, PartnerShopRepositoryPort shops,
			PaymentRepositoryPort payments, PaymentGatewayPort gateway, Clock clock,
			@Value("${wateratm.delivery-fee-kes}") int deliveryFeeKes) {
		this.orderService = new OrderService(orders, customers, shops, deliveryFeeKes);
		this.paymentService = new PaymentService(orders, customers, payments, gateway, clock);
	}

	@Bean
	PlaceOrderUseCase placeOrderUseCase(PlatformTransactionManager tx) {
		return transactional(orderService, PlaceOrderUseCase.class, tx);
	}

	@Bean
	UpdateOrderStatusUseCase updateOrderStatusUseCase(PlatformTransactionManager tx) {
		return transactional(orderService, UpdateOrderStatusUseCase.class, tx);
	}

	@Bean
	FindOrderUseCase findOrderUseCase(PlatformTransactionManager tx) {
		return transactional(orderService, FindOrderUseCase.class, tx);
	}

	@Bean
	RequestPaymentUseCase requestPaymentUseCase(PlatformTransactionManager tx) {
		return transactional(paymentService, RequestPaymentUseCase.class, tx);
	}

	@Bean
	HandlePaymentResultUseCase handlePaymentResultUseCase(PlatformTransactionManager tx) {
		return transactional(paymentService, HandlePaymentResultUseCase.class, tx);
	}

	@Bean
	ExpireStalePaymentsUseCase expireStalePaymentsUseCase(PlatformTransactionManager tx) {
		return transactional(paymentService, ExpireStalePaymentsUseCase.class, tx);
	}

	/** Every method called through the port runs in one transaction: all of it is saved, or none. */
	private static <T> T transactional(T target, Class<T> port, PlatformTransactionManager tx) {
		ProxyFactory proxy = new ProxyFactory();
		proxy.setTarget(target);
		proxy.setInterfaces(port);
		proxy.addAdvice(new TransactionInterceptor(tx, new MatchAlwaysTransactionAttributeSource()));
		return port.cast(proxy.getProxy());
	}

}
