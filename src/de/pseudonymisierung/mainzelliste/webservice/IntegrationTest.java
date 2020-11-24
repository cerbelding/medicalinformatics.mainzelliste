package de.pseudonymisierung.mainzelliste.webservice;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import de.pseudonymisierung.mainzelliste.ExternalID;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDGenerator;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.ListID;
import de.pseudonymisierung.mainzelliste.ListIDGenerator;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.dto.Persistor;

@Path("/test")
public class IntegrationTest {

    // I am unsure where to put this method
    /**
     * Retrieves the ListIDGenerator for an idType String if and only if this idType is configured to be contained in a ListID
     * @param subIdType
     * @return The ListIDGenerator for subIdType, null if none is configured
     */
    public ListIDGenerator getListIDGeneratorBySubIdType(String subIdType) {
        for(String idType: IDGeneratorFactory.instance.getIDTypes()) {
            IDGenerator<?> unknownIdGen = IDGeneratorFactory.instance.getFactory(idType);

            if(unknownIdGen instanceof ListIDGenerator) {
                ListIDGenerator listGen = (ListIDGenerator) unknownIdGen;
                if(listGen.holdsIdType(subIdType)) {
                    return listGen;
                }
            }
        }

        return null;
    }

    @GET
    @Path("/visit_1")
    public Response testVisitIdCreation(@Context HttpServletRequest req) {
        StringBuilder output = new StringBuilder();
        output.append("<p>We will perform hard-coded tests in order to check the functionality of ListID and ExternalID.</p>");

        // our test input
        String[] visit_id_1 = { "fallnummer", "1234asdf" };
        String[] visit_id_2 = { "fallnummer", "4321fdsa" };

        // test patient to attatch the ids to
        ExternalID pat_id = (ExternalID) IDGeneratorFactory.instance.buildId("patientennummer", "123456789");
        Patient patient = Persistor.instance.getPatient(pat_id);
        if(patient == null) {
            // I am to lazy to populate a real Patient. Let us use our imagination here :-)
            patient = new Patient();
            patient.addId(pat_id);
            Persistor.instance.updatePatient(patient);
            patient = Persistor.instance.getPatient(pat_id);
        }

        /*
         * ...
         * Do anything that would be necessary in the normal execution
         * ...
         */

        // lets check for a ListID
        ListIDGenerator genfac = this.getListIDGeneratorBySubIdType(visit_id_1[0]);
        if(genfac != null) {
            output.append("<h1> Visit 1 </h1>\n");
            // visit_id_1 is contained in a ListID
            ListID listId = (ListID) patient.getId(genfac.getIdType());
            if(listId == null) {
                listId = genfac.getNext();
            }

            // Try to retrieve the ExternalID or create a new one
            ExternalID visit_1 = (ExternalID) IDGeneratorFactory.instance.buildId(visit_id_1[0], visit_id_1[1]);
            if(listId.getIdList().contains(visit_1)) {
                visit_1 = (ExternalID) listId.getId(visit_1);
            }

            // add all other possible ids to visit_1. The ListID generator knows which id Types are allowed.
            Set<String> otherIdTypes = genfac.availableIdTypes();
            // do not create the supplied idType itself
            otherIdTypes.remove(visit_id_1[0]);
            System.out.println(visit_1);
            System.out.println(visit_1.getIdentifiers());
            for(String idType: otherIdTypes) {
                // for each idType: get the IDGenerator and if not external, create the next id 
                IDGenerator<?> gen = IDGeneratorFactory.instance.getFactory(idType);
                System.out.println(visit_1.getIdentifier(idType));
                if(!gen.isExternal() && visit_1.getIdentifier(idType) == null) {
                    visit_1.addIdentifier(gen.getNext());
                }
            }

            // add the ExternalID (including the GeneratedID) to the List
            listId.addToIdList(visit_1);
            // add the ListID to the patient
            patient.addId(listId);
            // persist the Patient, which triggers persisting the ListID, too
            Persistor.instance.updatePatient(patient);

            // re-read the patient and generate some outputs
            patient = Persistor.instance.getPatient(pat_id);
            output.append("<p>Added patient instance to database: " + patient + "</p>\n");
            output.append("<p>The patient includes this ListID: " + patient.getId(genfac.getIdType()) + "</p>\n");

            // and generate some additional output for the ExternalID
            output.append("The external Identifier (" + visit_1 + ") has one sub-ID:\n<ul>\n");
            for(ID subId: visit_1.getIdentifiers()) {
                output.append("<li>" + subId + "</li>\n");
            }
            output.append("</ul>");
        }
        genfac = null;

        // Let us assume we want to find visit_id_1 in our database.
        output.append("<h2>Intermezzo</h2>\n");
        output.append("<p>Getting the external ID from the Persistor directly this time.</p>\n");
        ExternalID intermezzo_extId = (ExternalID) IDGeneratorFactory.instance.buildId(visit_id_1[0], visit_id_1[1]); 

        // check for a ListID containing the searched external ID and retrieve it
        ListID intermezzo_listId = Persistor.instance.getListContaining(intermezzo_extId);
        intermezzo_extId = (ExternalID) intermezzo_listId.getId(intermezzo_extId);

        // generate the output of extId
        output.append("The external Identifier (" + intermezzo_extId + ") has one sub-ID:\n<ul>\n");
        for(ID subId: intermezzo_extId.getIdentifiers()) {
            output.append("<li>" + subId + "</li>\n");
        }
        output.append("</ul>");

        // Let us assume, Visit_2 arrives as a later API call
        genfac = this.getListIDGeneratorBySubIdType(visit_id_2[0]);
        if(genfac != null) {
            output.append("<h1> Visit 2 </h1>\n");
            // get the ListID from the patient
            ListID listId = (ListID) patient.getId(genfac.getIdType());

            // create the externalID and check the list, if it is already available
            ExternalID visit_2 = (ExternalID) IDGeneratorFactory.instance.buildId(visit_id_2[0], visit_id_2[1]);
            // I propose this type of List-checking as I assume it is more performant. I think `.contains()` is 
            // implemented very performantly on the JVM side:
            if(listId.getIdList().contains(visit_2)) {
                for(ID containedId: listId.getIdList()) {
                    if(containedId.equals(visit_2)) {
                        visit_2 = (ExternalID) containedId;
                    }
                }
            }

            // We chose not to add any ids to visit_2. Add the ID to the List and to the Patient as is.
            listId.addToIdList(visit_2);
            patient.addId(listId);

            // As above, persist the patient, which cascades to the ListID aswell.
            Persistor.instance.updatePatient(patient);
            patient = Persistor.instance.getPatient(pat_id);
            output.append("<p>Existing patient instance to database: " + patient + "</p>");
            output.append("<p>The patient includes this ListID: " + patient.getId(genfac.getIdType()) + "</p>");
        }

        return Response.ok(output.toString()).build();
    }
}
