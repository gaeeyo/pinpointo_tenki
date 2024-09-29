package nikeno.Tenki

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Area(
    val zipCode: String, val address1: String, val address2: String, val url: String
) : Parcelable {

    fun serialize(): String {
        return """
             $zipCode
             $address1
             $address2
             $url
             """.trimIndent()
    }

    companion object {
        fun deserialize(text: String): Area? {
            var data: Area? = null
            val values = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (values.size >= 4) {
                data = Area(
                    values[0], values[1], values[2], values[3]
                )
            }
            return data
        }
    }
}
