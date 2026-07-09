package com.ardhana.util;

public final class PhoneNumbers {

    private PhoneNumbers() {}

    /**
     * Strips everything but digits, so "+91 98765 43210", "91-98765-43210",
     * and "919876543210" all normalize to the same value. Used to make sure
     * the mobile number stored on signup matches the one an OTP was
     * verified for, regardless of how the user formatted it in the form.
     */
    public static String normalize(String mobile) {
        return mobile == null ? null : mobile.replaceAll("[^0-9]", "");
    }
}
