package nikeno.Tenki;

public class Area {
    public String zipCode;
    public String address1;
    public String address2;
    public String url;

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Area) {
            return url.equals(((Area) obj).url);
        }
        return false;
    }

    public static Area deserialize(String text) {
        Area     data   = null;
        String[] values = text.split("\n");
        if (values.length >= 4) {
            data = new Area();
            data.zipCode = values[0];
            data.address1 = values[1];
            data.address2 = values[2];
            data.url = values[3];
        }
        return data;
    }

    public String serialize() {
        return zipCode + "\n" +
                address1 + "\n" +
                address2 + "\n" +
                url;
    }
}
