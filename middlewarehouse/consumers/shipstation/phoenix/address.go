package phoenix

// Address is a representation of how addresses (both billing and shipping) are
// stored in Phoenix.
type Address struct {
	ID       int     `json:"id" binding:"required"`
	Name     string  `json:"name" binding:"required"`
	Zip      string  `json:"zip" binding:"required"`
	City     string  `json:"city" binding:"required"`
	Region   Region  `json:"region" binding:"required"`
	Address1 string  `json:"address1" binding:"required"`
	Address2 *string `json:"address2"`
}
