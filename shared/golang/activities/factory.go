package activities

import "github.com/FoxComm/metamorphosis"

const ProductCreated = "full_product_created"
const ProductUpdated = "full_product_updated"
const SKUCreated = "full_sku_created"
const SKUUpdated = "full_sku_updated"

func CreateActivity(m metamorphosis.AvroMessage) (SiteActivity, error) {
	plainActivity, err := NewActivityFromAvro(m)
	if err != nil {
		return nil, err
	}

	switch plainActivity.Type() {
	case ProductCreated:
	case ProductUpdated:
		return NewFullProduct(plainActivity)
	case SKUCreated:
	case SKUUpdated:
		return NewFullSKU(plainActivity)
	}

	return nil, nil
}
