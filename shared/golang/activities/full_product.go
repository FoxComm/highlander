package activities

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/shared/golang/api"
)

// FullProduct is one of a multiple activities that contains the full product
// response in its payload. Currently, those are the "full_product_created" and
// "full_product_updated" activities.
type FullProduct interface {
	Type() string
	Data() string
	CreatedAt() string
	Product() *api.Product
}

type fullProduct struct {
	product  *api.Product
	activity SiteActivity
}

// NewFullProduct creates a FullProduct activity from an existing SiteActivity.
// Throws an error if unmashalling to the activity fails.
func NewFullProduct(activity SiteActivity) (FullProduct, error) {
	if activity.Type() != ProductCreated && activity.Type() != ProductUpdated {
		return nil, fmt.Errorf(
			"Expected activity %s or %s, but got %s",
			ProductCreated,
			ProductUpdated,
			activity.Type(),
		)
	}

	bytes := []byte(activity.Data())
	productData := new(fullProductData)

	if err := json.Unmarshal(bytes, productData); err != nil {
		return nil, fmt.Errorf("Unable to unmarshal activity data %s", err.Error())
	}

	return &fullProduct{
		product:  productData.Product,
		activity: activity,
	}, nil
}

func (f fullProduct) Type() string {
	return f.activity.Type()
}

func (f fullProduct) Data() string {
	return f.activity.Data()
}

func (f fullProduct) CreatedAt() string {
	return f.activity.CreatedAt()
}

func (f fullProduct) Product() *api.Product {
	return f.product
}

type fullProductData struct {
	Admin   interface{}  `json:"admin"`
	Product *api.Product `json:"product"`
}
