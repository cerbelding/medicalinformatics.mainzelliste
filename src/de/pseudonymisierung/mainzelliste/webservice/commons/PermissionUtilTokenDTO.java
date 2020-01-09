package de.pseudonymisierung.mainzelliste.webservice.commons;

public class PermissionUtilTokenDTO {

    public PermissionUtilTokenDTO(String requestedParameter, String requestedValue){
        this.requestedParameter = requestedParameter;
        this.requestedValue = requestedValue;
    }

    public String getRequestedParameter() {
        return requestedParameter;
    }

    public void setRequestedParameter(String requestedParameter) {
        this.requestedParameter = requestedParameter;
    }

    private String requestedParameter;

    public String getRequestedValue() {
        return requestedValue;
    }

    public void setRequestedValue(String requestedValue) {
        this.requestedValue = requestedValue;
    }

    private String requestedValue;
}
