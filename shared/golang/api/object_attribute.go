package api

import (
    "fmt"
    "time"
)

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

func (o ObjectAttributes) LookupDateTime(name string) (*time.Time, error) {
    strVal, err := o.LookupValue(name)
    if err != nil {
        return &time.Time{}, err
    }
    if strVal == nil {
        return nil, nil
    }
    t, err := time.Parse(time.RFC3339, strVal.(string))
    return &t, err
}

