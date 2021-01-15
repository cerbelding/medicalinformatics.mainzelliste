package de.pseudonymisierung.mainzelliste.webservice;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import de.pseudonymisierung.mainzelliste.AssociatedIds;
import de.pseudonymisierung.mainzelliste.AssociatedIdsFactory;
import de.pseudonymisierung.mainzelliste.ExternalID;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.IDGeneratorFactory;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.PlainTextField;
import de.pseudonymisierung.mainzelliste.dto.Persistor;

@Path("/test")
public class IntegrationTest {

    @GET
    @Path("/visit_1")
    public Response testVisitIdCreation(@Context HttpServletRequest req) {
        /*********
         * Setup * 
         *********/
        // test patient to attatch the ids to
        ExternalID pat_id = (ExternalID) IDGeneratorFactory.instance.buildId("patientennummer", "123456789");
        Patient patient = Persistor.instance.getPatient(pat_id);

        if(patient == null) {
            // I am to lazy to populate a real Patient. Let us use our imagination here :-)
            patient = new Patient();
            Map<String, Field<?>> pat_input = new HashMap<String, Field<?>>();
            pat_input.put("vorname", new PlainTextField("Marcel"));
            pat_input.put("nachname", new PlainTextField("Parciak"));
            patient.setFields(pat_input);
            patient.addId(pat_id);
            Persistor.instance.updatePatient(patient);
            patient = Persistor.instance.getPatient(pat_id);
        }

        StringBuilder output = new StringBuilder();
        output.append("<p>We will perform hard-coded tests in order to check the functionality of AssociatedIds.</p>");

        /**********************
         * Prepare dummy data *
         **********************/
        String[] visit_ext_1 = { "extVisitId", "1234asdf" };
        ExternalID visitId_ext_1 = new ExternalID(visit_ext_1[1], visit_ext_1[0]);
        String[] visit_ext_2 = { "extVisitId", "4321fdsa" };
        ExternalID visitId_ext_2 = new ExternalID(visit_ext_2[1], visit_ext_2[0]);
        String[] visit_int_1 = { "intVisitId", "" };
        String[] visit_int_2 = { "intVisitId", "" };

        /*******************
         * Add first visit *
         *******************/

        // Check for an AssociatedId here
        AssociatedIdsFactory assocFactory = AssociatedIdsFactory.getInstance();
        if(assocFactory.isAssociatedIdsID(visitId_ext_1)) {
            output.append("<h1>External Visit 1</h1>\n");

            // get the AssociatedIds instance
            AssociatedIds assocId = assocFactory.getAssociatedIdsFor(visitId_ext_1);
            assert assocId.getId(visit_ext_1[0]) != null;

            // add assoc id to patient and persists
            patient.addAssociatedIds(assocId);
            Persistor.instance.updatePatient(patient);

            // update the patient instance
            patient = Persistor.instance.getPatient(pat_id);

            assert patient.getAssociatedIdsList().contains(assocId);

            // generate some output
            output.append("<p>Added patient instance to database: " + patient + "</p>\n");
            output.append("<p>Created External ID and added to patient: " + visitId_ext_1+ "</p>\n");
            output.append("<p>The patient includes AssociatedIds:</p>\n<ul>\n");
            for(AssociatedIds assocId_out: patient.getAssociatedIdsList()) {
                output.append("<li>" + assocId_out + "</li>\n");
            }
            output.append("</ul>");
        }

        /*******************
         * Add second visit *
         *******************/

        // Again, check of an AssociatedIds
        if(assocFactory.isAssociatedIdsID(visitId_ext_2)) {
            output.append("<h1>External Visit 2</h1>\n");
            output.append("<p>This time, we will create an internal ID right away");
            // get the AssociatedIds instance
            AssociatedIds assocId = assocFactory.getAssociatedIdsFor(visitId_ext_2);
            assert assocId.getId(visit_ext_2[0]) != null;
            assocId = assocFactory.createAllIdentifiers(assocId);
            assert assocId.getIds().size() == 2;

            // add assoc id to patient and persists
            patient.addAssociatedIds(assocId);
            Persistor.instance.updatePatient(patient);

            // update the patient instance
            patient = Persistor.instance.getPatient(pat_id);

            assert patient.getAssociatedIdsList().contains(assocId);

            // generate some output
            output.append("<p>Added patient instance to database: " + patient + "</p>\n");
            output.append("<p>Created External ID and added to patient: " + visitId_ext_2 + " + an internally created intVisitId</p>\n");
            output.append("<p>The patient includes AssociatedIds:</p>\n<ul>\n");
            for(AssociatedIds assocId_out: patient.getAssociatedIdsList()) {
                output.append("<li>" + assocId_out + "</li>\n");
            }
            output.append("</ul>");
        }

        /*********************
         * Create internally *
         *********************/
        if(assocFactory.isAssociatedIdsIdType(visit_int_1[0]) && assocFactory.isAssociatedIdsIdType(visit_int_2[0])) {
            output.append("<h1>Internal Visits</h1>\n");
            output.append("<p>Create two internal IDs now and attach them to the patient</p>\n");

            output.append("Creating new Identifiers:\n<ul>");
            for(String idType: new String[] {visit_int_1[0], visit_int_2[0]}) {
                // appropriate for "create", as we want to have a new id
                AssociatedIds assocId = assocFactory.createAssociatedIdsFor(visit_int_1[0]);
                assocId.addId(IDGeneratorFactory.instance.getFactory(idType).getNext());
                patient.addAssociatedIds(assocId);
                output.append("<li>"+assocId.getId(idType)+"</li>\n");
            }
            output.append("</ul>");

            // store the assocs with the patient
            Persistor.instance.updatePatient(patient);
        }

        /****************
         * Check assocs *
         ****************/
        output.append("<h1>Check for all AssociatedIds now.</h1>\n");
        patient = Persistor.instance.getPatient(pat_id);
        output.append("<p>The patient includes AssociatedIds:</p>\n<ul>\n");
        for(AssociatedIds assocId_out: patient.getAssociatedIdsList()) {
            output.append("<li>" + assocId_out + "</li>\n");
        }
        output.append("</ul>");

        /***********************
         * Checking duplicates *
         ***********************/
        // Check for an AssociatedId here
        if(assocFactory.isAssociatedIdsID(visitId_ext_1)) {
            output.append("<h1>Copy of External Visit 1</h1>\n");

            // get the AssociatedIds instance
            AssociatedIds assocId = assocFactory.getAssociatedIdsFor(visitId_ext_1);
            assert assocId.getId(visit_ext_1[0]) != null;

            // add assoc id to patient and persists
            patient.addAssociatedIds(assocId);
            Persistor.instance.updatePatient(patient);

            // update the patient instance
            patient = Persistor.instance.getPatient(pat_id);

            assert patient.getAssociatedIdsList().contains(assocId);

            // generate some output
            output.append("<p>Added patient instance to database: " + patient + "</p>\n");
            output.append("<p>Created External ID and added to patient: " + visitId_ext_1+ "</p>\n");
            output.append("<p>The patient includes AssociatedIds:</p>\n<ul>\n");
            for(AssociatedIds assocId_out: patient.getAssociatedIdsList()) {
                output.append("<li>" + assocId_out + "</li>\n");
            }
            output.append("</ul>");
        }

        /****************
         * Check assocs *
         ****************/
        output.append("<h1>Re-Check for all AssociatedIds now.</h1>\n");
        output.append("<p>Expecation: the number of AssociatedIds did not grow compared to the last check.</p>\n");
        patient = Persistor.instance.getPatient(pat_id);
        output.append("<p>The patient includes AssociatedIds:</p>\n<ul>\n");
        for(AssociatedIds assocId_out: patient.getAssociatedIdsList()) {
            output.append("<li>" + assocId_out + "</li>\n");
        }
        output.append("</ul>");

        return Response.ok(output.toString()).build();
    }
}
