package nikeno.Tenki;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YahooWeather {
	private static final String SERVER_ENCODING ="UTF-8";
	public String areaName = "";
	public Day today;
	public Day tomorrow;
	public WeeklyDay [] days;

	public static YahooWeather parse(byte [] htmlData) throws YahooWeatherParseException, UnsupportedEncodingException  {
		String html = new String(htmlData, SERVER_ENCODING);
		YahooWeather yw = new YahooWeather();

		html = html.replace("\n", "");

		// 地域名を取得
		Matcher m = Pattern.compile("<title.*?>(.*?)(（〒.*?）)?の天気.*?</title>").matcher(html);
		if (!m.find()) throw new YahooWeatherParseException(4);
		yw.areaName = m.group(1).replace(" ", "");

		// 今日と明日の天気を処理
		Pattern p = Pattern.compile("<!---Point--->(.*?)<!---/Point--->",
				Pattern.CASE_INSENSITIVE);
		m = p.matcher(html);
		if (!m.find()) throw new YahooWeatherParseException(1);
		yw.today = parseDay(m.group(1));

		if (!m.find()) throw new YahooWeatherParseException(2);
		yw.tomorrow = parseDay(m.group(1));

		// 週間天気を処理
		m = Pattern.compile("\"yjw_table\"(.*?)</table>").matcher(html);
		if (!m.find()) throw new YahooWeatherParseException(3);
		yw.days = parseWeek(m.group(1));

		return yw;
	}

	// 「今日」と「明日」の部分を処理
	private static Day parseDay(String html) throws YahooWeatherParseException {
		Day result = new Day();
		Pattern pRow = Pattern.compile("<tr.*?>(.*?)</tr>", Pattern.CASE_INSENSITIVE);
		Pattern pColumn = Pattern.compile("<td.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE);
		Pattern pUrl = Pattern.compile("(http://[a-zA-Z0-9./_]*)", Pattern.CASE_INSENSITIVE);

		Pattern pDate = Pattern.compile("yjSt.*?([\\d]+)月[ ]*?([\\d]+)日");
		Matcher dm = pDate.matcher(html);
		if (!dm.find()) {
			throw new YahooWeatherParseException(25);
		}
		else {
			int month = Integer.parseInt(dm.group(1), 10);
			int day = Integer.parseInt(dm.group(2), 10);
			result.date = convertDate(month, day);
		}

		Matcher rm = pRow.matcher(html);
		for (int r=0; r<6; r++) {
			if (!rm.find()) throw new YahooWeatherParseException(10);

			Matcher cm = pColumn.matcher(rm.group(1));
			for (int c=0; c<9; c++) {
				if (!cm.find()) throw new YahooWeatherParseException(11);
				if (c == 0) continue;
				Hour hour = result.hours[c-1];

				String text = removeTag(cm.group(1));
				switch (r) {
				case 0:	// 時間
					text = text.replace("時", "").trim();
					hour.hour = Integer.parseInt(text);
					break;
				case 1:	// 天気
					hour.text = text;
					Matcher mUrl = pUrl.matcher(cm.group(1));
					if (!mUrl.find()) {
						hour.setImageUrl(null);
//						throw new YahooWeatherParseException(12);
					}
					else {
						hour.setImageUrl(mUrl.group(1));
					}
					break;
				case 2:	// 気温
					hour.temp = text;
					break;
				case 3:	// 湿度
					hour.humidity = text;
					break;
				case 4:	// 降水量
					hour.rain = text;
					break;
				case 5:	// 風速
					hour.wind = text;
					break;
				}
			}
		}
		return result;
	}

	private static Date convertDate(int month, int date) {
		Calendar today = Calendar.getInstance(Locale.JAPAN);
		int year = today.get(Calendar.YEAR);

		switch (today.get(Calendar.MONTH)) {
		case Calendar.JANUARY:
			if (month == 12) year--;
			break;
		case Calendar.DECEMBER:
			if (month == 1) year++;
			break;
		}
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
		c.set(year , month - 1, date, 0, 0, 0);

		return c.getTime();
	}

	private static WeeklyDay [] parseWeek(String html) throws YahooWeatherParseException {
		WeeklyDay [] result = new WeeklyDay [6];
		for (int j=0; j<result.length; j++) {
			result[j] = new WeeklyDay();
		}
		Pattern pRow = Pattern.compile("<tr.*?>(.*?)</tr>", Pattern.CASE_INSENSITIVE);
		Pattern pColumn = Pattern.compile("<td.*?>(.*?)(<br>.*?)?</td>", Pattern.CASE_INSENSITIVE);
		Pattern pUrl = Pattern.compile("(http://[a-zA-Z0-9./_]*)", Pattern.CASE_INSENSITIVE);


		Matcher rm = pRow.matcher(html);
		for (int r=0; r<4; r++) {
			if (!rm.find()) {
				throw new YahooWeatherParseException(20);
			}

			Matcher cm = pColumn.matcher(rm.group(1));
			for (int c=0; c<7; c++) {
				if (!cm.find()) {
					throw new YahooWeatherParseException(21);
				}
				if (c == 0) continue;
				WeeklyDay day = result[c-1];

				String text = removeTag(cm.group(1));
				switch (r) {
				case 0:	// 日付
					day.date = text.replaceAll("[0-9]+月", "");
					if (cm.groupCount() == 2) day.date += "\n" + removeTag(cm.group(2));
					break;
				case 1:	// 天気
					if (cm.groupCount() != 2) {
						throw new YahooWeatherParseException(22);
					}
					day.text = removeTag(cm.group(2));
					Matcher mUrl = pUrl.matcher(cm.group(1));
					if (mUrl.find()) {
						day.imageUrl = mUrl.group(1);
						//throw new YahooWeatherParseException(24);
					}
					else {
						// 週間天気予報では7日後の予報が公開されていないことがある
						day.imageUrl = null;
					}
					break;
				case 2:	// 気温
					if (cm.groupCount() != 2) {
						throw new YahooWeatherParseException(23);
					}
					day.tempMax = text;
					day.tempMin = removeTag(cm.group(2));
					break;
				case 3:	// 降水確率
					day.rain = text;
					break;
				}
			}
		}
		return result;
	}

	private static String removeTag(String html) {
		Pattern p = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE);
		return p.matcher(html).replaceAll("");
	}

	public static class YahooWeatherParseException extends Exception {
		private static final long serialVersionUID = 1L;
		public int errorCode;
		YahooWeatherParseException(int errorCode) {
			this.errorCode = errorCode;
		}
		@Override
		public String getMessage() {
			return String.format("HTMLの処理エラー:%d", errorCode);
		}
	}

	public static class WeeklyDay {
		public String date;
		public String text;
		public String imageUrl;
		public String tempMax;
		public String tempMin;
		public String rain;
	}

	public static class Day {
		public Date date;
		public Hour [] hours = new Hour [8];
		public Day() {
			for (int j=0; j<hours.length; j++) {
				hours[j] = new Hour();
			}
		}
	}

	public static class Hour {
		public int hour;
		public String text;
		public String temp;
		public String humidity;
		public String rain;
		public String wind;
		private String imageUrl;

		public void setImageUrl(String url) {
			if (url != null) {
				imageUrl = url.replace("_g.gif", ".gif");
			} else {
				imageUrl = null;
			}
		}
		public String getImageUrl(boolean enabled) {
			if (imageUrl == null) {
				return null;
			}
			else if (enabled) {
				return imageUrl;
			}
			else {
				return imageUrl.replace(".gif", "_g.gif");
			}
		}

		@Override
		public String toString() {
			return String.format("%d時 %s,%s,%s,%s,%s,%s",
					hour, text, temp, humidity, rain, wind, imageUrl);
		}
	}

//	private static final String HAN_ZEN = "０１２３４５６７８９";
//
//	public static String NumHanToZen(String src) {
//
//		int src_length = src.length();
//		String str = "";
//		int num;
//		for (int j=0; j<src_length; j++) {
//			num = src.charAt(j);
//			if (num >= '0' && num <= '9') {
//				str += HAN_ZEN.charAt(num - '0');
//			} else {
//				str += num;
//			}
//		}
//		return str;
//	}
}
