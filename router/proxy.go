package main

import (
	"fmt"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
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
	// Set basic headers.
	w.Header().Set("X-Forwarded-For", xForwardedFor(r))
	w.Header().Set("X-Forwarded-Proto", xForwardedProto(r))
	w.Header().Set("X-Forwarded-Port", router.origin.Port())
	w.Header().Set("Host", router.origin.Host)

	// Handle request.
	router.proxy.ServeHTTP(w, r)
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
