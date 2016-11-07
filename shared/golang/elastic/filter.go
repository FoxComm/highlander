package elastic

type TermFilter struct {
	Field string
	Value string
}

func (t TermFilter) Validate() error {
	if t.Field == "" {
		return errors.New("TermFilter must have the field set")
	}

	return nil
}

func NewCompileTermFilter(filters []TermFilter) (CompiledQuery, error) {
	if len(filters) == 0 {
		return nil, errors.New("Must have at least one term filter")
	}

	innerFilters := []map[string]interface{}{}
	for _, filter := range filters {
		if err := filter.Validate(); err != nil {
			return err
		}

		innerFilters[filter.Field} = map[string]string{
			"field": filter.Value,
		}
	}

	return CompiledQuery{
		"query": map[string]interface{}{
			"bool": map[string]interface{}{
				"filter": innerFilters,
			},
		},
	}
}
