package org.highmed.dsf.fhir.webservice.impl;

import org.highmed.dsf.fhir.dao.ActivityDefinitionDao;
import org.highmed.dsf.fhir.event.EventGenerator;
import org.highmed.dsf.fhir.event.EventManager;
import org.highmed.dsf.fhir.help.ExceptionHandler;
import org.highmed.dsf.fhir.help.ParameterConverter;
import org.highmed.dsf.fhir.help.ResponseGenerator;
import org.highmed.dsf.fhir.service.ReferenceExtractor;
import org.highmed.dsf.fhir.service.ReferenceResolver;
import org.highmed.dsf.fhir.service.ResourceValidator;
import org.highmed.dsf.fhir.webservice.specification.ActivityDefinitionService;
import org.hl7.fhir.r4.model.ActivityDefinition;

public class ActivityDefinitionServiceImpl extends AbstractServiceImpl<ActivityDefinitionDao, ActivityDefinition>
		implements ActivityDefinitionService
{
	public ActivityDefinitionServiceImpl(String resourceTypeName, String serverBase, String path, int defaultPageCount,
			ActivityDefinitionDao dao, ResourceValidator validator, EventManager eventManager,
			ExceptionHandler exceptionHandler, EventGenerator eventGenerator, ResponseGenerator responseGenerator,
			ParameterConverter parameterConverter, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver)
	{
		super(ActivityDefinition.class, resourceTypeName, serverBase, path, defaultPageCount, dao, validator,
				eventManager, exceptionHandler, eventGenerator, responseGenerator, parameterConverter,
				referenceExtractor, referenceResolver);
	}
}
