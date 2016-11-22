package exceptions

type ValidationException struct {
	cls string `json:"type"`
	Exception
}

func NewValidationException(error error) IException {
	if error == nil {
		return nil
	}

	return NotImplementedException{
		cls:       "validation",
		Exception: Exception{error},
	}
}
