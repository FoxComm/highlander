package phoenix

// Address is a representation of how addresses (both billing and shipping) are
// stored in Phoenix.
type Address struct {
	Zip       string
	City      string
	Region    string
	Country   string
	Address1  string
	Address2  string
	Currency  string
	Continent string
}
