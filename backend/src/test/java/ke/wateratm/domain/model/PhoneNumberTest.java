package ke.wateratm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

	@ParameterizedTest
	@ValueSource(strings = { "0712345678", "0712 345 678", "+254712345678", "254712345678", "712345678",
			"(+254) 712-345-678" })
	void normalizesCommonFormats(String raw) {
		assertThat(PhoneNumber.of(raw).value()).isEqualTo("254712345678");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0110345678", "254110345678" })
	void accepts01Numbers(String raw) {
		assertThat(PhoneNumber.of(raw).value()).isEqualTo("254110345678");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "07123", "0812345678", "+33612345678", "07abc45678", "07123456789" })
	void rejectsInvalidNumbers(String raw) {
		assertThatThrownBy(() -> PhoneNumber.of(raw)).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "0712345678", "+254 712 345 678" })
	void sameNumberIsEqual(String raw) {
		assertThat(PhoneNumber.of(raw)).isEqualTo(PhoneNumber.of("254712345678"));
	}

}
