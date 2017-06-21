package gohttp

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"

    "github.com/FoxComm/highlander/middlewarehouse/common/utils"
)

func Get(url string, headers map[string]string, retval interface{}) error {
    _, err := Request("GET", url, headers, nil, retval)

    return err
}

func Post(url string, headers map[string]string, payload interface{}, retval interface{}) error {
    _, err := Request("POST", url, headers, payload, retval)

    return err
}

func Patch(url string, headers map[string]string, payload interface{}, retval interface{}) error {
    _, err := Request("PATCH", url, headers, payload, retval)

    return err
}

func Put(url string, headers map[string]string, payload interface{}, retval interface{}) error {
    _, err := Request("PUT", url, headers, payload, retval)

    return err
}

func Request(method string, url string, headers map[string]string, payload interface{}, retval interface{}) (*http.Response, error) {
    debug := utils.IsDebug()

    if method != "POST" && method != "PATCH" && method != "GET" && method != "PUT" {
        return nil, fmt.Errorf("Invalid method %s. Only GET, POST, PUT, and PATCH are currently supported", method)
    }

    if headers == nil {
        headers = map[string]string{}
    }

    var req *http.Request
    var err error
    payloadBytes := []byte{}

    if method == "GET" {
        req, err = http.NewRequest(method, url, nil)
    } else {
        payloadBytes, err = json.Marshal(&payload)
        if err != nil {
            return nil, fmt.Errorf("Unable to marshal payload: %s", err.Error())
        }

        req, err = http.NewRequest(method, url, bytes.NewBuffer(payloadBytes))
    }

    log.Printf("HTTP --> %s %s %s", method, url, utils.SanitizePassword(payloadBytes))

    if err != nil {
        return nil, fmt.Errorf("Unable to create %s request: %s", method, err.Error())
    }

    req.Header.Set("Content-Type", "application/json")
    for k, v := range headers {
        req.Header.Set(k, v)
    }

    client := &http.Client{}
    resp, err := client.Do(req)
    if err != nil {
        return nil, fmt.Errorf("Unable to make %s request: %s", method, err.Error())
    }

    log.Printf("HTTP <-- %s %s %s", method, url, resp.Status)

    defer resp.Body.Close()
    body, err := ioutil.ReadAll(resp.Body)

    if err != nil {
        return nil, fmt.Errorf("Unable to read body of the response: %s", err.Error())
    }

    if resp.StatusCode < 200 || resp.StatusCode > 299 {
        return nil, fmt.Errorf(
            "Error in %s response - status: %s, body %s",
            method,
            resp.Status,
            string(body),
        )
    }

    if retval != nil {
        err = json.Unmarshal(body, retval)
    }

    if err != nil && debug {
        err = fmt.Errorf(
            "Failed to unmarshall body: %s \nOriginal message: %s",
            string(body),
            err.Error(),
        )
    }

    return resp, err
}
