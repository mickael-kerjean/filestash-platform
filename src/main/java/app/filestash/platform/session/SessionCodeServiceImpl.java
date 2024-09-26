package app.filestash.platform.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import app.filestash.platform.payment.PaymentServiceImpl;
import com.stripe.Stripe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class SessionCodeServiceImpl implements SessionCodeService {

	private static final Logger logger = LoggerFactory.getLogger(SessionCodeServiceImpl.class);

	@Value("${session.secret_key}")
	public String SALT;
	
	public static String invalid_code = "__invalid__";

	@PostConstruct
	public void StripeServiceSetup() {
		if (SALT.isEmpty()) {
			logger.warn("secret key is empty");
		}
	}

	@Override
	public String generateCode(String key) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (Exception err) {
			return invalid_code;
		}
		byte[] hash = digest.digest(String.format("%s::%s", SALT, key).getBytes(StandardCharsets.UTF_8));
		final StringBuilder hashStr = new StringBuilder(hash.length);
		for (byte hashByte : hash)
	    	hashStr.append(Integer.toHexString(255 & hashByte));
	    return hashStr.toString();
	}

	@Override
	public boolean verifyCode(String key, String code) {
		if (code.equals(invalid_code)) {
			return false;
		}
		return this.generateCode(key).equals(code);
	}
}
