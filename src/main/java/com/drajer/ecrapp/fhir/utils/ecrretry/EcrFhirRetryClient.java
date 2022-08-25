package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import com.drajer.ecrapp.fhir.utils.FHIRRetryTemplate;
import com.drajer.sof.utils.FhirClient;

public class EcrFhirRetryClient extends FhirClient {

  private FHIRRetryTemplate fhirRetryTemplate;

  public EcrFhirRetryClient(
      IGenericClient parent, FHIRRetryTemplate fhirRetryTemplate, String requestId) {
    super(parent, requestId);
    this.fhirRetryTemplate = fhirRetryTemplate;
  }

  public FHIRRetryTemplate getRetryTemplate() {
    return fhirRetryTemplate;
  }

  @Override
  public IRead read() {
    this.interceptor.reset();
    return new EcrFhirRetryableRead(client.read(), this);
  }
}