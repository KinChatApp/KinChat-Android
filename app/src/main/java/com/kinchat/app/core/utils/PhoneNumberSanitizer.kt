package com.kinchat.app.core.utils

import javax.inject.Inject

class PhoneNumberSanitizer @Inject constructor() {
    
    fun sanitize(phone: String): String {
        var cleaned = phone.replace(Regex("[^\\d+]"), "")
        
        if (cleaned.startsWith("01") && cleaned.length == 11) {
            cleaned = "+88$cleaned"
        } else if (cleaned.startsWith("8801") && cleaned.length == 13) {
            cleaned = "+$cleaned"
        }
        
        return cleaned
    }
}
