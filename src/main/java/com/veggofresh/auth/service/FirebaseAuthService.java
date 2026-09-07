package com.veggofresh.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.veggofresh.platform.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * Server-side verification of Firebase Phone Auth ID tokens.
 *
 * <p>With client-side Firebase Phone Auth, the mobile app sends the SMS OTP and signs
 * the user in, producing a Firebase ID token. The backend has no role in sending the
 * SMS; it only verifies the ID token the client presents, using the Firebase Admin SDK
 * already present in this project.
 */
@Service
public class FirebaseAuthService {

    /**
     * Verifies the given Firebase ID token and returns the verified claims.
     *
     * @param idToken the Firebase ID token produced by the client after phone sign-in
     * @return verified token claims (including phone number)
     */
    public FirebaseToken verifyIdToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new BusinessException("AUTH_FIREBASE_TOKEN_INVALID", "Invalid Firebase ID token");
        }
    }

    /**
     * Verifies the Firebase ID token and returns the verified phone number.
     *
     * @param idToken the Firebase ID token produced by the client after phone sign-in
     * @return the verified phone number (with country code) from the token
     */
    public String getVerifiedPhone(String idToken) {
        FirebaseToken token = verifyIdToken(idToken);
        String phone = (String) token.getClaims().get("phone_number");
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("AUTH_FIREBASE_NO_PHONE", "Firebase token does not contain a phone number");
        }
        return phone;
    }
}
