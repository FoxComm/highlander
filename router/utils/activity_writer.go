package utils

import (
	"bytes"
	"compress/gzip"
	"io"
	"io/ioutil"
	"net/http"
	"strconv"
)

// ActivityWriter implements the http.ResponseWriter interface and is used to
// record the response from the origin while also writing back to the client.
type ActivityWriter struct {
	body       *bytes.Buffer
	header     http.Header
	statusCode int
	writer     io.WriteCloser
}

// NewActivityWriter creates a new empty ActivityWriter.
func NewActivityWriter() *ActivityWriter {
	body := &bytes.Buffer{}
	writer := NewNoopWriteCloser(body)

	return &ActivityWriter{
		body:   body,
		header: make(http.Header),
		writer: writer,
	}
}

// BodyString writes the content of the HTTP response body to a string. If the
// content is encoded with gzip it will convert it to a plain text string.
// Otherwise, the raw string is returned.
func (a *ActivityWriter) BodyString() (string, error) {
	// Check to see if the response is compressed.
	switch a.header.Get("Content-Encoding") {
	case "gzip":
		reader, err := gzip.NewReader(bytes.NewBuffer(a.body.Bytes()))
		if err != nil {
			return "", err
		}

		defer reader.Close()
		bs, err := ioutil.ReadAll(reader)
		if err != nil {
			return "", err
		}

		return string(bs), nil
	default:
		return a.body.String(), nil
	}
}

// StatusCode returns the status code of the HTTP response.
func (a *ActivityWriter) StatusCode() int {
	return a.statusCode
}

// Copy writes the results of this writer back to the default http.ResponseWriter.
func (a *ActivityWriter) Copy(rw http.ResponseWriter) error {
	// Copy headers
	for k, v := range a.header {
		rw.Header()[k] = append(rw.Header()[k], v...)
	}

	rw.Header().Set("Content-Length", strconv.Itoa(a.body.Len()))
	rw.WriteHeader(a.statusCode)

	_, err := io.Copy(rw, a.body)
	return err
}

func (a *ActivityWriter) Header() http.Header {
	return a.header
}

func (a *ActivityWriter) Write(buf []byte) (int, error) {
	if a.statusCode == 0 {
		a.WriteHeader(http.StatusOK)
	}

	return a.writer.Write(buf)
}

func (a *ActivityWriter) WriteHeader(code int) {
	a.statusCode = code
}
