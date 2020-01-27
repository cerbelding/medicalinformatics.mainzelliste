package de.pseudonymisierung.mainzelliste.webservice.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import de.pseudonymisierung.mainzelliste.Servers;
import org.apache.log4j.Logger;

public class DeleteTokenFilter implements ContainerResponseFilter {

    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        logger.info("DeleteTokenFilter");
        // Tokens are only used in patient resource
        if(request.getAbsolutePath().toString().contains("/patients")){
            if(100 <= response.getStatus() && response.getStatus() < 400){
                String tokenId = getTokenId(request);
                logger.info("Deleting Token " + tokenId + " from Mainzelliste");
                Servers.instance.deleteToken(tokenId);
            }
        }
        return response;
    }

    private String getTokenId(ContainerRequest request) {
        String requestPath = request.getAbsolutePath().toString();
        switch (request.getMethod().toUpperCase()){
            case "GET":
            case "PUT":
                return requestPath.split("/tokenId/")[1];
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
