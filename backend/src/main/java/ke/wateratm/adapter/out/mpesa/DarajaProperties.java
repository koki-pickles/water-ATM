package ke.wateratm.adapter.out.mpesa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings from {@code wateratm.mpesa.*} in application.properties (secrets come from environment variables).
 *
 * @param transactionType {@code CustomerPayBillOnline} for a Paybill, {@code CustomerBuyGoodsOnline} for a Till
 * @param callbackBaseUrl public URL M-Pesa can reach us on (a tunnel like ngrok during development)
 * @param callbackSecret random string in the callback URL, so strangers can't fake "payment succeeded"
 */
@ConfigurationProperties("wateratm.mpesa")
public record DarajaProperties(String baseUrl, String consumerKey, String consumerSecret, String shortcode,
		String passkey, String transactionType, String callbackBaseUrl, String callbackSecret) {

	public String callbackUrl() {
		return callbackBaseUrl + "/api/mpesa/callback/" + callbackSecret;
	}

}
