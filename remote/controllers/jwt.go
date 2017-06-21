package controllers

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
)

// JWT is a simplified JWT wrapper for our usage. Since we rely on Isaac for
// validation, we just get customer and scope information.
type JWT struct {
	Header  map[string]interface{}
	Payload map[string]interface{}
}

// NewJWT parses the JWT from a string.
func NewJWT(jwtStr string) (*JWT, error) {
	parts := strings.Split(jwtStr, ".")
	if len(parts) != 3 {
		return nil, errors.New("JWT is malformed")
	}

	headerBytes, err := base64.URLEncoding.DecodeString(parts[0])
	if err != nil {
		return nil, fmt.Errorf("Error decoding header with error: %s", err)
	}

	header := map[string]interface{}{}
	if err := json.Unmarshal(headerBytes, &header); err != nil {
		return nil, fmt.Errorf("Error marshalling header with error: %s", err)
	}

	payloadBytes, err := base64.URLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, fmt.Errorf("Error decoding payload with error: %s", err)
	}

	payload := map[string]interface{}{}
	if err := json.Unmarshal(payloadBytes, &payload); err != nil {
		return nil, fmt.Errorf("Error marshalling payload with error: %s", err)
	}

	return &JWT{Header: header, Payload: payload}, nil
}

// Scope gets the scope string passed in the JWT.
func (j JWT) Scope() string {
	scope, ok := j.Payload["Scope"]
	if !ok {
		return ""
	}

	return scope.(string)
}
