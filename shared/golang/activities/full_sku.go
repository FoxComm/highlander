package activities

import (
	"encoding/json"
	"fmt"

	"github.com/FoxComm/highlander/shared/golang/api"
)

// FullSKU is one of a multiple activities that contains the full product
// response in its payload. Currently, those are the "full_sku_created" and
// "full_sku_updated" activities.
type FullSKU interface {
	Type() string
	Data() string
	CreatedAt() string
	SKU() *api.SKU
}

type fullSKU struct {
	sku      *api.SKU
	activity SiteActivity
}

// NewFullSKU creates a FullProduct activity from an existing SiteActivity.
// Throws an error if unmashalling to the activity fails.
func NewFullSKU(activity SiteActivity) (FullSKU, error) {
	if activity.Type() != SKUCreated && activity.Type() != SKUUpdated {
		return nil, fmt.Errorf(
			"Expected activity %s or %s, but got %s",
			SKUCreated,
			SKUUpdated,
			activity.Type(),
		)
	}

	bytes := []byte(activity.Data())
	skuData := new(fullSKUData)

	if err := json.Unmarshal(bytes, skuData); err != nil {
		return nil, fmt.Errorf("Unable to unmarshal activity data %s", err.Error())
	}

	return &fullSKU{
		sku:      skuData.SKU,
		activity: activity,
	}, nil
}

func (f fullSKU) Type() string {
	return f.activity.Type()
}

func (f fullSKU) Data() string {
	return f.activity.Data()
}

func (f fullSKU) CreatedAt() string {
	return f.activity.CreatedAt()
}

func (f fullSKU) SKU() *api.SKU {
	return f.sku
}

type fullSKUData struct {
	Admin interface{} `json:"admin"`
	SKU   *api.SKU    `json:"sku"`
}
