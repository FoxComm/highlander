package payloads

// Hold is the payload sent by Phoenix when requesting that items are held for
// a specific order.
type Hold struct {
	OrderRefNum string     `json:"refNum" binding:"required"`
	Items       []itemHold `json:"items"`
	Scopable
}

type itemHold struct {
	SkuID          uint   `json:"skuId" binding:"required"`
	LineItemRefNum string `json:"lineItemRefNum" binding:"required"`
}
