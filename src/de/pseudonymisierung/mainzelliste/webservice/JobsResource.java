/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste.webservice;

import com.sun.jersey.api.representation.Form;
import com.sun.jersey.spi.resource.Singleton;
import de.pseudonymisierung.mainzelliste.ID;
import de.pseudonymisierung.mainzelliste.IDRequest;
import de.pseudonymisierung.mainzelliste.PatientBackend;
import de.pseudonymisierung.mainzelliste.Servers;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidTokenException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Resource-based access to jobs.
 */
@Path("/jobs")
@Singleton
public class JobsResource {

  private final Logger logger = LogManager.getLogger(JobsResource.class);
  private final Map<Long, Job> jobs = new HashMap<>();

  private static class Job {

    private final String tokenId;
    private final Thread thread;
    private JSONArray result;
    private Throwable throwable;

    public Job(String tokenId, Function<Job, Runnable> runnableSupplier) {
      this.tokenId = StringUtils.trimToEmpty(tokenId);
      this.thread = new Thread(runnableSupplier.apply(this));
      this.thread.setUncaughtExceptionHandler((t, e) -> {
        throwable = e;
        result = new JSONArray();
      });
      this.thread.start();
    }

    public boolean isSuccessful() {
      return throwable == null;
    }

    public void setResult(JSONArray result) {
      this.result = result;
    }

    public String getTokenId() {
      return tokenId;
    }

    public long getId() {
      return thread.getId();
    }

    public Throwable getThrowable() {
      return throwable;
    }
  }


  @Path("/{jobId}/")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobResult(@PathParam("jobId") long jobId,
      @QueryParam("tokenId") String tokenId,
      @Context HttpServletRequest request) {

    Job job = jobs.get(jobId);
    if (job == null) {
      return Response
          .status(Status.NOT_FOUND)
          .entity("job not found")
          .build();
    } else if (!StringUtils.trimToEmpty(tokenId).equals(job.getTokenId())) {
      return Response.status(Status.UNAUTHORIZED)
          .entity("please supply a valid 'addPatients' token")
          .build();
    } else if (job.result == null) {
      return Response
          .status(Status.NO_CONTENT)
          .build();
    } else if (!job.isSuccessful()) {
      jobs.remove(jobId);
      Throwable cause = job.getThrowable();
      if (cause instanceof WebApplicationException) {
        throw (WebApplicationException) cause;
      } else {
        return Response
            .status(Status.INTERNAL_SERVER_ERROR)
            .entity(cause.getMessage())
            .build();
      }
    } else {
      jobs.remove(jobId);
      return Response
          .status(Status.ACCEPTED)
          .entity(job.result)
          .build();
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public synchronized Response runJob(
      @QueryParam("tokenId") String tokenId,
      @Context HttpServletRequest request,
      @Context UriInfo context,
      String batchForms) {

    //read Token
    Token token = readToken(tokenId);

    // prepare job task
    Function<MultivaluedMap<String, String>, JSONObject> jobTask;
    if (token instanceof AddPatientsToken) {
      jobTask = form -> addPatient((AddPatientsToken) token, form, context);
    } else {
      logger.error("Token {} is not of type 'addPatients' but '{}'", tokenId, token.getType());
      throw new InvalidTokenException("Please supply a valid 'addPatients' token.",
          Status.UNAUTHORIZED);
    }

    // deserialize JSON to a list of MultivaluedMap
    ObjectMapper mapper = new ObjectMapper();
    List<MultivaluedMap<String, String>> formData;
    try {
      //workaround: deserialize to MultiValuedMap
      formData = ((List<Map<String, String>>) mapper
          .readValue(batchForms, new TypeReference<List<Map<String, String>>>() {})).stream()
          .map(map -> {
            Form form = new Form();
            map.forEach(form::putSingle);
            return form;
          }).collect(Collectors.toList());
    } catch (IOException e) {
      return Response
          .status(Status.BAD_REQUEST)
          .entity(e.getMessage())
          .build();
    }

    // create and start the job
    Job job = new Job(tokenId, w -> () -> {
      JSONArray result = new JSONArray();
      for (MultivaluedMap<String, String> form : formData) {
        JSONObject jsonResponse = new JSONObject();
        try {
          jsonResponse = processTask(jobTask, form);
        } catch (JSONException e) {
          logger.error("can't serialize response to json", e);
        }
        result.put(jsonResponse);
      }
      w.setResult(result);
    });
    jobs.put(job.getId(), job);

    return Response
        .status(Status.ACCEPTED)
        .location(context.getBaseUriBuilder()
            .path("/jobs/{jobId}/")
            .queryParam("tokenId", tokenId)
            .build(job.thread.getId()))
        .build();
  }

  private JSONObject processTask(Function<MultivaluedMap<String, String>, JSONObject> jobTask,
      MultivaluedMap<String, String> form) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    try {
      jsonResponse = jobTask.apply(form);
    } catch (WebApplicationException e) {
      jsonResponse.put("status", e.getResponse().getStatus());
      jsonResponse.put("body", new JSONObject().put("message", e.getResponse().getEntity()));
    } catch (RuntimeException e) {
      jsonResponse.put("status", Status.INTERNAL_SERVER_ERROR.getStatusCode());
      jsonResponse.put("body", new JSONObject().put("message", e.getMessage()));
    }
    return jsonResponse;
  }

  private JSONObject addPatient(AddPatientsToken token, MultivaluedMap<String, String> form,
      UriInfo uriInfo) {
    // create patient
    IDRequest response = PatientsResource.addNewPatient(form, new HashMap<>(), new HashMap<>(),
        token.getRequestedIdTypes(), token.getId());

    // serialize result to json
    JSONObject jsonResponse = new JSONObject();
    try {
      if (response.getMatchResult().getResultType() != MatchResultType.POSSIBLE_MATCH) {
        jsonResponse.put("status", Status.CREATED.getStatusCode());
        JSONArray jsonData = new JSONArray();
        for (ID thisID : response.createRequestedIds()) {
          URI newUri = uriInfo.getBaseUriBuilder()
              .path(PatientsResource.class)
              .path("/{idtype}/{idvalue}")
              .build(thisID.getType(), thisID.getEncryptedIdStringFirst());

          jsonData.put(thisID.toJSON()
              .put("uri", newUri));
        }
        jsonResponse.put("body", jsonData);
      } else if (response.getAssignedPatient() == null) {
        jsonResponse.put("status", Status.CONFLICT.getStatusCode());
        jsonResponse.put("body", new JSONObject().put("message", "Unable to definitely determined "
            + "whether the data refers to an existing or to a new patient. Please check data or "
            + "resubmit with sureness=true to get a tentative result. Please check documentation "
            + "for details."));
      }
    } catch (JSONException e) {
      logger.error("can't serialize result to json", e);
    }

    return jsonResponse;
  }

  /* Helpers */

  /**
   * find token with the given token id and token class
   *
   * @param tokenId token id
   * @return token
   */
  private Token readToken(String tokenId) {
    return Optional.ofNullable(Servers.instance.getTokenByTid(tokenId))
        .orElseThrow(() -> {
          logger.error("No token with id {} found", tokenId);
          return new InvalidTokenException("Please supply a valid 'addPatients' token.",
              Status.UNAUTHORIZED);
        });
  }
}
