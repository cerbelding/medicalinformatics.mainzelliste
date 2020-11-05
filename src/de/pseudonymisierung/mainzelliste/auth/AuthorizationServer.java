package de.pseudonymisierung.mainzelliste.auth;

public class AuthorizationServer {
  protected String issuer;
  protected String metaDataUrl;


  public AuthorizationServer(String issuer){
    this.issuer=issuer;
    this.metaDataUrl = ".well-known/oauth-authorization-server";
  }
  public AuthorizationServer(String issuer, String metaDataUrl){
    this.issuer=issuer;
    this.metaDataUrl=metaDataUrl;
  }

  protected String getIssuer() {
    return issuer;
  }

  protected String getMetaDataUrl() {
    return issuer+metaDataUrl;
  }

}
