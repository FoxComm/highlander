package phoenix

type OrderTotals struct {
	Taxes       int `json:"taxes" binding:"required"`
	Total       int `json:"total" binding:"required"`
	Shipping    int `json:"shipping" binding:"required"`
	SubTotal    int `json:"subTotal" binding:"required"`
	Adjustments int `json:"adjustments" binding:"required"`
}
