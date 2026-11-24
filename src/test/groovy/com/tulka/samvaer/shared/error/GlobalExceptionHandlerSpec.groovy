package com.tulka.samvaer.shared.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import com.tulka.samvaer.shared.logging.CorrelationIdFilter
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import spock.lang.Specification

class GlobalExceptionHandlerSpec extends Specification {

    private Logger logger
    private ListAppender appender

    def setup() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler)
        appender = new ListAppender()
        appender.start()
        logger.addAppender(appender)
    }

    def cleanup() {
        logger.detachAppender(appender)
        appender.stop()
        MDC.clear()
    }

    def "api exception becomes problem details with stable code, field errors and correlation id"() {
        given:
        def handler = new GlobalExceptionHandler()
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, 'corr-123')

        when:
        def response = handler.handleApiException(ApiException.validation([new FieldError('title', 'Give the event a name.')]))
        def body = (ProblemDetail) response.body

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.headers.getContentType() == MediaType.APPLICATION_PROBLEM_JSON
        body.status == 400
        body.title == 'Invalid input'
        body.properties.code == ErrorCode.VALIDATION
        body.properties.fieldErrors == [new FieldError('title', 'Give the event a name.')]
        body.properties.correlationId == 'corr-123'

        and:
        appender.list.size() == 1
        def event = appender.list.first()
        event.level == Level.WARN
        event.mdcPropertyMap[GlobalExceptionHandler.ERROR_CODE_MDC_KEY] == ErrorCode.VALIDATION
        event.mdcPropertyMap[CorrelationIdFilter.CORRELATION_ID_MDC_KEY] == 'corr-123'

        and:
        MDC.get(GlobalExceptionHandler.ERROR_CODE_MDC_KEY) == null
    }

    def "unexpected exception hides its message and logs at error level"() {
        given:
        def handler = new GlobalExceptionHandler()
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, 'corr-500')

        when:
        def response = handler.handleException(new IllegalStateException('boom'))
        def body = (ProblemDetail) response.body

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        body.properties.code == ErrorCode.INTERNAL
        body.title == 'Internal server error'
        body.detail == null
        body.properties.correlationId == 'corr-500'

        and:
        appender.list.size() == 1
        appender.list.first().level == Level.ERROR
        appender.list.first().mdcPropertyMap[GlobalExceptionHandler.ERROR_CODE_MDC_KEY] == ErrorCode.INTERNAL
    }
}
