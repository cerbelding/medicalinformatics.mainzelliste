package de.pseudonymisierung.mainzelliste.httpsClient;

public enum HttpHeaderEnum {
  APIKEY("mainzellisteApiKey"),
  APIKEY_DEPRECATED("mzidApiKey"),
  AUTHORIZATION("Authorization");

  private String httpHeader;

  HttpHeaderEnum(String httpHeaderString) {
    this.httpHeader = httpHeaderString;
  }

  public String getHttpHeaderKey() {
    return this.httpHeader;
  }


}
