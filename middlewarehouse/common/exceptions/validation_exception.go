package exceptions

type ValidationException struct {
	Type string `json:"type"`
	Exception
}

func (exception ValidationException) ToJSON() interface{} {
	return exception
}

func NewValidationException(error error) IException {
	if error == nil {
		return nil
	}

	return NotImplementedException{
		Type:      "validation",
		Exception: Exception{error.Error()},
	}
}
