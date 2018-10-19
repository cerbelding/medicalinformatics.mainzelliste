
package de.securerecordlinkage.initializer;

public class Server {
    protected String id;
    protected String apiKey;
    //TODO: implement different authentication types
    protected String idType;
    protected String url;

    Server() {

    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the apiKey property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the value of the apiKey property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setApiKey(String value) {
        this.apiKey = value;
    }

    /**
     * Gets the value of the idType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getIdType() {
        return idType;
    }

    /**
     * Sets the value of the idType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setIdType(String value) {
        this.idType = value;
    }

    /**
     * Gets the value of the url property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the value of the url property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUrl(String value) {
        this.url = value;
    }

}
