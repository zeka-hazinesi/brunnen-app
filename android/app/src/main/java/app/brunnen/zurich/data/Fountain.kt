package app.brunnen.zurich.data

data class Fountain(
    val id: Int,
    val name: String,
    val location: String?,
    val quartier: String?,
    val baujahr: Int?,
    val brunnenNummer: String?,
    val wasserart: String?,
    val isTrinkwasser: Boolean,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val isAbgestellt: Boolean,
)

data class CheckIn(
    val id: String = "",
    val userId: String = "",
    val fountainId: Int = 0,
    val fountainName: String = "",
    val timestamp: Long = 0L,
    val points: Int = 0,
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val nickname: String = "",
    val avatarUrl: String? = null,
    val points: Long = 0L,
    val checkInCount: Long = 0L,
    val friends: List<String> = emptyList(),
)
