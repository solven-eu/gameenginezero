<?xml version="1.0" encoding="UTF-8"?>

<!-- Copyright © 2019 MiTrust (cto@m-itrust.com). Unauthorized copying of this file, via any medium is strictly prohibited. Proprietary and confidential -->

<configuration scan="true" debug="false" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="logback.xsd">
	<!-- http://logback.qos.ch/manual/jmxConfig.html -->
	<!-- TODO: stop in a ServletContextListener . See logback doc -->
	<contextName>MyOnlineService</contextName>
	<jmxConfigurator />

	<!-- http://logback.qos.ch/manual/configuration.html -->
	<contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator" />

	<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
		<!-- encoders are assigned the type ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
		<encoder>
			<!-- http://stackoverflow.com/questions/2005929/howto-prevent-eclipse-from-line-wrapping-in-xml-and-html-files -->
			<!-- '%highlight' works under unix even if withJansi==false -->
			<!-- Not a '.' between class and method in order to easily double-click to select the class and dreictly poaste in IDE -->
			<pattern>
				<![CDATA[%date %-5level[%thread] %logger{36}|%method\(%line\) - %msg%n]]>
			</pattern>
		</encoder>

		<!-- https://logback.qos.ch/manual/appenders.html#conAppWithJansi -->
		<!-- ANSI fails in our Windows env: https://jira.qos.ch/browse/LOGBACK-762 -->
		<withJansi>false</withJansi>
	</appender>

	<!-- Configure the Sentry appender, overriding the logging threshold to the WARN level -->
	<appender name="SENTRY" class="io.sentry.logback.SentryAppender">
		<!-- Default for Events is ERROR -->
		<minimumEventLevel>WARN</minimumEventLevel>
		<!-- Default for Breadcrumbs is INFO -->
		<minimumBreadcrumbLevel>INFO</minimumBreadcrumbLevel>
	</appender>

	<appender name="ELK" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
		<destination>prd-opendistro.francecentral.cloudapp.azure.com:5000</destination>

		<!-- We seem to observe unability of Logstash appender to reconnect -->
		<!-- https://github.com/logstash/logstash-logback-encoder#keep-alive -->
		<keepAliveDuration>5 minutes</keepAliveDuration>

		<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
			<!-- https://github.com/logstash/logstash-logback-encoder#providers-for-loggingevents -->
			<providers>
				<timestamp />
				<mdc />
				<!-- MDC variables on the Thread will be written as JSON fields -->
				<context />
				<!--Outputs entries from logback's context -->
				<version />
				<!-- Logstash json format version, the @version field in the output -->
				<logLevel />
				<logLevelValue />
				<loggerName />

				<callerData />

				<threadName />
				<rawMessage />
				<message />

				<logstashMarkers />
				<!-- Useful so we can add extra information for specific log lines as Markers -->
				<arguments />
				<!--or through StructuredArguments -->

				<stackTrace />
				<stackHash />
				<throwableClassName />
				<throwableRootCauseClassName />

				<!-- java.lang.ClassNotFoundException: com.fasterxml.uuid.NoArgGenerator -->
				<uuid>
					<!-- Default field in ELK/Bitnami -->
					<fieldName>logstash_checksum</fieldName>
				</uuid>
			</providers>
		</encoder>
	</appender>

	<logger name="org.springframework" level="INFO" />
	<logger name="org.springframework.security" level="info" />

	<!-- https://stackoverflow.com/questions/28272284/how-to-disable-jooqs-self-ad-message-in-3-4 -->
	<logger name="org.jooq.Constants" level="WARN" />

	<!-- https://stackoverflow.com/questions/27230702/speed-up-spring-boot-startup-time -->
	<logger name="org.springframework.boot.autoconfigure" level="INFO" />

	<root level="INFO">
		<appender-ref ref="STDOUT" />
		<appender-ref ref="SENTRY" />
		<appender-ref ref="ELK" />
	</root>
</configuration>
