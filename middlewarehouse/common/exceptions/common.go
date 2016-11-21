package exceptions

type NotImplementedException struct {
	cls string `json:"type"`
	Exception
}

func NewNotImplementedException(error error) IException {
	if error == nil {
		return nil
	}

	return NotImplementedException{
		cls:       "notImplemented",
		Exception: Exception{error},
	}
}

type BadConfigurationException struct {
	cls string `json:"type"`
	Exception
}

func NewBadConfigurationException(error error) IException {
	if error == nil {
		return nil
	}

	return NotImplementedException{
		cls:       "badConfiguration",
		Exception: Exception{error},
	}
}
