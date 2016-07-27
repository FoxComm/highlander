package responses

type Region struct {
	ID          uint   `json:"id"`
	Name        string `json:"name"`
	CountryID   uint   `json:"countryId"`
	CountryName string `json:"countryName"`
}
