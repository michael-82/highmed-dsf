package org.highmed.dsf.fhir.dao.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;
import org.highmed.dsf.fhir.dao.ActivityDefinitionDao;
import org.highmed.dsf.fhir.search.parameters.ActivityDefinitionIdentifier;
import org.highmed.dsf.fhir.search.parameters.ActivityDefinitionName;
import org.highmed.dsf.fhir.search.parameters.ActivityDefinitionStatus;
import org.highmed.dsf.fhir.search.parameters.ActivityDefinitionUrl;
import org.highmed.dsf.fhir.search.parameters.ActivityDefinitionVersion;
import org.hl7.fhir.r4.model.ActivityDefinition;

import ca.uhn.fhir.context.FhirContext;

public class ActivityDefinitionDaoJdbc extends AbstractResourceDaoJdbc<ActivityDefinition>
		implements ActivityDefinitionDao
{
	public ActivityDefinitionDaoJdbc(BasicDataSource dataSource, FhirContext fhirContext)
	{
		super(dataSource, fhirContext, ActivityDefinition.class, "activity_definitions", "activity_definition",
				"activity_definition_id", ActivityDefinitionIdentifier::new, ActivityDefinitionName::new,
				ActivityDefinitionStatus::new, ActivityDefinitionUrl::new, ActivityDefinitionVersion::new);
	}

	@Override
	protected ActivityDefinition copy(ActivityDefinition resource)
	{
		return resource.copy();
	}
}
