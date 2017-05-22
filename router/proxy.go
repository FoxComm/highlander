package main

import (
	"fmt"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"

	"github.com/FoxComm/highlander/router/utils"
)

// RouterProxy is the meat of this application. It directs to the origin and
// records the request/response pair for analysis.
type RouterProxy struct {
	origin *url.URL
	proxy  *httputil.ReverseProxy
}

// NewRouterProxy creates a new instance of the RouterProxy.
func NewRouterProxy(origin string) (*RouterProxy, error) {
	url, err := url.Parse(origin)
	if err != nil {
		return nil, err
	}

	return &RouterProxy{
		origin: url,
		proxy:  httputil.NewSingleHostReverseProxy(url),
	}, nil
}

func (router *RouterProxy) Run(port string) {
	log.Println("Starting router proxy")
	log.Printf("Directing to %s", router.origin)
	log.Printf("Listening on %s", port)

	http.HandleFunc("/", router.handle)
	http.ListenAndServe(port, nil)
}

func (router *RouterProxy) handle(w http.ResponseWriter, r *http.Request) {
	aw := utils.NewActivityWriter()

	// Set basic headers.
	aw.Header().Set("X-Forwarded-For", xForwardedFor(r))
	aw.Header().Set("X-Forwarded-Proto", xForwardedProto(r))
	aw.Header().Set("X-Forwarded-Port", router.origin.Port())
	aw.Header().Set("Host", router.origin.Host)

	// Handle request.
	router.proxy.ServeHTTP(aw, r)

	// Log the output
	log.Printf("Request URL: %s", r.RequestURI)
	log.Printf("Response status code: %d", aw.StatusCode())
	log.Printf("Response headers: %+v", aw.Header())

	bodyString, err := aw.BodyString()
	if err != nil {
		e := fmt.Errorf("Enable to parse response body with error: %s", err.Error())
		log.Panic(e)
	}

	log.Printf("Response body: %s", bodyString)

	aw.Copy(w)
}

func xForwardedFor(r *http.Request) string {
	return fmt.Sprintf("%s, %s", r.RemoteAddr, r.Host)
}

func xForwardedProto(r *http.Request) string {
	if r.TLS != nil {
		return "https"
	}

	return "http"
}
