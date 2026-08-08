package com.kinchat.app.features.auth.utils

object PhoneFormatter {
    private const val EXPECTED_LENGTH_WITH_ZERO = 11
    private const val EXPECTED_LENGTH_WITHOUT_ZERO = 10
    private const val OTP_LENGTH = 6

    fun cleanPhoneNumber(phone: String): String {
        return phone.replace("\\s+".toRegex(), "")
    }

    fun isReadyForSubmission(cleanPhone: String): Boolean {
        val expectedLength = if (cleanPhone.startsWith("0")) EXPECTED_LENGTH_WITH_ZERO else EXPECTED_LENGTH_WITHOUT_ZERO
        return cleanPhone.length == expectedLength
    }

    fun getFullFormattedPhone(countryCode: String, phoneNumber: String): String {
        val phoneWithoutLeadingZero = if (phoneNumber.startsWith("0")) {
            phoneNumber.substring(1)
        } else {
            phoneNumber
        }
        return "$countryCode$phoneWithoutLeadingZero"
    }

    fun cleanOtp(otp: String): String {
        return otp.replace("\\D".toRegex(), "").take(OTP_LENGTH)
    }
    
    fun isOtpValid(cleanOtp: String): Boolean {
        return cleanOtp.length == OTP_LENGTH
    }
}
