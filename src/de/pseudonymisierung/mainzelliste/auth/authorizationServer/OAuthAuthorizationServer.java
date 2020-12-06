package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

/**
 * Represents the OAuth-Server
 */
public class OAuthAuthorizationServer {

  protected String issuer;
  protected String metaDataUrl;
  protected String id;


  public OAuthAuthorizationServer(String issuer, String id) {
    this.issuer = issuer;
    this.metaDataUrl = ".well-known/oauth-authorization-server";
    this.id = id;
  }

  public OAuthAuthorizationServer(String issuer, String id, String metaDataUrl) {
    this.issuer = issuer;
    this.metaDataUrl = metaDataUrl;
    this.id = id;
  }

  public String getIssuer() {
    return this.issuer;
  }

  public String getId() {
    return this.id;
  }

}
