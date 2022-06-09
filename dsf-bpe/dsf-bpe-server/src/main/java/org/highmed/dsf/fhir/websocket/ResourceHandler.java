package org.highmed.dsf.fhir.websocket;

import org.hl7.fhir.r4.model.DomainResource;

public interface ResourceHandler<R extends DomainResource>
{
	void onResource(R resource);
}
