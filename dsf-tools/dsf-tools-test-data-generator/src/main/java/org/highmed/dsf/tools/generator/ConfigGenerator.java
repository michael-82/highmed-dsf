package org.highmed.dsf.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.highmed.dsf.tools.generator.CertificateGenerator.CertificateFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigGenerator.class);

	private Properties javaTestFhirConfigProperties;
	private Properties dockerTestFhirConfigProperties;
	private Properties dockerMedic1FhirConfigProperties;
	private Properties dockerMedic2FhirConfigProperties;
	private Properties dockerMedic3FhirConfigProperties;
	private Properties dockerTtpFhirConfigProperties;

	private Properties readProperties(Path propertiesFile)
	{
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(propertiesFile);
				InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			properties.load(reader);
		}
		catch (IOException e)
		{
			logger.error("Error while reading properties from " + propertiesFile.toString(), e);
			throw new RuntimeException(e);
		}
		return properties;
	}

	private void writeProperties(Path propertiesFiles, Properties properties)
	{
		try (OutputStream out = Files.newOutputStream(propertiesFiles);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			properties.store(writer, "Generated by test-data-generator");
		}
		catch (IOException e)
		{
			logger.error("Error while writing properties to " + propertiesFiles.toString(), e);
			throw new RuntimeException(e);
		}
	}

	public void modifyJavaTestFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles testClient = clientCertificateFilesByCommonName.get("test-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path javaTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/java-test-fhir-config.properties");
		javaTestFhirConfigProperties = readProperties(javaTestFhirConfigTemplateFile);
		javaTestFhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				testClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		System.out.println("PPPP " + javaTestFhirConfigProperties);

		writeProperties(Paths.get("config/java-test-fhir-config.properties"), javaTestFhirConfigProperties);
	}

	public void modifyDockerTestFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles testClient = clientCertificateFilesByCommonName.get("test-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-fhir-config.properties");
		dockerTestFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerTestFhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				testClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-fhir-config.properties"), dockerTestFhirConfigProperties);
	}

	public void copyJavaTestFhirConfigProperties()
	{
		Path javaTestConfigPropertiesFile = Paths.get("../../dsf-fhir/dsf-fhir-server-jetty/conf/config.properties");
		logger.info("Copying config.properties to {}", javaTestConfigPropertiesFile);
		writeProperties(javaTestConfigPropertiesFile, javaTestFhirConfigProperties);
	}

	public void copyDockerTestFhirConfigProperties()
	{
		Path dockerTestFhirConfigPropertiesFile = Paths
				.get("../../dsf-docker-test-setup/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerTestFhirConfigPropertiesFile);
		writeProperties(dockerTestFhirConfigPropertiesFile, dockerTestFhirConfigProperties);
	}

	public void modifyDockerTest3MedicTtpFhirConfigProperties(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		modifyDockerMedic1FhirConfigProperties(clientCertificateFilesByCommonName);
		modifyDockerMedic2FhirConfigProperties(clientCertificateFilesByCommonName);
		modifyDockerMedic3FhirConfigProperties(clientCertificateFilesByCommonName);
		modifyDockerTtpFhirConfigProperties(clientCertificateFilesByCommonName);
	}

	private void modifyDockerMedic1FhirConfigProperties(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles medic1Client = clientCertificateFilesByCommonName.get("medic1-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-medic1-fhir-config.properties");
		dockerMedic1FhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerMedic1FhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				medic1Client.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-medic1-fhir-config.properties"),
				dockerMedic1FhirConfigProperties);
	}

	private void modifyDockerMedic2FhirConfigProperties(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles medic2Client = clientCertificateFilesByCommonName.get("medic2-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-medic2-fhir-config.properties");
		dockerMedic2FhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerMedic2FhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				medic2Client.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-medic2-fhir-config.properties"),
				dockerMedic2FhirConfigProperties);
	}

	private void modifyDockerMedic3FhirConfigProperties(
			Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles medic3Client = clientCertificateFilesByCommonName.get("medic3-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-medic3-fhir-config.properties");
		dockerMedic3FhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerMedic3FhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				medic3Client.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-medic3-fhir-config.properties"),
				dockerMedic3FhirConfigProperties);
	}

	private void modifyDockerTtpFhirConfigProperties(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		CertificateFiles ttpClient = clientCertificateFilesByCommonName.get("ttp-client");
		CertificateFiles webbrowserTestUser = clientCertificateFilesByCommonName.get("Webbrowser Test User");

		Path dockerTestFhirConfigTemplateFile = Paths
				.get("src/main/resources/config-templates/docker-test-ttp-fhir-config.properties");
		dockerTtpFhirConfigProperties = readProperties(dockerTestFhirConfigTemplateFile);
		dockerTtpFhirConfigProperties.setProperty("org.highmed.dsf.fhir.local-user.thumbprints",
				ttpClient.getCertificateSha512ThumbprintHex() + ","
						+ webbrowserTestUser.getCertificateSha512ThumbprintHex());

		writeProperties(Paths.get("config/docker-test-ttp-fhir-config.properties"), dockerTtpFhirConfigProperties);
	}

	public void copyDockerTest3MedicTtpFhirConfigProperties()
	{
		Path dockerMedic1FhirConfigPropertiesFile = Paths
				.get("../../dsf-docker-test-setup-3medic-ttp/medic1/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerMedic1FhirConfigPropertiesFile);
		writeProperties(dockerMedic1FhirConfigPropertiesFile, dockerMedic1FhirConfigProperties);

		Path dockerMedic2FhirConfigPropertiesFile = Paths
				.get("../../dsf-docker-test-setup-3medic-ttp/medic2/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerMedic2FhirConfigPropertiesFile);
		writeProperties(dockerMedic2FhirConfigPropertiesFile, dockerMedic2FhirConfigProperties);

		Path dockerMedic3FhirConfigPropertiesFile = Paths
				.get("../../dsf-docker-test-setup-3medic-ttp/medic3/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerMedic3FhirConfigPropertiesFile);
		writeProperties(dockerMedic3FhirConfigPropertiesFile, dockerMedic3FhirConfigProperties);

		Path dockerTtpFhirConfigPropertiesFile = Paths
				.get("../../dsf-docker-test-setup-3medic-ttp/ttp/fhir/app/conf/config.properties");
		logger.info("Copying config.properties to {}", dockerTtpFhirConfigPropertiesFile);
		writeProperties(dockerTtpFhirConfigPropertiesFile, dockerTtpFhirConfigProperties);

	}
}
