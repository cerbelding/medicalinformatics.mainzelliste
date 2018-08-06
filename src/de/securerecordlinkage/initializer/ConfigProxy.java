package de.securerecordlinkage.initializer;

import de.samply.common.config.Configuration;
import de.samply.common.config.ObjectFactory;
import de.samply.common.http.HttpConnector;
import de.samply.config.util.FileFinderUtil;
import de.samply.config.util.JAXBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;

public enum ConfigProxy {

    /**
     * The singleton instance
     */
    instance;

    //region Properties
    /**
     * The Constant logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ConfigProxy.class);
    private final HashMap<String, String> proxyConfig = new HashMap<>();

    // samply config
    private Configuration proxyConfiguration;

    public HashMap<String, String> getProxyConfig() {
        return proxyConfig;
    }

    public Configuration getProxyConfiguration() {
        return proxyConfiguration;
    }
    //endregion Properties

    ConfigProxy() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the config
     *
     * @throws IOException
     * @throws JAXBException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public void init() throws IOException, JAXBException, ParserConfigurationException, SAXException, URISyntaxException {

        String fallback = new File(ConfigProxy.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getAbsolutePath();

        // proxy
        File proxyFile = FileFinderUtil.findFile("initializer.proxy.xml", "samply", fallback);

        proxyConfiguration = JAXBUtil.unmarshall(proxyFile, JAXBContext.newInstance(ObjectFactory.class),
                                                 Configuration.class);
        prepareProxy(proxyConfiguration);
    }

    /**
     * prepares proxy configuration as it is not null-safe, and common-http yet
     * does not accept common-config TODO: make common-http know common-config
     */
    private void prepareProxy(Configuration proxyConfiguration) {
        if (proxyConfiguration != null && proxyConfiguration.getProxy() != null) {
            if (proxyConfiguration.getProxy().getHTTP() != null
                && proxyConfiguration.getProxy().getHTTP().getUrl() != null) {
                proxyConfig
                        .put(HttpConnector.PROXY_HTTP_HOST, proxyConfiguration.getProxy().getHTTP().getUrl().getHost());
                proxyConfig.put(HttpConnector.PROXY_HTTP_PORT,
                                String.valueOf(proxyConfiguration.getProxy().getHTTP().getUrl().getPort()));
            }

            if (proxyConfiguration.getProxy().getHTTPS() != null
                && proxyConfiguration.getProxy().getHTTPS().getUrl() != null) {
                proxyConfig.put(HttpConnector.PROXY_HTTPS_HOST,
                                proxyConfiguration.getProxy().getHTTPS().getUrl().getHost());
                proxyConfig.put(HttpConnector.PROXY_HTTPS_PORT,
                                String.valueOf(proxyConfiguration.getProxy().getHTTPS().getUrl().getPort()));
            }
        }
    }

}
