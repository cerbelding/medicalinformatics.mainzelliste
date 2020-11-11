package de.pseudonymisierung.mainzelliste.auth;

public class AuthorizationServer {

  protected String issuer;
  protected String metaDataUrl;
  protected String id;


  public AuthorizationServer(String issuer, String id) {
    this.issuer = issuer;
    this.metaDataUrl = ".well-known/oauth-authorization-server";
    this.id = id;
  }

  public AuthorizationServer(String issuer, String id, String metaDataUrl) {
    this.issuer = issuer;
    this.metaDataUrl = metaDataUrl;
    this.id = id;
  }

  public String getIssuer() {
    return this.issuer;
  }

  public String getMetaDataUrl() {
    return this.issuer + this.metaDataUrl;
  }

  public String getId(){return this.id;}


}
