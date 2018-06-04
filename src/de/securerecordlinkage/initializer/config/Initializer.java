
package de.securerecordlinkage.initializer.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="mainzellisteLocal" type="{http://schema.mitro.dkfz-heidelberg.de/config/initializer}server"/&gt;
 *         &lt;element name="srlLocal" type="{http://schema.mitro.dkfz-heidelberg.de/config/initializer}server"/&gt;
 *         &lt;element name="servers"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="server" type="{http://schema.mitro.dkfz-heidelberg.de/config/initializer}externalServer" maxOccurs="unbounded"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "mainzellisteLocal",
    "srlLocal",
    "servers"
})
@XmlRootElement(name = "initializer")
public class Initializer {

    @XmlElement(required = true)
    protected Server mainzellisteLocal;
    @XmlElement(required = true)
    protected Server srlLocal;
    @XmlElement(required = true)
    protected Initializer.Servers servers;

    /**
     * Gets the value of the mainzellisteLocal property.
     * 
     * @return
     *     possible object is
     *     {@link Server }
     *     
     */
    public Server getMainzellisteLocal() {
        return mainzellisteLocal;
    }

    /**
     * Sets the value of the mainzellisteLocal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Server }
     *     
     */
    public void setMainzellisteLocal(Server value) {
        this.mainzellisteLocal = value;
    }

    /**
     * Gets the value of the srlLocal property.
     * 
     * @return
     *     possible object is
     *     {@link Server }
     *     
     */
    public Server getSrlLocal() {
        return srlLocal;
    }

    /**
     * Sets the value of the srlLocal property.
     * 
     * @param value
     *     allowed object is
     *     {@link Server }
     *     
     */
    public void setSrlLocal(Server value) {
        this.srlLocal = value;
    }

    /**
     * Gets the value of the servers property.
     * 
     * @return
     *     possible object is
     *     {@link Initializer.Servers }
     *     
     */
    public Initializer.Servers getServers() {
        return servers;
    }

    /**
     * Sets the value of the servers property.
     * 
     * @param value
     *     allowed object is
     *     {@link Initializer.Servers }
     *     
     */
    public void setServers(Initializer.Servers value) {
        this.servers = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="server" type="{http://schema.mitro.dkfz-heidelberg.de/config/initializer}externalServer" maxOccurs="unbounded"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "server"
    })
    public static class Servers {

        @XmlElement(required = true)
        protected List<ExternalServer> server;

        /**
         * Gets the value of the server property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the server property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getServer().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ExternalServer }
         * 
         * 
         */
        public List<ExternalServer> getServer() {
            if (server == null) {
                server = new ArrayList<ExternalServer>();
            }
            return this.server;
        }

    }

}
