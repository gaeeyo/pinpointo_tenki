package nikeno.Tenki.feature.weather

import nikeno.Tenki.Area

class YahooSearchHtmlParser {

    fun parse(body: ByteArray): ArrayList<Area> {
        val html = body.toString(Charsets.UTF_8)
        val result = ArrayList<Area>()

        // ざっくりとした
        val body = Regex("<thead(.*?)<tfoot", RegexOption.IGNORE_CASE)
            .find(html.replace("\n", ""))?.groupValues?.get(1)

        if (body != null) {
            // <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
            Regex(
                "<tr.*?href=\"(.*?)\".*?>(.*?)</a>",
                RegexOption.IGNORE_CASE
            ).findAll(body).forEach {
                val url = if (it.groupValues[1].startsWith("//")) {
                    "https:" + it.groupValues[1]
                } else {
                    it.groupValues[1]
                }
                val d = Area(
                    "", it.groupValues[2], "", url
                )

                result.add(d)
                //Log.d(TAG, m.group(1) +"," +m.group(2) + "," + m.group(4) + "," + m.group(3) );
            }
        }
        return result
    }
}