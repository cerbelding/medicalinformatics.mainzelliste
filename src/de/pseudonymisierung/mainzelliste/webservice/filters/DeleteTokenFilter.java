package de.pseudonymisierung.mainzelliste.webservice.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.webservice.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeleteTokenFilter implements ContainerResponseFilter {

    private Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        // Tokens are only used in patient resource
        if(request.getAbsolutePath().toString().contains("/patients")){
            if(100 <= response.getStatus() && response.getStatus() < 400){
                String tokenId = getTokenId(request);
                logger.info("Checking if token with id {} can be be deleted ...", tokenId);
                Token token = Servers.instance.getTokenByTid(tokenId);
                if(token != null){
                    int remainingUses = token.decreaseRemainingUses();
                    if(remainingUses > 0){
                        logger.info("Token with id {} should not be deleted. Remaining uses are: {}", token.getId(), remainingUses);
                    } else {
                        logger.info("Deleting Token with id {} from Mainzelliste", token.getId());
                        Servers.instance.deleteToken(token.getId());
                    }
                }
            }
        }
        return response;
    }

    private String getTokenId(ContainerRequest request) {
        String requestPath = request.getAbsolutePath().toString();
        switch (request.getMethod().toUpperCase()){
            case "GET":
            case "PUT":
                if(requestPath.contains("/tokenId/")) {
                    return requestPath.split("/tokenId/")[1];
                } else {
                    return request.getQueryParameters().getFirst("tokenId");
                }
            case "POST":
                if(requestPath.contains("checkMatch"))
                    return requestPath.split("checkMatch/")[1];
                else
                    return request.getQueryParameters().getFirst("tokenId");
            case "DELETE":
                String[] splitedRequestPath = requestPath.split("/");
                return splitedRequestPath[splitedRequestPath.length - 3];
            default:
                return "";
        }
    }
}
