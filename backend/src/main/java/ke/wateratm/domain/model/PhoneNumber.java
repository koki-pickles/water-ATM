package ke.wateratm.domain.model;

/**
 * A Kenyan mobile number, always stored as 2547XXXXXXXX / 2541XXXXXXXX (the format M-Pesa and
 * WhatsApp expect). Accepts the shapes people actually type: 0712 345 678, +254712345678, 712345678...
 */
public record PhoneNumber(String value) {

	public PhoneNumber {
		value = normalize(value);
	}

	public static PhoneNumber of(String raw) {
		return new PhoneNumber(raw);
	}

	private static String normalize(String raw) {
		if (raw == null) {
			throw new IllegalArgumentException("Phone number is missing");
		}
		String digits = raw.replaceAll("[\\s\\-()+]", "");
		if (!digits.matches("\\d+")) {
			throw new IllegalArgumentException("Phone number has invalid characters: " + raw);
		}
		if (digits.startsWith("0")) {
			digits = "254" + digits.substring(1);
		}
		else if (digits.length() == 9) {
			digits = "254" + digits;
		}
		if (!digits.matches("254[17]\\d{8}")) {
			throw new IllegalArgumentException("Not a Kenyan mobile number: " + raw);
		}
		return digits;
	}

	@Override
	public String toString() {
		return value;
	}

}
