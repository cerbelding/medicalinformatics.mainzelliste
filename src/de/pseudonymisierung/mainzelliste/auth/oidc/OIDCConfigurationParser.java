package de.pseudonymisierung.mainzelliste.auth.oidc;


import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.auth.ClaimProperty;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.OIDCClaim;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset.Subset;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset.SubsetEnum;
import de.pseudonymisierung.mainzelliste.auth.oidc.claim.subset.SubsetFactory;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.Operator;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.OperatorEnum;
import de.pseudonymisierung.mainzelliste.auth.oidc.operator.OperatorFactory;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationParser;
import de.pseudonymisierung.mainzelliste.configuration.ConfigurationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

  private static OIDCServer getOIDCServer(Map<String, String> configurationProperties, String prefix){
    if(configurationProperties.containsKey(prefix)){
      String oidcServerName = configurationProperties.get(prefix);
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

  private static OIDCClaim parseOIDCClaim(Map<String, String> configurationProperties, String prefix, String claim){
    String claimPrefix = ConfigurationUtils.getConcatedConfigurationPath(prefix, claim);
    SubsetEnum subsetEnum = getSubsetEnum(configurationProperties, claimPrefix);
    Subset subset = new SubsetFactory().createSubset(subsetEnum);
    String claimValue = configurationProperties.get(claimPrefix);
    Set<String> splittedClaimValues = ConfigurationUtils.splitDefaultConfigurationValue(claimValue);
    return  new OIDCClaim(claim, subset, splittedClaimValues);
  }



  public static OIDCProperty parseConfiguration(Map<String, String> configurationProperties, String prefix){

    OperatorEnum operatorEnum = getOperatorEnum(configurationProperties, prefix);
    Operator operator = new OperatorFactory().createOperator(operatorEnum);

    Set<String> claims = ConfigurationParser.getDynamicKeys(configurationProperties, "("+prefix+")"+EXCLUDEOIDC, subsetRegex);
    List<OIDCClaim> oidcClaimList = new ArrayList<>();
    for(String claim: claims){
      OIDCClaim oidcClaim =  parseOIDCClaim(configurationProperties, prefix, claim);
      if(oidcClaim != null) oidcClaimList.add(oidcClaim);
    }

    OIDCServer oidcServer = getOIDCServer(configurationProperties, prefix);
    if(oidcServer == null){
      return null;
    }
    OIDCProperty oidcProperty = new OIDCProperty(oidcServer,operator, oidcClaimList);
    return oidcProperty;

  }

}
