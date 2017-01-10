package payloads

import "errors"

// Hold is the payload sent by Phoenix when requesting that items are held for
// a specific order. It accepts an order reference number, and an array of each
// individual order line item with its associate SKU.
type Hold struct {
	OrderRefNum string         `json:"refNum" binding:"required"`
	Items       []LineItemHold `json:"items"`
	Scopable
}

// Validate ensures that the structure of the hold payload is valid.
func (h Hold) Validate() error {
	if len(h.Items) == 0 {
		return errors.New("Hold must have at least one line item")
	}

	return nil
}

// LineItemHold is the payload for requesting a hold for a single line item.
type LineItemHold struct {
	SkuID          uint   `json:"skuId" binding:"required"`
	LineItemRefNum string `json:"lineItemRefNum" binding:"required"`
}
