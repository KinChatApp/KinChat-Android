package com.kinchat.app.features.auth.utils

import androidx.compose.runtime.Immutable

@Immutable
data class Country(
    val iso: String, 
    val code: String, 
    val flag: String, 
    val name: String
)

val COUNTRIES: List<Country> = listOf(
    // South Asia
    Country("BD", "+880", "🇧🇩", "Bangladesh"),
    Country("IN", "+91", "🇮🇳", "India"),
    Country("PK", "+92", "🇵🇰", "Pakistan"),
    Country("NP", "+977", "🇳🇵", "Nepal"),
    Country("LK", "+94", "🇱🇰", "Sri Lanka"),
    Country("BT", "+975", "🇧🇹", "Bhutan"),
    Country("MV", "+960", "🇲🇻", "Maldives"),
    Country("MM", "+95", "🇲🇲", "Myanmar"),

    // Middle East
    Country("AE", "+971", "🇦🇪", "UAE"),
    Country("SA", "+966", "🇸🇦", "Saudi Arabia"),
    Country("QA", "+974", "🇶🇦", "Qatar"),
    Country("KW", "+965", "🇰🇼", "Kuwait"),
    Country("OM", "+968", "🇴🇲", "Oman"),
    Country("BH", "+973", "🇧🇭", "Bahrain"),
    Country("JO", "+962", "🇯🇴", "Jordan"),
    Country("IQ", "+964", "🇮🇶", "Iraq"),
    Country("LB", "+961", "🇱🇧", "Lebanon"),

    // East & Southeast Asia
    Country("MY", "+60", "🇲🇾", "Malaysia"),
    Country("SG", "+65", "🇸🇬", "Singapore"),
    Country("ID", "+62", "🇮🇩", "Indonesia"),
    Country("PH", "+63", "🇵🇭", "Philippines"),
    Country("TH", "+66", "🇹🇭", "Thailand"),
    Country("VN", "+84", "🇻🇳", "Vietnam"),
    Country("CN", "+86", "🇨🇳", "China"),
    Country("JP", "+81", "🇯🇵", "Japan"),
    Country("KR", "+82", "🇰🇷", "South Korea"),
    Country("HK", "+852", "🇭🇰", "Hong Kong"),
    Country("TW", "+886", "🇹🇼", "Taiwan"),

    // North America & Oceania
    Country("US", "+1", "🇺🇸", "United States"),
    Country("CA", "+1", "🇨🇦", "Canada"),
    Country("GB", "+44", "🇬🇧", "United Kingdom"),
    Country("AU", "+61", "🇦🇺", "Australia"),
    Country("NZ", "+64", "🇳🇿", "New Zealand"),

    // Europe
    Country("DE", "+49", "🇩🇪", "Germany"),
    Country("FR", "+33", "🇫🇷", "France"),
    Country("IT", "+39", "🇮🇹", "Italy"),
    Country("ES", "+34", "🇪🇸", "Spain"),
    Country("NL", "+31", "🇳🇱", "Netherlands"),
    Country("BE", "+32", "🇧🇪", "Belgium"),
    Country("PT", "+351", "🇵🇹", "Portugal"),
    Country("CH", "+41", "🇨🇭", "Switzerland"),
    Country("SE", "+46", "🇸🇪", "Sweden"),
    Country("NO", "+47", "🇳🇴", "Norway"),
    Country("IE", "+353", "🇮🇪", "Ireland"),

    // Africa
    Country("EG", "+20", "🇪🇬", "Egypt"),
    Country("MA", "+212", "🇲🇦", "Morocco"),
    Country("DZ", "+213", "🇩🇿", "Algeria"),
    Country("LY", "+218", "🇱🇾", "Libya"),
    Country("NG", "+234", "🇳🇬", "Nigeria"),
    Country("ZA", "+27", "🇿🇦", "South Africa"),

    // Others
    Country("BR", "+55", "🇧🇷", "Brazil"),
    Country("TR", "+90", "🇹🇷", "Turkey"),
    Country("RU", "+7", "🇷🇺", "Russia")
)
