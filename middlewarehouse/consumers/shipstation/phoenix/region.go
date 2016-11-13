package phoenix

type Region struct {
	ID        int    `json:"id" binding:"required"`
	Name      string `json:"name" binding:"required"`
	CountryID int    `json:"countryId" binding:"required"`
}
