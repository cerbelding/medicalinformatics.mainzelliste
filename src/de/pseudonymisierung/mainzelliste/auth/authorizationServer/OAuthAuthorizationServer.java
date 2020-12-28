package de.pseudonymisierung.mainzelliste.auth.authorizationServer;

import de.pseudonymisierung.mainzelliste.auth.credentials.AuthorizationServerCredentials;

/**
 * Represents the OAuth-Server
 */
public class OAuthAuthorizationServer implements AuthorizationServer {

  protected String issuer;
  protected String metaDataUrl;
  protected String name;


  public OAuthAuthorizationServer(String issuer, String name) {
    this.issuer = issuer;
    this.metaDataUrl = ".well-known/oauth-authorization-server";
    this.name = name;
  }

  public OAuthAuthorizationServer(String issuer, String name, String metaDataUrl) {
    this.issuer = issuer;
    this.metaDataUrl = metaDataUrl;
    this.name = name;
  }

  public String getId(){return this.issuer; }

  public String getName(){return this.name;}

  @Override
  public boolean authorize(AuthorizationServerCredentials oidcCredentials) {
    return this.issuer.equals(oidcCredentials.getServerId());
  }

}
