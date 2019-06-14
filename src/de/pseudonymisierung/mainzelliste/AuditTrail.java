/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.pseudonymisierung.mainzelliste;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 *
 * @author lemmer
 */
@Entity
@Table(name="audit_trail")
public class AuditTrail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private Long auditTrailJpaId;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date timestamp;
    private String idValue;
    private String idType;
    private String username;
    private String remoteSystem;
    private String remoteIp;
    private String typeOfChange;
    private String reasonForChange;
    private String oldValue;
    private String newValue;

    public AuditTrail(Date timestmp, String idValue, String idType, String username, String remoteSystem, String remoteIp, String typeOfChange, String reasonForChange, String oldValue, String newValue) {
        this.timestamp = timestmp;
        this.idValue = idValue;
        this.idType = idType;
        this.username = username;
        this.remoteSystem = remoteSystem;
        this.remoteIp = remoteIp;
        this.typeOfChange = typeOfChange;
        this.reasonForChange = reasonForChange;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Long getAuditTrailJpaId() {
        return auditTrailJpaId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getIdValue() {
        return idValue;
    }

    public void setIdValue(String idValue) {
        this.idValue = idValue;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRemoteSystem() {
        return remoteSystem;
    }

    public void setRemoteSystem(String remoteSystem) {
        this.remoteSystem = remoteSystem;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public String getTypeOfChange() {
        return typeOfChange;
    }

    public void setTypeOfChange(String typeOfChange) {
        this.typeOfChange = typeOfChange;
    }

    public String getReasonForChange() {
        return reasonForChange;
    }

    public void setReasonForChange(String reasonForChange) {
        this.reasonForChange = reasonForChange;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (auditTrailJpaId != null ? auditTrailJpaId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof AuditTrail)) {
            return false;
        }
        AuditTrail other = (AuditTrail) object;
        if ((this.auditTrailJpaId == null && other.auditTrailJpaId != null) || (this.auditTrailJpaId != null && !this.auditTrailJpaId.equals(other.auditTrailJpaId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.pseudonymisierung.mainzelliste.AuditTrail[ id=" + auditTrailJpaId + " ]";
    }

}