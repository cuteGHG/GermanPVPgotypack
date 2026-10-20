package de.cuteghg.fre.country;

public record CountryInfo(String countryCode, String countryName, String unicodeFlag, boolean known) {
    public static CountryInfo unknown(String code, String name, String flag) {
        return new CountryInfo(code, name, flag, false);
    }
}
