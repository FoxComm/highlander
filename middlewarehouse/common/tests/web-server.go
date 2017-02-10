package tests

import (
	"bytes"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"

	"github.com/SermoDigital/jose/crypto"
	"github.com/SermoDigital/jose/jws"
	"github.com/gin-gonic/gin"
)

// TestWebServer is a test webserver and wrappers for making HTTP calls that
// surrounds a gin router.
type TestWebServer struct {
	Router *gin.Engine
}

// Get issues an HTTP GET request and records the response in the supplied args.
func (ws *TestWebServer) Get(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("GET", url, nil)

	return ws.query(request, args...)
}

// Post issues an HTTP POST request and records the response in the supplied args.
func (ws *TestWebServer) Post(url string, body interface{}, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("POST", url, prepareBody(body))

	return ws.query(request, args...)
}

// Put issues an HTTP PUT request and records the response in the supplied args.
func (ws *TestWebServer) Put(url string, body interface{}, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("PUT", url, prepareBody(body))

	return ws.query(request, args...)
}

// Patch issues an HTTP PATCH request and records the response in the supplied args.
func (ws *TestWebServer) Patch(url string, body interface{}, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("PATCH", url, prepareBody(body))

	return ws.query(request, args...)
}

// Delete issues an HTTP DELETE request and records the response in the supplied args.
func (ws *TestWebServer) Delete(url string, args ...interface{}) *httptest.ResponseRecorder {
	request, _ := http.NewRequest("DELETE", url, nil)

	return ws.query(request, args...)
}

func (ws *TestWebServer) query(request *http.Request, target ...interface{}) *httptest.ResponseRecorder {
	rawClaims := make(map[string]interface{})
	rawClaims["scope"] = "1"
	jwt := jws.NewJWT(jws.Claims(rawClaims), crypto.SigningMethodHS256)
	serializedJWT, _ := jwt.Serialize([]byte("key"))
	request.Header.Set("JWT", string(serializedJWT))

	//record response
	response := httptest.NewRecorder()

	//serve request with router, writing to response
	ws.Router.ServeHTTP(response, request)

	//return raw response or parse, if needed
	switch len(target) {
	case 0:
		return response
	case 1:
		return parseBody(response, target[0])
	default:
		panic("Unexpected number of arguments")
	}
}

func prepareBody(raw interface{}) io.Reader {
	switch raw.(type) {
	case string:
		return strings.NewReader(raw.(string))
	default:
		buffer := new(bytes.Buffer)
		json.NewEncoder(buffer).Encode(raw)

		return buffer
	}
}

func parseBody(response *httptest.ResponseRecorder, target interface{}) *httptest.ResponseRecorder {
	//decode if any data
	if response.Body.Len() != 0 {
		if err := json.NewDecoder(response.Body).Decode(target); err != nil {
			log.Panicf(`Cannot decode "%v" into "%v".\n%v`, response.Body, target, err)
		}
	}

	return response
}
