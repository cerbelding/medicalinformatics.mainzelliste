package de.pseudonymisierung.mainzelliste.webservice.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import de.pseudonymisierung.mainzelliste.Servers;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DeleteTokenFilter implements ContainerResponseFilter {

    Logger logger = Logger.getLogger(this.getClass());

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // TODO: Check if it is allways tokenId
        String tokenId = httpRequest.getParameter("tokenId");
        if(100 <= response.getStatus() && response.getStatus() <=400){
            logger.info("Deleting Token " + tokenId + " from Mainzelliste");
            Servers.instance.deleteToken(tokenId);
        }
        // TODO: Check if other statusCodes need also deletion
        return response;
    }
}
