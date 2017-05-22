package main

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"

	"github.com/FoxComm/highlander/router/utils"
)

// RouterProxy is the meat of this application. It directs to the origin and
// records the request/response pair for analysis.
type RouterProxy struct {
	origin *url.URL
	host   *url.URL
	proxy  *httputil.ReverseProxy
}

// NewRouterProxy creates a new instance of the RouterProxy.
func NewRouterProxy(host, origin string) (*RouterProxy, error) {
	hostURL, err := url.Parse(host)
	if err != nil {
		return nil, err
	}

	originURL, err := url.Parse(origin)
	if err != nil {
		return nil, err
	}

	return &RouterProxy{
		host:   hostURL,
		origin: originURL,
		proxy:  httputil.NewSingleHostReverseProxy(originURL),
	}, nil
}

func (router *RouterProxy) Run() {
	log.Println("Starting router proxy")
	log.Printf("Directing to %s", router.origin)
	log.Printf("Listening on %s", router.host)

	http.HandleFunc("/", router.handle)
	http.ListenAndServe(router.host.Host, nil)
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

	ct := aw.Header().Get("Content-Type")
	if strings.HasPrefix(ct, "text/html") {
		bodyString, err := aw.BodyString()
		if err != nil {
			e := fmt.Errorf("Enable to parse response body with error: %s", err.Error())
			log.Panic(e)
		}

		updatedBodyString := strings.Replace(bodyString, router.origin.Host, router.host.Host, -1)
		b := bytes.NewBufferString(updatedBodyString)

		io.Copy(w, b)
	} else {
		aw.Copy(w)
	}
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
