package payloads

// Address is a payload for creating and updating addresses.
type Address struct {
	Name        string
	Company     *string
	Street1     string
	Street2     *string
	Street3     *string
	City        string
	State       string
	PostalCode  string
	Country     string
	Phone       *string
	Residential bool
}
