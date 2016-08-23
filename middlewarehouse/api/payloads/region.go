package payloads

type Region struct {
	ID          uint   `json:"id"`
	Name        string `json:"name" binding:"required"`
	CountryID   uint   `json:"countryId" binding:"required"`
	CountryName string `json:"countryName"`
}
