package elastic

import "errors"

type TermFilter struct {
	Field string
	Value interface{}
}

func (t TermFilter) Validate() error {
	if t.Field == "" {
		return errors.New("TermFilter must have the field set")
	}

	return nil
}

func NewCompiledTermFilter(filters []TermFilter) (CompiledQuery, error) {
	if len(filters) == 0 {
		return nil, errors.New("Must have at least one term filter")
	}

	innerFilters := []map[string]interface{}{}
	for _, filter := range filters {
		if err := filter.Validate(); err != nil {
			return nil, err
		}

		innerFilter := map[string]interface{}{
			"term": map[string]interface{}{
				filter.Field: filter.Value,
			},
		}

		innerFilters = append(innerFilters, innerFilter)
	}

	return CompiledQuery{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"filter": innerFilters,
			},
		},
	}, nil
}
