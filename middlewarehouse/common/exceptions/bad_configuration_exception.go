package exceptions


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
