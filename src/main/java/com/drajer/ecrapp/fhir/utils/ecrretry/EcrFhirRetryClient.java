package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IGetPage;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.drajer.eca.model.EventTypes;
import com.drajer.ecrapp.fhir.utils.FHIRRetryTemplate;
import com.drajer.ecrapp.fhir.utils.RetryableException;
import com.drajer.sof.utils.FhirClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;

public class EcrFhirRetryClient extends FhirClient {

  private FHIRRetryTemplate fhirRetryTemplate;

  public EcrFhirRetryClient(
      IGenericClient parent,
      FHIRRetryTemplate fhirRetryTemplate,
      String requestId,
      EventTypes.QueryType type) {
    super(parent, requestId, type);
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

  @Override
  public IGetPage loadPage() {
    this.interceptor.incrementPageNum();
    return new EcrFhirRetryablePage(client.loadPage(), this);
  }

  @Override
  public <T extends IBaseBundle> IUntypedQuery<T> search() {
    this.interceptor.reset();
    return new EcrFhirRetryableSearch(client.search(), this);
  }

  public RuntimeException handleException(final Exception e, final String methodName) {
    int httpStatusCode = 0;

    if (e instanceof BaseServerResponseException) {
      httpStatusCode = ((BaseServerResponseException) e).getStatusCode();
    }
    return new RetryableException(e, httpStatusCode, methodName);
  }
}
