package de.pseudonymisierung.mainzelliste.configuration.claim.oidc;


import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.authorizationServer.OIDCServer;
import de.pseudonymisierung.mainzelliste.configuration.claim.Claim;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset.Subset;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset.SubsetEnum;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.subset.SubsetFactory;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator.Operator;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator.OperatorEnum;
import de.pseudonymisierung.mainzelliste.configuration.claim.oidc.operator.OperatorFactory;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationParser;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

public class OIDCConfigurationParser {
  private static Logger logger = Logger.getLogger(OIDCConfigurationParser.class);
  private static final String OPERATOR ="operator";
  private static final String SUBSET = "subset";
  private static OperatorEnum defaulltOperatorEnum = OperatorEnum.AND;
  private static SubsetEnum defaultSubsetEnum = SubsetEnum.ANY;
  private static String subsetRegex = "subset|";
  private static final String EXCLUDEOIDC = "(?!.operator)";
  private static final String ISSPREFIX ="iss";

  public static OIDCServer getOIDCServer(Map<String, String> configurationProperties, String prefix){
    String oidcServerKey = ConfigurationUtils.getConcatedConfigurationPath(prefix, ISSPREFIX);
    if(configurationProperties.containsKey(oidcServerKey)){
      String oidcServerName = configurationProperties.get(oidcServerKey);
      Set<OIDCServer> oidcServerSet = Config.instance.getOidcServerSet();
      return  oidcServerSet.stream().filter(el -> el.getId().equals(oidcServerName)).findFirst().orElse(null);
    }
    logger.warn("OIDC-Server name not found "+prefix);
    return null;
  }


  private static OperatorEnum getOperatorEnum(Map<String, String> configurationProperties, String prefix){
    String operatorKey = ConfigurationUtils.getConcatedConfigurationPath(prefix,OPERATOR);
    if(configurationProperties.containsKey(operatorKey)){
      String operatorValue = configurationProperties.get(operatorKey);
      return OperatorEnum.valueOf(operatorValue);
    }
    else{
      return defaulltOperatorEnum;
    }
  }

  private static SubsetEnum getSubsetEnum(Map<String, String> configurationProperties, String prefix) {
    String subsetKey = ConfigurationUtils.getConcatedConfigurationPath(prefix,SUBSET);
    if (configurationProperties.containsKey(subsetKey)) {
      String subsetValue = configurationProperties.get(subsetKey);
      return SubsetEnum.valueOf(subsetValue);
    } else {
      return defaultSubsetEnum;
    }
  }

  private static Claim parseOIDCClaim(Map<String, String> configurationProperties, String prefix, String claim){
    String claimPrefix = ConfigurationUtils.getConcatedConfigurationPath(prefix, claim);
    SubsetEnum subsetEnum = getSubsetEnum(configurationProperties, claimPrefix);
    Subset subset = new SubsetFactory().createSubset(subsetEnum);
    String claimValue = configurationProperties.get(claimPrefix);
    Set<String> splittedClaimValues = ConfigurationUtils.splitDefaultConfigurationValue(claimValue);
    return  new Claim(claim, subset, splittedClaimValues);
  }



  public static OIDCProperty parseConfiguration(Map<String, String> configurationProperties, String prefix){

    OperatorEnum operatorEnum = getOperatorEnum(configurationProperties, prefix);
    Operator operator = new OperatorFactory().createOperator(operatorEnum);

    Map<String, String> filteredConfgigurationProperties = configurationProperties;

    Set<String> claims = ConfigurationParser.getDynamicKeys(filteredConfgigurationProperties, "("+prefix+")"+EXCLUDEOIDC, subsetRegex);
    claims.remove(ISSPREFIX);
    List<Claim> claimList = new ArrayList<>();
    for(String claim: claims){
      Claim oidcClaim =  parseOIDCClaim(configurationProperties, prefix, claim);
      if(oidcClaim != null) claimList.add(oidcClaim);
    }
    OIDCServer oidcServer = getOIDCServer(configurationProperties, prefix);
    if(oidcServer == null){
      return null;
    }
    OIDCProperty oidcProperty = new OIDCProperty(operator, claimList);
    return oidcProperty;

  }

}
