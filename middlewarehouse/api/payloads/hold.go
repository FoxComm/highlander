package payloads

// Hold is the payload sent by Phoenix when requesting that items are held for
// a specific order. It accepts an order reference number, and an array of each
// individual order line item with its associate SKU.
type Hold struct {
	OrderRefNum string     `json:"refNum" binding:"required"`
	Items       []itemHold `json:"items"`
	Scopable
}

type itemHold struct {
	SkuID          uint   `json:"skuId" binding:"required"`
	LineItemRefNum string `json:"lineItemRefNum" binding:"required"`
}
