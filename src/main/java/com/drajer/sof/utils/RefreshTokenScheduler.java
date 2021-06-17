package com.drajer.sof.utils;

import com.drajer.eca.model.ActionRepo;
import com.drajer.sof.model.ClientDetails;
import com.drajer.sof.model.LaunchDetails;
import com.drajer.sof.model.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class RefreshTokenScheduler {

  @Autowired ThreadPoolTaskScheduler taskScheduler;

  private final Logger logger = LoggerFactory.getLogger(RefreshTokenScheduler.class);

  private static final String GRANT_TYPE = "grant_type";
  private static final String SCOPE = "scope";
  private static final String CLIENT_CREDENTIALS = "client_credentials";
  private static final String ACCEPT_HEADER = "Accept";

  public void scheduleJob(LaunchDetails authDetails) {
    logger.info(
        "Scheduling Cron Job to Get AccessToken which expires every {} seconds",
        authDetails.getExpiry());
    long minutes = TimeUnit.SECONDS.toMinutes(authDetails.getExpiry());
    String cronExpression = "0 " + "0/" + minutes + " * * * ?";
    CronTrigger cronTrigger = new CronTrigger(cronExpression);
    taskScheduler.schedule(new RunnableTask(authDetails), cronTrigger);
    logger.info(
        "Job Scheduled to get AccessToken for every {} minutes for Client: {}",
        minutes,
        authDetails.getClientId());
  }

  class RunnableTask implements Runnable {

    private LaunchDetails authDetails;

    public RunnableTask(LaunchDetails authDetails) {
      this.authDetails = authDetails;
    }

    @Override
    public void run() {
      try {
        getAccessTokenUsingLaunchDetails(this.authDetails);
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        logger.info("Error in Getting AccessToken=====>", e);
      }
    }
  }

  public JSONObject getAccessTokenUsingLaunchDetails(LaunchDetails authDetails) {
    JSONObject tokenResponse = null;
    logger.trace("Getting AccessToken for Client: {}", authDetails.getClientId());
    try {
      RestTemplate resTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      if (Boolean.FALSE.equals(authDetails.getIsSystem())) {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(GRANT_TYPE, "refresh_token");
        map.add("refresh_token", authDetails.getRefreshToken());
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<?> response =
            resTemplate.exchange(
                authDetails.getTokenUrl(), HttpMethod.POST, entity, Response.class);
        tokenResponse = new JSONObject(response.getBody());
      } else {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
        String authValues = authDetails.getClientId() + ":" + authDetails.getClientSecret();
        String base64EncodedString =
            Base64.getEncoder().encodeToString(authValues.getBytes(StandardCharsets.UTF_8));
        headers.add("Authorization", "Basic " + base64EncodedString);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(GRANT_TYPE, "client_credentials");
        map.add("scope", authDetails.getScope());
        if (Boolean.TRUE.equals(authDetails.getRequireAud()) && authDetails.getIsSystem()) {
          logger.debug("Adding Aud Parameter while getting AccessToken");
          map.add("aud", authDetails.getEhrServerURL());
        }
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<?> response =
            resTemplate.exchange(
                authDetails.getTokenUrl(),
                authDetails.getIsUserAccountLaunch() ? HttpMethod.GET : HttpMethod.POST,
                entity,
                Response.class);
        tokenResponse = new JSONObject(response.getBody());
      }
      logger.trace("Received AccessToken for Client {}", authDetails.getClientId());
      updateAccessToken(authDetails, tokenResponse);

    } catch (Exception e) {
      logger.error(
          "Error in Getting the AccessToken for the client: {}", authDetails.getClientId(), e);
    }
    return tokenResponse;
  }

  private void updateAccessToken(LaunchDetails authDetails, JSONObject tokenResponse) {
    try {
      LaunchDetails existingAuthDetails =
          ActionRepo.getInstance().getLaunchService().getAuthDetailsById(authDetails.getId());
      if (existingAuthDetails != null) {
        logger.trace("Updating the AccessToken value in LaunchDetails table");
        existingAuthDetails.setAccessToken(tokenResponse.getString("access_token"));
        existingAuthDetails.setExpiry(tokenResponse.getInt("expires_in"));
        existingAuthDetails.setLastUpdated(new Date());
        long expiresInSec = tokenResponse.getLong("expires_in");
        Instant expireInstantTime = new Date().toInstant().plusSeconds(expiresInSec);
        existingAuthDetails.setTokenExpiryDateTime(Date.from(expireInstantTime));
        ActionRepo.getInstance().getLaunchService().saveOrUpdate(existingAuthDetails);
        logger.trace("Successfully updated AccessToken value in LaunchDetails table");
      }
    } catch (Exception e) {
      logger.error("Error in Updating the AccessToken value into LaunchDetails: ", e);
    }
  }

  public JSONObject getAccessTokenUsingClientDetails(ClientDetails clientDetails) {
    JSONObject tokenResponse = null;
    logger.trace("Getting AccessToken for Client: {}", clientDetails.getClientId());
    try {
      RestTemplate resTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
      String authValues = clientDetails.getClientId() + ":" + clientDetails.getClientSecret();
      String base64EncodedString =
          Base64.getEncoder().encodeToString(authValues.getBytes(StandardCharsets.UTF_8));
      headers.add("Authorization", "Basic " + base64EncodedString);
      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add(GRANT_TYPE, "client_credentials");
      map.add("scope", clientDetails.getScopes());
      if (Boolean.TRUE.equals(clientDetails.getRequireAud()) && clientDetails.getIsSystem()) {
        logger.debug("Adding Aud Parameter while getting Access token");
        map.add("aud", clientDetails.getFhirServerBaseURL());
      }
      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

      ResponseEntity<?> response =
          resTemplate.exchange(
              clientDetails.getTokenURL(),
              clientDetails.getIsUserAccountLaunch() ? HttpMethod.GET : HttpMethod.POST,
              entity,
              Response.class);
      tokenResponse = new JSONObject(response.getBody());
      logger.trace("Received AccessToken for Client: {}", clientDetails.getClientId());

    } catch (Exception e) {
      logger.error(
          "Error in Getting the AccessToken for the client: {}", clientDetails.getClientId(), e);
    }
    return tokenResponse;
  }

  public JSONObject getAccessTokenUsingUserCredentials(ClientDetails clientDetails) {
    JSONObject tokenResponse = null;
    logger.info("Getting AccessToken with Username: {}", clientDetails.getClientId());
    try {
      RestTemplate resTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.add(ACCEPT_HEADER, MediaType.APPLICATION_JSON_VALUE);
      String authValues = clientDetails.getClientId() + ":" + clientDetails.getClientSecret();
      String base64EncodedString = Base64.getEncoder().encodeToString(authValues.getBytes("utf-8"));
      headers.add("Authorization", "Basic " + base64EncodedString);
      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add(GRANT_TYPE, CLIENT_CREDENTIALS);
      map.add(SCOPE, clientDetails.getScopes());
      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

      ResponseEntity<?> response =
          resTemplate.exchange(clientDetails.getTokenURL(), HttpMethod.GET, entity, Response.class);
      tokenResponse = new JSONObject(response.getBody());
      logger.info("Received AccessToken for Client: {}", clientDetails.getClientId());
      logger.info("Received AccessToken: {}", tokenResponse);

    } catch (Exception e) {
      logger.error(
          "Error in Getting the AccessToken for the Username: {} ", clientDetails.getClientId(), e);
    }
    return tokenResponse;
  }

  /*
    private void getResourcesData(LaunchDetails authDetails) {
      FhirContext context = resourceData.getFhirContext(authDetails.getFhirVersion());
      context.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
      IGenericClient genericClient =
          resourceData.createClient(
              context, authDetails.getEhrServerURL(), authDetails.getAccessToken());
      try {
        resourceData.getResouceById(
            authDetails, genericClient, context, "Patient", authDetails.getLaunchPatientId());
      } catch (Exception e) {
        logger.error("Error in getting Patient details", e);
      }

      try {
        resourceData.getResouceById(
            authDetails, genericClient, context, "Encounter", authDetails.getEncounterId());
        // resourceData.getEncounterData(client, genericClient, ctx);
      } catch (Exception e) {
        logger.error("Error in getting Encounter details", e);
      }

      try {
        resourceData.getResourceByPatientId(authDetails, genericClient, context, "Observation");
        // resourceData.getObservationData(client, genericClient, ctx);
      } catch (Exception e) {
        logger.error("Error in getting Observation details", e);
      }

      try {
        resourceData.getResourceByPatientId(authDetails, genericClient, context, "Condition");
        // resourceData.getConditionData(client, genericClient, ctx);
      } catch (Exception e) {
        logger.error("Error in getting Condition details", e);
      }

      if (authDetails.getFhirVersion().equalsIgnoreCase("DSTU2")) {
        try {
          resourceData.getResourceByPatientId(
              authDetails, genericClient, context, "MedicationAdministration");
          // resourceData.getMedicationAdministrationData(client, genericClient, ctx);
        } catch (Exception e) {
          logger.error("Error in getting MedicationAdministration details", e);
        }

        try {
          resourceData.getResourceByPatientId(authDetails, genericClient, context, "MedicationOrder");
          // resourceData.getMedicationOrderData(client, genericClient, ctx);
        } catch (Exception e) {
          logger.error("Error in getting MedicationOrder details", e);
        }

        try {
          resourceData.getResourceByPatientId(
              authDetails, genericClient, context, "MedicationStatement");
          // resourceData.getMedicationStatementData(client, genericClient, ctx);
        } catch (Exception e) {
          logger.error("Error in getting MedicationStatement details", e);
        }
      } else if (authDetails.getFhirVersion().equalsIgnoreCase("R4")) {
        try {
          resourceData.getResourceByPatientId(
              authDetails, genericClient, context, "MedicationRequest");
          // resourceData.getMedicationAdministrationData(client, genericClient, ctx);
        } catch (Exception e) {
          logger.error("Error in getting MedicationRequest details", e);
        }
      }
    }
  */
}
