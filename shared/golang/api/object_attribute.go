package api

import "fmt"

type ObjectAttribute struct {
	Type  string      `json:"t"`
	Value interface{} `json:"v"`
}

type ObjectAttributes map[string]ObjectAttribute

func (o ObjectAttributes) LookupValue(name, expectedType string) (interface{}, error) {
	attr, ok := o[name]
	if !ok {
		return "", fmt.Errorf("Variant %s not found", name)
	}

	if attr.Type != expectedType {
		return "", fmt.Errorf("Expected %s type for variant value, got %s", expectedType, attr.Type)
	}

	return attr.Value, nil
}
