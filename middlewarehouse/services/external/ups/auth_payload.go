package ups

// AuthPayload represents the payload used to authorize with the UPS API.
type AuthPayload struct {
	UsernameToken      usernameToken      `json:"UsernameToken"`
	ServiceAccessToken serviceAccessToken `json:"ServiceAccessToken"`
}

// NewAuthPayload creates a new AuthPayload.
func NewAuthPayload(username, password, licenseNumber string) *AuthPayload {
	return &AuthPayload{
		UsernameToken: usernameToken{
			Username: username,
			Password: password,
		},
		ServiceAccessToken: serviceAccessToken{
			AccessLicenseNumber: licenseNumber,
		},
	}
}

type usernameToken struct {
	Username string `json:"Username"`
	Password string `json:"Password"`
}

type serviceAccessToken struct {
	AccessLicenseNumber string `json:"AccessLicenseNumber"`
}
