package app.filestash.platform.session;

import app.filestash.platform.support.SupportController;

public interface SessionCodeService {

	public String generateCode(String key);
	
	public boolean verifyCode(String key, String code);
}
