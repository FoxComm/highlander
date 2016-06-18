package validation

type invalidErr struct {
	Invalids Invalids
}

func (e invalidErr) Error() string {
	return e.Invalids.String()
}
