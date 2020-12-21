package de.pseudonymisierung.mainzelliste.webservice.commons;

import com.sun.jersey.api.uri.UriTemplate;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RedirectBuilder {

    private Logger logger = LogManager.getLogger(RedirectBuilder.class);

    private Map<String, String> tokenId;
    private Map<String, String> mappedIdTypesdAndIds;
    private Map<String, String> similarityScores;
    private UriTemplate uriTemplate;
    private URI redirectURI;

    private Map<String, String> allMappedValues;

    public RedirectBuilder setTokenId(String tokenId) {
        Map<String, String> tempTokenId = new HashMap<>();
        tempTokenId.put("tokenId", tokenId);
        this.tokenId = tempTokenId;
        return this;
    }

    public RedirectBuilder setSimilarityScores(List<Double> similarityScores) {
        Map<String, String> mappedScores = new HashMap<>();
        String similarityScoresString = similarityScores.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        mappedScores.put("similarityScores", similarityScoresString);
        this.similarityScores = mappedScores;
        return this;
    }

    public RedirectBuilder setMappedIdTypesdAndIds(List<String> idTypes, IDRequest idRequest) {
        return setMappedIdTypesdAndIds(idTypes, idRequest.getAssignedPatient());
    }

    public RedirectBuilder setMappedIdTypesdAndIds(List<String> idTypes, Patient patient) {
        Map<String, String> mappedIdTypesAndIds = new HashMap<>();
        for (String idType : idTypes) {
            mappedIdTypesAndIds.put(idType, patient.getId(idType).getIdString());
        }
        this.mappedIdTypesdAndIds = mappedIdTypesAndIds;
        return this;
    }

    //put in build() to avoid "uriTemplate is not set" problem?
    public RedirectBuilder setTemplateURI(UriTemplate uriTemplate) {
        logger.info("SetTemplateURI: {}", uriTemplate);
        this.uriTemplate = uriTemplate;
        return this;
    }

    public Redirect build() {
        mapAllMaps();
        if (uriTemplate == null) {
            throw new InternalErrorException("uriTemplate is not set");
        }
        try {
            redirectURI = new URI(uriTemplate.createURI(allMappedValues));
        } catch (URISyntaxException e) {
            throw new InternalErrorException("The passed redirect URL " + this.uriTemplate + "is invalid!");
        }

        return new Redirect(this.tokenId, this.mappedIdTypesdAndIds, this.similarityScores, this.uriTemplate, this.redirectURI, this.allMappedValues);
    }

    private void mapAllMaps() {
        allMappedValues = new HashMap<>();

        if (this.tokenId != null) {
            allMappedValues.putAll(this.tokenId);
        }
        if (this.mappedIdTypesdAndIds != null) {
            allMappedValues.putAll(this.mappedIdTypesdAndIds);
        }
        if (this.similarityScores != null) {
            allMappedValues.putAll(this.similarityScores);
        }
    }

}
