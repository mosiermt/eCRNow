package com.drajer.ecrapp.fhir.utils.ecrretry;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.SummaryEnum;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadIfNoneMatch;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public class EcrFhirRetryableRead implements IRead, IReadTyped, IReadExecutable {

  private IRead readParent;
  private IReadTyped readTypedParent;
  private IReadExecutable readExecutableParent;
  private EcrFhirClient client;

  public EcrFhirRetryableRead(IRead read, EcrFhirClient client) {
    super();
    this.readParent = read;
    this.client = client;
  }

  public EcrFhirRetryableRead(IReadTyped readTyped, EcrFhirClient client) {
    super();
    this.readTypedParent = readTyped;
    this.client = client;
  }

  public EcrFhirRetryableRead(IReadExecutable readExecutable, EcrFhirClient client) {
    super();
    this.readExecutableParent = readExecutable;
    this.client = client;
  }

  @Override
  public <T extends IBaseResource> IReadTyped<T> resource(Class<T> theResourceType) {
    return readParent.resource(theResourceType);
  }

  @Override
  public IReadTyped<IBaseResource> resource(String theResourceType) {
    return new EcrFhirRetryableRead(readParent.resource(theResourceType), this.client);
  }

  @Override
  public IReadExecutable withId(String theId) {
    return new EcrFhirRetryableRead(readTypedParent.withId(theId), this.client);
  }

  @Override
  public IReadExecutable withIdAndVersion(String theId, String theVersion) {
    return readTypedParent.withIdAndVersion(theId, theVersion);
  }

  @Override
  public IReadExecutable withId(Long theId) {
    return readTypedParent.withId(theId);
  }

  @Override
  public IReadExecutable withId(IIdType theId) {
    return readTypedParent.withId(theId);
  }

  @Override
  public IReadExecutable withUrl(String theUrl) {
    return readTypedParent.withUrl(theUrl);
  }

  @Override
  public IReadExecutable withUrl(IIdType theUrl) {
    return readTypedParent.withUrl(theUrl);
  }

  @Override
  public IClientExecutable andLogRequestAndResponse(boolean theLogRequestAndResponse) {
    return readExecutableParent.andLogRequestAndResponse(theLogRequestAndResponse);
  }

  @Override
  public IClientExecutable cacheControl(CacheControlDirective theCacheControlDirective) {

    return readExecutableParent.cacheControl(theCacheControlDirective);
  }

  @Override
  public IClientExecutable elementsSubset(String... theElements) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable encoded(EncodingEnum theEncoding) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable encodedJson() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable encodedXml() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable withAdditionalHeader(String theHeaderName, String theHeaderValue) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Object execute() {

    return client
        .getRetryTemplate()
        .execute(
            retryContext -> {
              return readExecutableParent.execute();
            },
            retryContext -> {
              System.out.println("All retries exausted.");

              return null;
            });
  }

  @Override
  public IClientExecutable preferResponseType(Class theType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable preferResponseTypes(List theTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable prettyPrint() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable summaryMode(SummaryEnum theSummary) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IClientExecutable accept(String theHeaderValue) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IReadIfNoneMatch ifVersionMatches(String theVersion) {
    // TODO Auto-generated method stub
    return null;
  }
}
