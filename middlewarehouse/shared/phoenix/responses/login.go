package responses

type LoginResponse struct {
	ID         int    `json:"id"`
	Email      string `json:"email"`
	Ratchet    int    `json:"ratchet"`
	Name       string `json:"name"`
	Expiration int64  `json:"exp"`
	Issuer     string `json:"iss"`
	IsAdmin    bool   `json:"admin"`
}
