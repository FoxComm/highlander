package api

import "fmt"

type ObjectAttribute struct {
	Type  string      `json:"t"`
	Value interface{} `json:"v"`
}

type ObjectAttributes map[string]ObjectAttribute

func (o ObjectAttributes) LookupValue(name string) (interface{}, error) {
	attr, ok := o[name]
	if !ok {
		return "", fmt.Errorf("Variant %s not found", name)
	}

	return attr.Value, nil
}
