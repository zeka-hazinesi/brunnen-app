package app.brunnen.zurich.data

import android.content.Context
import android.location.Location
import org.json.JSONObject

object FountainRepository {

    private var cachedFountains: List<Fountain>? = null

    fun loadFountains(context: Context): List<Fountain> {
        cachedFountains?.let { return it }

        val json = context.assets.open("brunnen.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val features = root.getJSONArray("features")
        val fountains = mutableListOf<Fountain>()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            val props = feature.getJSONObject("properties")

            val wasserart = props.optString("wasserart", "")
            val isTrinkwasser = wasserart.isNotBlank() &&
                !wasserart.equals("kein Trinkwasser", ignoreCase = true)

            fountains.add(
                Fountain(
                    id = props.optInt("objectid"),
                    name = props.optString("standort", "Unbekannter Brunnen"),
                    location = props.optString("ortsbezeichnung", null),
                    quartier = props.optString("quartier", null),
                    baujahr = if (props.isNull("historisches_baujahr")) {
                        if (props.isNull("baujahr")) null else props.optInt("baujahr")
                    } else {
                        props.optInt("historisches_baujahr")
                    },
                    brunnenNummer = props.optString("brunnennummer", null),
                    wasserart = wasserart.ifBlank { null },
                    isTrinkwasser = isTrinkwasser,
                    latitude = coords.getDouble(1),
                    longitude = coords.getDouble(0),
                    photoUrl = props.optString("foto", null)?.ifBlank { null },
                    isAbgestellt = props.optString("abgestellt", "nein")
                        .equals("ja", ignoreCase = true),
                ),
            )
        }

        cachedFountains = fountains
        return fountains
    }

    fun sortedByDistance(
        fountains: List<Fountain>,
        userLat: Double,
        userLng: Double,
    ): List<Pair<Fountain, Float>> {
        return fountains.map { fountain ->
            val distance = FloatArray(1)
            Location.distanceBetween(userLat, userLng, fountain.latitude, fountain.longitude, distance)
            fountain to distance[0]
        }.sortedBy { it.second }
    }
}
