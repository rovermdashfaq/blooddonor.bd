package com.example.data.model

data class DistrictCoord(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class BloodBankEntry(
    val name: String,
    val district: String,
    val phone: String,
    val address: String,
    val hours: String
)

data class BloodCompatibility(
    val group: String,
    val donateTo: List<String>,
    val receiveFrom: List<String>,
    val note: String
)

data class MythBuster(
    val myth: String,
    val fact: String
)

data class EligibilityQuestion(
    val question: String,
    val requiredYes: Boolean,
    val guidance: String
)

object BangladeshData {
    const val SUPPORT_EMAIL = "blooddonorbd025@gmail.com"
    const val EMERGENCY_HELPLINE = "+8801612018221"

    val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")

    val DISTRICTS: List<DistrictCoord> = listOf(
        DistrictCoord("Dhaka", 23.8103, 90.4125),
        DistrictCoord("Chattogram", 22.3569, 91.7832),
        DistrictCoord("Sylhet", 24.8949, 91.8687),
        DistrictCoord("Rajshahi", 24.3745, 88.6042),
        DistrictCoord("Khulna", 22.8456, 89.5403),
        DistrictCoord("Barishal", 22.7010, 90.3535),
        DistrictCoord("Rangpur", 25.7439, 89.2752),
        DistrictCoord("Mymensingh", 24.7471, 90.4203),
        DistrictCoord("Gazipur", 23.9999, 90.4203),
        DistrictCoord("Narayanganj", 23.6238, 90.5000),
        DistrictCoord("Cumilla", 23.4607, 91.1809),
        DistrictCoord("Bogura", 24.8465, 89.3776),
        DistrictCoord("Cox's Bazar", 21.4272, 92.0058),
        DistrictCoord("Jashore", 23.1634, 89.2182),
        DistrictCoord("Dinajpur", 25.6279, 88.6332),
        DistrictCoord("Tangail", 24.2513, 89.9167),
        DistrictCoord("Faridpur", 23.6071, 89.8429),
        DistrictCoord("Pabna", 24.0064, 89.2372),
        DistrictCoord("Noakhali", 22.8696, 91.0995),
        DistrictCoord("Feni", 23.0159, 91.3976),
        DistrictCoord("Brahmanbaria", 23.9608, 91.1115),
        DistrictCoord("Narsingdi", 23.9322, 90.7154),
        DistrictCoord("Manikganj", 23.8617, 90.0003),
        DistrictCoord("Munshiganj", 23.5422, 90.5305),
        DistrictCoord("Kishoreganj", 24.4449, 90.7766),
        DistrictCoord("Netrokona", 24.8709, 90.7279),
        DistrictCoord("Jamalpur", 24.9375, 89.9378),
        DistrictCoord("Sherpur", 25.0205, 90.0153),
        DistrictCoord("Gopalganj", 23.0051, 89.8266),
        DistrictCoord("Madaripur", 23.1641, 90.1897),
        DistrictCoord("Shariatpur", 23.2423, 90.4348),
        DistrictCoord("Rajbari", 23.7574, 89.6445),
        DistrictCoord("Sirajganj", 24.4534, 89.7007),
        DistrictCoord("Natore", 24.4206, 89.0003),
        DistrictCoord("Naogaon", 24.7936, 88.9318),
        DistrictCoord("Chapainawabganj", 24.5965, 88.2775),
        DistrictCoord("Joypurhat", 25.0968, 89.0227),
        DistrictCoord("Kushtia", 23.9013, 89.1205),
        DistrictCoord("Jhenaidah", 23.5450, 89.1726),
        DistrictCoord("Magura", 23.4873, 89.4199),
        DistrictCoord("Narail", 23.1725, 89.5127),
        DistrictCoord("Satkhira", 22.7185, 89.0705),
        DistrictCoord("Bagerhat", 22.6516, 89.7859),
        DistrictCoord("Chuadanga", 23.6402, 88.8418),
        DistrictCoord("Meherpur", 23.7622, 88.6318),
        DistrictCoord("Bhola", 22.6859, 90.6482),
        DistrictCoord("Patuakhali", 22.3596, 90.3299),
        DistrictCoord("Pirojpur", 22.5841, 89.9720),
        DistrictCoord("Jhalokati", 22.6406, 90.1987),
        DistrictCoord("Barguna", 22.1592, 90.1256),
        DistrictCoord("Habiganj", 24.3749, 91.4155),
        DistrictCoord("Moulvibazar", 24.4829, 91.7774),
        DistrictCoord("Sunamganj", 25.0658, 91.3950),
        DistrictCoord("Chandpur", 23.2333, 90.6713),
        DistrictCoord("Lakshmipur", 22.9425, 90.8412),
        DistrictCoord("Rangamati", 22.7324, 92.2985),
        DistrictCoord("Bandarban", 22.1953, 92.2184),
        DistrictCoord("Khagrachhari", 23.1193, 91.9847),
        DistrictCoord("Gaibandha", 25.3288, 89.5281),
        DistrictCoord("Kurigram", 25.8054, 89.6362),
        DistrictCoord("Lalmonirhat", 25.9923, 89.2847),
        DistrictCoord("Nilphamari", 25.9318, 88.8560),
        DistrictCoord("Panchagarh", 26.3411, 88.5542),
        DistrictCoord("Thakurgaon", 26.0337, 88.4617)
    )

    fun districtNames(): List<String> = DISTRICTS.map { it.name }

    fun coordForDistrict(district: String?): DistrictCoord? =
        DISTRICTS.firstOrNull { it.name.equals(district?.trim(), ignoreCase = true) }

    val BLOOD_BANKS: List<BloodBankEntry> = listOf(
        BloodBankEntry("Dhaka Medical College Blood Bank", "Dhaka", "+880 2-55165088", "Secretariat Rd, Dhaka 1000", "24/7"),
        BloodBankEntry("Bangladesh Red Crescent Blood Center", "Dhaka", "+880 2-9116563", "7/5 Aurangzeb Rd, Mohammadpur, Dhaka", "24/7"),
        BloodBankEntry("Quantum Blood Bank Lab", "Dhaka", "+880 2-9351969", "31/V Shilpacharya Zainul Abedin Sarak, Shantinagar", "24/7"),
        BloodBankEntry("Sandhani Central Blood Bank", "Dhaka", "+880 2-9668690", "DMCH Campus, Dhaka 1000", "24/7"),
        BloodBankEntry("Badhan Blood Bank", "Dhaka", "+880 1534-982674", "TSC, University of Dhaka", "8 AM - 10 PM"),
        BloodBankEntry("BSMMU Blood Bank", "Dhaka", "+880 2-9661051", "Shahbag, Dhaka 1000", "24/7"),
        BloodBankEntry("Chattogram Medical College Blood Bank", "Chattogram", "+880 31-630335", "K.B. Fazlul Kader Rd, Chattogram", "24/7"),
        BloodBankEntry("Red Crescent Chattogram", "Chattogram", "+880 31-620686", "395 Andarkilla, Chattogram", "24/7"),
        BloodBankEntry("Sylhet MAG Osmani Medical Blood Bank", "Sylhet", "+880 821-713667", "Medical Rd, Sylhet 3100", "24/7"),
        BloodBankEntry("Rajshahi Medical College Blood Bank", "Rajshahi", "+880 721-776001", "Laxmipur, Rajshahi 6000", "24/7"),
        BloodBankEntry("Khulna Medical College Blood Bank", "Khulna", "+880 41-760350", "Boyra, Khulna 9000", "24/7"),
        BloodBankEntry("Sher-e-Bangla Medical Blood Bank", "Barishal", "+880 431-2173547", "Band Rd, Barishal 8200", "24/7"),
        BloodBankEntry("Rangpur Medical College Blood Bank", "Rangpur", "+880 521-63388", "Medical Pakhir Mor, Rangpur 5400", "24/7"),
        BloodBankEntry("Mymensingh Medical College Blood Bank", "Mymensingh", "+880 91-66063", "Charpara, Mymensingh 2200", "24/7"),
        BloodBankEntry("Cumilla Medical College Blood Bank", "Cumilla", "+880 81-65562", "Kuchaitoli, Cumilla 3500", "24/7"),
        BloodBankEntry("Bogura SZMC Blood Bank", "Bogura", "+880 51-69995", "Silimpur, Bogura 5800", "24/7"),
        BloodBankEntry("Cox's Bazar Sadar Hospital Blood Bank", "Cox's Bazar", "+880 341-63235", "Hospital Rd, Cox's Bazar", "24/7"),
        BloodBankEntry("Jashore 250 Bed Hospital Blood Bank", "Jashore", "+880 421-68661", "Hospital Rd, Jashore", "24/7")
    )

    val COMPATIBILITY_CHART: List<BloodCompatibility> = listOf(
        BloodCompatibility("O-", BLOOD_GROUPS, listOf("O-"), "Universal Red Cell Donor"),
        BloodCompatibility("O+", listOf("O+", "A+", "B+", "AB+"), listOf("O+", "O-"), "Most common donor group in Bangladesh"),
        BloodCompatibility("A-", listOf("A+", "A-", "AB+", "AB-"), listOf("A-", "O-"), "High demand for platelet and red cells"),
        BloodCompatibility("A+", listOf("A+", "AB+"), listOf("A+", "A-", "O+", "O-"), "Second most common blood type"),
        BloodCompatibility("B-", listOf("B+", "B-", "AB+", "AB-"), listOf("B-", "O-"), "Rare negative group"),
        BloodCompatibility("B+", listOf("B+", "AB+"), listOf("B+", "B-", "O+", "O-"), "Very common in South Asia"),
        BloodCompatibility("AB-", listOf("AB+", "AB-"), listOf("AB-", "A-", "B-", "O-"), "Universal Plasma Donor (Negative)"),
        BloodCompatibility("AB+", listOf("AB+"), BLOOD_GROUPS, "Universal Recipient")
    )

    val MYTH_BUSTERS: List<MythBuster> = listOf(
        MythBuster(
            myth = "Donating blood makes you permanently weak.",
            fact = "Your body replaces lost plasma within 24–48 hours and red blood cells within 4–6 weeks. Normal healthy donors recover rapidly with hydration."
        ),
        MythBuster(
            myth = "You can catch infections from donating blood.",
            fact = "Registered blood centers in Bangladesh always use a brand-new, sterile, single-use disposable needle for every single donor."
        ),
        MythBuster(
            myth = "People who weigh under 60 kg cannot donate.",
            fact = "Healthy adults weighing 45 kg (100 lbs) or more and aged 18–60 with good hemoglobin levels can safely donate 350–450 ml."
        ),
        MythBuster(
            myth = "One donation only helps one patient.",
            fact = "A single 450 ml unit is separated into red cells, plasma, and platelets—saving up to 3 lives from one donation."
        )
    )

    val ELIGIBILITY_QUIZ: List<EligibilityQuestion> = listOf(
        EligibilityQuestion(
            question = "Are you between 18 and 60 years old?",
            requiredYes = true,
            guidance = "Donors in Bangladesh must be at least 18 years old."
        ),
        EligibilityQuestion(
            question = "Is your body weight at least 45 kg?",
            requiredYes = true,
            guidance = "Minimum weight of 45 kg ensures safe blood volume."
        ),
        EligibilityQuestion(
            question = "Has it been at least 90 days (3 months) since your last whole blood donation?",
            requiredYes = true,
            guidance = "A 90-day recovery window protects your iron & hemoglobin reserves."
        ),
        EligibilityQuestion(
            question = "Do you currently feel healthy (no fever, active infection, or antibiotics)?",
            requiredYes = true,
            guidance = "Please wait until full recovery before donating blood."
        ),
        EligibilityQuestion(
            question = "Have you avoided major surgery, tattooing, or unscreened transfusions in the last 6 months?",
            requiredYes = true,
            guidance = "A 6-month deferral period applies after surgery or tattoos."
        )
    )

    val HEALTH_TIPS_BY_GROUP: Map<String, String> = mapOf(
        "A+" to "Focus on iron-rich leafy greens (palong shak, lal shak), lentils, and Vitamin C to keep hemoglobin above 12.5 g/dL.",
        "A-" to "Because A- is rare, keeping an updated availability status in Dhaka/Chattogram helps emergency maternity and surgery patients immediately.",
        "B+" to "Stay hydrated with 3+ liters of water and oral saline before and after every 90-day donation cycle.",
        "B-" to "B- is rare in Bangladesh; share your verified Donor ID card with close family so emergency requests find you faster.",
        "O+" to "O+ can help every Rh+ patient. Eat dates, eggs, and spinach regularly to maintain healthy ferritin stores.",
        "O-" to "As a Universal Red Cell Donor, your donation is critical in trauma emergencies—keep at least 90 days between whole blood donations.",
        "AB+" to "As a Universal Recipient, you can also donate plasma and platelets frequently when cleared by a medical officer.",
        "AB-" to "AB- plasma helps newborns and trauma patients across all blood groups. Maintain balanced hydration and rest."
    )

    val DID_YOU_KNOW_FACTS: List<String> = listOf(
        "Over 800,000+ units of blood are needed annually across hospitals in Bangladesh.",
        "Platelets must be transfused within just 5 days of collection, making regular voluntary donors indispensable.",
        "Donating blood stimulates bone marrow to produce fresh new red blood cells.",
        "Male and female donors both only need 90 days in our BD verification schedule to refresh eligibility after admin-approved donations."
    )
}
