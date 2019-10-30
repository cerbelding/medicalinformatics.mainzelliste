package de.pseudonymisierung.mainzelliste.webservice.commons;

import com.sun.jersey.api.uri.UriTemplate;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



public class RedirectBuilder {


    private Logger logger = Logger.getLogger(RedirectBuilder.class);

    private Map<String, String> tokenId;
    private Map<String, String> mappedIdTypesdAndIds;
    private UriTemplate uriTemplate;
    private URI redirectURI;

    private Map<String, String> allMappedValues;


    public RedirectBuilder setTokenId(String tokenId){
        Map<String, String> tempTokenId = new HashMap<>();
        tempTokenId.put("tokenId", tokenId);
        this.tokenId = tempTokenId;
        return this;
    }


    public RedirectBuilder setMappedIdTypesdAndIds(List<String> IdTypes, IDRequest idRequest){
        Map<String, String> mappedIdTypesAndIds = new HashMap<>();
        for (String idType :IdTypes){
            mappedIdTypesAndIds.put(idType, idRequest.getAssignedPatient().getId(idType).getIdString());
        }
        this.mappedIdTypesdAndIds = mappedIdTypesAndIds;
        return this;
    }

    public RedirectBuilder setMappedIdTypesdAndIds(List<String> IdTypes, Patient patient){
        Map<String, String> mappedIdTypesAndIds = new HashMap<>();
        for (String idType :IdTypes){
            mappedIdTypesAndIds.put(idType, patient.getId(idType).getIdString());
        }
        this.mappedIdTypesdAndIds = mappedIdTypesAndIds;
        return this;
    }

    //put in build() to avoid "uriTemplate is not set" problem?
    public RedirectBuilder setTemplateURI(UriTemplate uriTemplate){
        logger.info("SetTemplateURI: " + uriTemplate);
        this.uriTemplate = uriTemplate;
        return this;
    }

    public Redirect build(){
        mapAllMaps();
        if(uriTemplate==null){
            throw new InternalErrorException("uriTemplate is not set");
        }
        try {
            redirectURI = new URI(uriTemplate.createURI(allMappedValues));
        } catch (URISyntaxException e) {
            throw new InternalErrorException("The passed redirect URL " + this.uriTemplate + "is invalid!");
        }

        return new Redirect(this.tokenId, this.mappedIdTypesdAndIds, this.uriTemplate, this.redirectURI, this.allMappedValues);
    }

    private void mapAllMaps(){
        allMappedValues = new HashMap<>();

        if(this.tokenId!=null){
            allMappedValues.putAll(this.tokenId);
        }
        if(this.mappedIdTypesdAndIds!=null){
            allMappedValues.putAll(this.mappedIdTypesdAndIds);
        }
    }


}
