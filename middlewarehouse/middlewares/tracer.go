// Package middleware provides some usable transport middleware to deal with
// propagating Zipkin traces across service boundaries.
package middlewares

import (
	"fmt"
	"net"
	"net/http"
	"strconv"

	opentracing "github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	oLog "github.com/opentracing/opentracing-go/log"

	"bytes"
	"github.com/gin-gonic/gin"
	"io/ioutil"
)

// RequestFunc is a middleware function for outgoing HTTP requests.
type RequestFunc func(req *http.Request) *http.Request

// ToHTTPRequest returns a RequestFunc that injects an OpenTracing Span found in
// context into the HTTP Headers. If no such Span can be found, the RequestFunc
// is a noop.
func TraceToHTTPRequest(tracer opentracing.Tracer) RequestFunc {
	return func(req *http.Request) *http.Request {
		// Retrieve the Span from context.
		if span := opentracing.SpanFromContext(req.Context()); span != nil {

			// We are going to use this span in a client request, so mark as such.
			ext.SpanKindRPCClient.Set(span)

			// Add some standard OpenTracing tags, useful in an HTTP request.
			ext.HTTPMethod.Set(span, req.Method)
			span.SetTag("http.host", req.URL.Host)
			span.SetTag("http.path", req.URL.Path)
			ext.HTTPUrl.Set(
				span,
				fmt.Sprintf("%s://%s%s", req.URL.Scheme, req.URL.Host, req.URL.Path),
			)

			// Add information on the peer service we're about to contact.
			if host, portString, err := net.SplitHostPort(req.URL.Host); err == nil {
				ext.PeerHostname.Set(span, host)
				if port, err := strconv.Atoi(portString); err != nil {
					ext.PeerPort.Set(span, uint16(port))
				}
			} else {
				ext.PeerHostname.Set(span, req.URL.Host)
			}

			// Inject the Span context into the outgoing HTTP Request.
			if err := tracer.Inject(
				span.Context(),
				opentracing.TextMap,
				opentracing.HTTPHeadersCarrier(req.Header),
			); err != nil {
				fmt.Printf("error encountered while trying to inject span: %+v", err)
			}
		}
		return req
	}
}

// FromHTTPRequest returns a Middleware HandlerFunc that tries to join with an
// OpenTracing trace found in the HTTP request headers and starts a new Span
// called `operationName`. If no trace could be found in the HTTP request
// headers, the Span will be a trace root. The Span is incorporated in the
// HTTP Context object and can be retrieved with
// opentracing.SpanFromContext(ctx).
func TraceFromHTTPRequest(tracer opentracing.Tracer) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Try to join to a trace propagated in `req`.
		wireContext, err := tracer.Extract(
			opentracing.TextMap,
			opentracing.HTTPHeadersCarrier(c.Request.Header),
		)
		if err != nil {
			fmt.Printf("Parent span not found: %+v\n", err)
		}

		// create span
		span := tracer.StartSpan(c.Request.URL.Path, ext.RPCServerOption(wireContext))
		defer span.Finish()

		ext.HTTPMethod.Set(span, c.Request.Method)
		span.SetTag("http.path", c.Request.URL.Path)

		if body := dumpBody(c.Request); len(body) > 0 {
			span.LogFields(oLog.String("Body", body))
		}

		if jwt := c.Request.Header.Get("JWT"); len(jwt) > 0 {
			span.LogFields(oLog.String("JWT", jwt))
		}

		// store span in context
		ctx := opentracing.ContextWithSpan(c.Request.Context(), span)

		// update request context to include our new span
		c.Request = c.Request.WithContext(ctx)

		fmt.Print("Processing request with new ctx\n")

		c.Next()
	}
}

func dumpBody(req *http.Request) string {
	buf, _ := ioutil.ReadAll(req.Body)
	readerCopy := ioutil.NopCloser(bytes.NewBuffer(buf))

	// set new Reader to body
	req.Body = readerCopy

	return string(buf)
}
