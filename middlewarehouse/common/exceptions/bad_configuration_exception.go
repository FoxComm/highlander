package exceptions

type BadConfigurationException struct {
	Type string `json:"type"`
	Exception
}

func (exception BadConfigurationException) ToJSON() interface{} {
	return exception
}

func NewBadConfigurationException(error error) IException {
	if error == nil {
		return nil
	}

	return BadConfigurationException{
		Type:      "badConfiguration",
		Exception: Exception{error.Error()},
	}
}
