package com.credibanco.integration;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

/**
 * A Camel Java DSL Router
 */
public class ProxyRoute extends RouteBuilder {

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
        onException(Exception.class)
            .handled(true)
            .log(LoggingLevel.INFO, "Error en la petición ${body} ${headers} ${exception.stacktrace}");

        from("netty-http:proxy://0.0.0.0:8080").streamCaching()
            .setHeader("url-resend", simple("${headers." + Exchange.HTTP_SCHEME + "}://"
                + "${headers." + Exchange.HTTP_HOST + "}:"
                + "${headers." + Exchange.HTTP_PORT + "}"
                + "${headers." + Exchange.HTTP_PATH + "}"))
            .log(LoggingLevel.INFO, "Peticion recibida reenviando a ${headers.url-resend}")
            .toD("netty-http:${headers.url-resend}?throwExceptionOnFailure=false")
            .log(LoggingLevel.INFO, "Respuesta del backend inicial: ${body}")
            .choice()
                .when(simple("${headers.CamelHttpResponseCode} == 500"))
                    .removeHeader(Exchange.HTTP_SCHEME)
                    .removeHeader(Exchange.HTTP_HOST)
                    .removeHeader(Exchange.HTTP_PORT)
                    .removeHeader(Exchange.HTTP_PATH)
                    .removeHeader(Exchange.HTTP_RESPONSE_CODE)
                    .removeHeader(Exchange.HTTP_URL)
                    .removeHeader(Exchange.HTTP_URI)
                    .removeHeader(Exchange.HTTP_QUERY)
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .setHeader(Exchange.HTTP_SCHEME, simple("{{schema-mock-1}}"))
                    .setHeader(Exchange.HTTP_HOST, constant("{{host-mock-1}}"))
                    .setHeader(Exchange.HTTP_PORT, constant("{{port-mock-1}}"))
                    .setHeader(Exchange.HTTP_PATH, constant("{{path-mock-1}}"))

                    .log(LoggingLevel.INFO, "Respuesta del backend orignal con error 500 enviando peticion a Mock")
                    .toD("${headers." + Exchange.HTTP_SCHEME + "}://"
                        + "${headers." + Exchange.HTTP_HOST + "}:"
                        + "${headers." + Exchange.HTTP_PORT + "}"
                        + "${headers." + Exchange.HTTP_PATH + "}?bridgeEndpoint=true")
                    .log(LoggingLevel.INFO, "Respuesta del mock ${body}")
                    .process(ProxyRoute::setNewBody)
                .end()
                .log(LoggingLevel.INFO, "Respuesta a enviar ${body}")
            .end();
    }

    public static void setNewBody(final Exchange exchange) {
        final Message message = exchange.getIn();
        final String body = message.getBody(String.class);
        message.setBody(body);
    }
}
