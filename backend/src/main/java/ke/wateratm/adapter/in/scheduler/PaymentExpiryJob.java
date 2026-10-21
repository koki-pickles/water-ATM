package ke.wateratm.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import ke.wateratm.application.port.in.ExpireStalePaymentsUseCase;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class PaymentExpiryJob {

	private static final Logger LOG = LoggerFactory.getLogger(PaymentExpiryJob.class);

	private final ExpireStalePaymentsUseCase expireStale;

	PaymentExpiryJob(ExpireStalePaymentsUseCase expireStale) {
		this.expireStale = expireStale;
	}

	@Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1M")
	void run() {
		int closed = expireStale.expireStale();
		if (closed > 0) {
			LOG.info("Closed {} stale payments/orders", closed);
		}
	}

}
