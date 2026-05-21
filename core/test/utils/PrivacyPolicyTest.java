package utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PrivacyPolicyTest {
	@Test
	void policyIncludesPlayStoreRequiredBasics(){
		assertTrue(PrivacyPolicy.DISPLAY_TEXT.contains("Privacy Policy"));
		assertTrue(PrivacyPolicy.DISPLAY_TEXT.contains("does not collect"));
		assertTrue(PrivacyPolicy.DISPLAY_TEXT.contains(PrivacyPolicy.DEVELOPER_NAME));
		assertTrue(PrivacyPolicy.DISPLAY_TEXT.contains(PrivacyPolicy.CONTACT_EMAIL));
		assertTrue(PrivacyPolicy.DISPLAY_TEXT.contains("Local progress"));
	}
}
