package exceptions

type NotImplementedException struct {
	Type string `json:"type"`
	Exception
}

func (exception NotImplementedException) ToJSON() interface{} {
	return exception
}

func NewNotImplementedException(error error) IException {
	if error == nil {
		return nil
	}

	return NotImplementedException{
		Type:      "notImplemented",
		Exception: Exception{error.Error()},
	}
}
