package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpCheckTest {

    @Test
    fun `real bank OTPs give OTP`() {
        listOf(
            "Your one-time password is 482913. It expires in 5 minutes.",
            "Your OTP is 4821. Do not share this code with anyone.",
            "123456 is your verification code. Never share it.",
            "Use token 90817263 to complete your transfer. Don't give it to anybody.",
            "OTP: 5512. Valid for 10 mins. Bank staff will never ask for it. Do not disclose.",
        ).forEach { assertTrue(it, OtpCheck.isRealOtp(it)) }
    }

    @Test
    fun `strangers asking for the code are not real OTPs`() {
        listOf(
            "Please send me the OTP 4821 that just came to your phone",
            "Your OTP is 4821, kindly forward it to our agent on 08012345678",
            "Tell me the verification code 123456 so I can fix your account",
            "Share the one-time code 5512 with customer care to unblock",
            "Give me the token 9081 quickly",
        ).forEach { assertFalse(it, OtpCheck.isRealOtp(it)) }
    }

    @Test
    fun `OTP messages with links are not real OTPs`() {
        assertFalse(OtpCheck.isRealOtp("Your OTP is 4821. Confirm at https://bit.ly/x"))
        assertFalse(OtpCheck.isRealOtp("Your OTP is 4821. Confirm at www.fake-bank.com"))
    }

    @Test
    fun `needs both a code and OTP words`() {
        assertFalse(OtpCheck.isRealOtp("Your OTP will arrive shortly"))
        assertFalse(OtpCheck.isRealOtp("Your order 482913 has shipped"))
    }
}
