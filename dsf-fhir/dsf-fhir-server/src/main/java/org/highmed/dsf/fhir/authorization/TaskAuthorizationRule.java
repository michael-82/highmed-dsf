package org.highmed.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.highmed.dsf.fhir.authentication.OrganizationProvider;
import org.highmed.dsf.fhir.authentication.User;
import org.highmed.dsf.fhir.dao.TaskDao;
import org.highmed.dsf.fhir.dao.provider.DaoProvider;
import org.highmed.dsf.fhir.service.ReferenceResolver;
import org.highmed.dsf.fhir.service.ResourceReference;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class TaskAuthorizationRule extends AbstractAuthorizationRule<Task, TaskDao>
{
	private static final Logger logger = LoggerFactory.getLogger(TaskAuthorizationRule.class);

	private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://highmed.org/fhir/CodeSystem/bpmn-message";
	private static final String CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";

	private static final String INSTANTIATES_URI_PATTERN_STRING = "(http://highmed.org/bpe/Process/[-\\w]+)/(\\d+\\.\\d+\\.\\d+)";
	private static final Pattern INSTANTIATES_URI_PATTERN = Pattern.compile(INSTANTIATES_URI_PATTERN_STRING);

	private final ActivityDefinitionProvider activityDefinitionProvider;

	public TaskAuthorizationRule(DaoProvider daoProvider, String serverBase, ReferenceResolver referenceResolver,
			OrganizationProvider organizationProvider, ActivityDefinitionProvider activityDefinitionProvider)
	{
		super(Task.class, daoProvider, serverBase, referenceResolver, organizationProvider);

		this.activityDefinitionProvider = activityDefinitionProvider;
	}

	@Override
	public Optional<String> reasonCreateAllowed(Connection connection, User user, Task newResource)
	{
		if (isLocalUser(user) || isRemoteUser(user))
		{
			Optional<String> errors = newResourceOk(connection, user, newResource);
			if (errors.isEmpty())
			{
				if (taskAllowedForUser(connection, user, newResource.getInstantiatesUri(),
						getBpmnMessageName(newResource).findFirst().orElseThrow()))
				{
					logger.info("Create of Task authorized for local or remote user '{}'", user.getName());
					return Optional.of(
							"local or remote user, task.status draft or requested, task.requester current users organization, task.restriction.recipient local organization, process with instantiatesUri and message-name allowed for current user");
				}
				else
				{
					logger.warn(
							"Create of Task unauthorized, process with instantiatesUri and message-name not allowed for current user",
							user.getName());
					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Create of Task unauthorized, " + errors.get());
				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Create of Task unauthorized, not a local or remote user");
			return Optional.empty();
		}
	}

	private Optional<String> newResourceOk(Connection connection, User user, Task newResource)
	{
		List<String> errors = new ArrayList<String>();

		if (newResource.hasStatus())
		{
			if (!EnumSet.of(TaskStatus.DRAFT, TaskStatus.REQUESTED).contains(newResource.getStatus()))
				errors.add("task.status not draft or requested");
		}
		else
		{
			errors.add("task.status missing");
		}

		if (newResource.hasRequester())
		{
			if (!isCurrentUserPartOfReferencedOrganization(connection, user, "task.requester",
					newResource.getRequester()))
			{
				errors.add("task.requester user not part of referenced organization");
			}
		}
		else
		{
			errors.add("task.requester missing");
		}

		if (newResource.hasRestriction())
		{
			if (newResource.getRestriction().hasRecipient() && newResource.getRestriction().getRecipient().size() == 1)
			{
				ResourceReference reference = new ResourceReference("task.restriction.recipient",
						newResource.getRestriction().getRecipientFirstRep(), Organization.class);
				Optional<Resource> recipient = referenceResolver.resolveReference(user, reference, connection);
				if (recipient.isPresent())
				{
					if (recipient.get() instanceof Organization)
					{
						if (!isLocalOrganization((Organization) recipient.get()))
							errors.add("task.restriction.recipient not local organization");
					}
					else
					{
						errors.add("task.restriction.recipient not a organization");
					}
				}
				else
				{
					errors.add("task.restriction.recipient could not be resolved");
				}
			}
			else
			{
				errors.add("task.restriction.recipient missing or more than one");
			}
		}
		else
		{
			errors.add("task.restriction missing");
		}

		if (newResource.hasInstantiatesUri())
		{
			if (!INSTANTIATES_URI_PATTERN.matcher(newResource.getInstantiatesUri()).matches())
			{
				errors.add("task.instantiatesUri not matching " + INSTANTIATES_URI_PATTERN_STRING + " pattern");
			}
		}
		else
		{
			errors.add("task.instantiatesUri missing");
		}

		if (newResource.hasInput())
		{
			if (getBpmnMessageName(newResource).count() != 1)
			{
				errors.add("task.input with system " + CODE_SYSTEM_BPMN_MESSAGE + " and code "
						+ CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME + " with string value not empty missing");
			}
		}
		else
		{
			errors.add("task.input empty");
		}

		if (newResource.hasOutput())
		{
			errors.add("task.output not empty");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	private Stream<String> getBpmnMessageName(Task newResource)
	{
		return newResource.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> CODE_SYSTEM_BPMN_MESSAGE.equals(c.getSystem())
								&& CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME.equals(c.getCode()))
						&& i.getValue() instanceof StringType && !((StringType) i.getValue()).getValue().isBlank())
				.map(i -> ((StringType) i.getValue()).getValue());
	}

	private boolean taskAllowedForUser(Connection connection, User user, String instantiatesUri, String messageName)
	{
		Matcher matcher = INSTANTIATES_URI_PATTERN.matcher(instantiatesUri);
		if (matcher.matches())
		{
			String processUrl = matcher.group(1);
			String processVersion = matcher.group(2);

			return activityDefinitionProvider
					.getActivityDefinition(connection, user, processUrl, processVersion, messageName).map(ad -> true)
					.orElse(false);
		}
		else
			return false;
	}

	@Override
	public Optional<String> reasonReadAllowed(Connection connection, User user, Task existingResource)
	{
		if (isCurrentUserPartOfReferencedOrganization(connection, user, "task.requester",
				existingResource.getRequester()))
		{
			logger.info(
					"Read of Task authorized, task.requester reference could be resolved and user '{}' is part of referenced organization",
					user.getName());
			return Optional.of("task.requester resolved and user part of referenced organization");
		}
		else if (isLocalUser(user) && isCurrentUserPartOfReferencedOrganization(connection, user,
				"task.restriction.recipient", existingResource.getRestriction().getRecipientFirstRep()))
		{
			logger.info(
					"Read of Task authorized, task.restriction.recipient reference could be resolved and user '{}' is part of referenced organization",
					user.getName());
			return Optional.of("task.restriction.recipient resolved and local user part of referenced organization");
		}
		else
		{
			logger.warn(
					"Read of Task unauthorized, task.requester or task.restriction.recipient references could not be resolved or user '{}' not part of referenced organizations",
					user.getName());
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonUpdateAllowed(Connection connection, User user, Task oldResource, Task newResource)
	{
		if (isRemoteUser(user) || isLocalUser(user))
		{
			if (TaskStatus.DRAFT.equals(oldResource.getStatus()) && isCurrentUserPartOfReferencedOrganization(
					connection, user, "task.requester", oldResource.getRequester()))
			{
				Optional<String> errors = newResourceOk(connection, user, newResource);
				if (errors.isEmpty())
				{
					logger.info("Update of Task authorized for local or remote user '{}'", user.getName());
					return Optional.of(
							"local or remote user, task.status draft or requested, task.requester current users organization, task.restriction.recipient local organization");
				}
				else
				{
					logger.warn("Create of Task unauthorized, " + errors.get());
					return Optional.empty();
				}
			}
			else if (isLocalUser(user)
					&& EnumSet.of(TaskStatus.REQUESTED, TaskStatus.INPROGRESS).contains(oldResource.getStatus())
					&& isCurrentUserPartOfReferencedOrganization(connection, user, "task.restriction.recipient",
							oldResource.getRestriction().getRecipientFirstRep()))
			{
				Optional<String> same = reasonNotSame(oldResource, newResource);
				if (same.isEmpty())
				{
					// REQUESTED -> INPROGRESS
					if (TaskStatus.REQUESTED.equals(oldResource.getStatus())
							&& TaskStatus.INPROGRESS.equals(newResource.getStatus()))
					{
						if (!newResource.hasOutput())
						{
							logger.info(
									"local user (user is part of task.restriction.recipient organization), task.status inprogress, properties task.instantiatesUri, task.requester, task.restriction, task.input not changed");
							return Optional.of(
									"local user (user part of task.restriction.recipient), task.status inprogress, properties task.instantiatesUri, task.requester, task.restriction, task.input not changed");
						}
						else
						{
							logger.warn("Update of Task unauthorized, task.output not expected");
							return Optional.empty();
						}
					}
					// INPROGRESS -> COMPLETED or FAILED
					else if (TaskStatus.INPROGRESS.equals(oldResource.getStatus())
							&& (TaskStatus.COMPLETED.equals(newResource.getStatus())
									|| TaskStatus.FAILED.equals(newResource.getStatus())))
					{
						// might have output
						logger.info(
								"local user (user is part of task.restriction.recipient organization), task.status completed or failed, properties task.instantiatesUri, task.requester, task.restriction, task.input not changed");
						return Optional.of(
								"local user (user part of task.restriction.recipient), task.status completed or failed, properties task.instantiatesUri, task.requester, task.restriction, task.input not changed");
					}
					else
					{
						logger.warn("Update of Task unauthorized, task.status change {} -> {} not allowed",
								oldResource.getStatus(), newResource.getStatus());
						return Optional.empty();
					}
				}
				else
				{
					logger.warn("Update of Task unauthorized, task properties {} changed", same.get());
					return Optional.empty();
				}
			}
			else
			{
				logger.warn(
						"Update of Task unauthorized, expected taks.status draft and current user part of task.requester or task.status requester or inprogress and current local user part of task.restriction.recipient");
				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Update of Task unauthorized, not a local or remote user");
			return Optional.empty();
		}
	}

	private Optional<String> reasonNotSame(Task oldResource, Task newResource)
	{
		List<String> errors = new ArrayList<String>();
		if (!oldResource.getRequester().equalsDeep(newResource.getRequester()))
		{
			errors.add("task.requester");
		}

		if (!oldResource.getRestriction().equalsDeep(newResource.getRestriction()))
		{
			errors.add("task.restriction");
		}

		if (!oldResource.getInstantiatesUri().equals(newResource.getInstantiatesUri()))
		{
			errors.add("task.instantiatesUri");
		}

		if (oldResource.getInput().size() != newResource.getInput().size())
		{
			errors.add("task.input");
		}
		else
		{
			for (int i = 0; i < oldResource.getInput().size(); i++)
			{
				if (!oldResource.getInput().get(i).equalsDeep(newResource.getInput().get(i)))
				{
					errors.add("task.input[" + i + "]");
					break;
				}
			}
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
		{
			logger.debug("Old Task: {}", FhirContext.forR4().newJsonParser().setStripVersionsFromReferences(false)
					.encodeResourceToString(oldResource));
			logger.debug("New Task: {}", FhirContext.forR4().newJsonParser().setStripVersionsFromReferences(false)
					.encodeResourceToString(newResource));
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
		}
	}

	@Override
	public Optional<String> reasonDeleteAllowed(Connection connection, User user, Task oldResource)
	{
		if (TaskStatus.DRAFT.equals(oldResource.getStatus()) && isCurrentUserPartOfReferencedOrganization(connection,
				user, "task.requester", oldResource.getRequester()))
		{
			logger.info(
					"Delete of Task authorized for user '{}', task.status draft, task.requester resolved and user part of referenced organization",
					user.getName());
			return Optional.of("task.status draft, task.requester resolved and user part of referenced organization");
		}
		else if (isLocalUser(user) && TaskStatus.DRAFT.equals(oldResource.getStatus())
				&& isCurrentUserPartOfReferencedOrganization(connection, user, "task.restriction.recipient",
						oldResource.getRestriction().getRecipientFirstRep()))
		{
			logger.info(
					"Delete of Task authorized for local user '{}', task.status draft, task.restriction.recipient resolved and user part of referenced organization",
					user.getName());
			return Optional.of(
					"local user, task.status draft, task.restriction.recipient resolved and user part of referenced organization");
		}
		else
		{
			logger.warn(
					"Delete of Task unauthorized, task.status not draft, task.requester not current user or task.restriction.recipient not local user");
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonSearchAllowed(Connection connection, User user)
	{
		logger.info("Search of Task authorized for {} user '{}', will be fitered by users organization {}",
				user.getRole(), user.getName(), user.getOrganization().getIdElement().getValueAsString());
		return Optional.of("Allowed for all, filtered by users organization");
	}
}
