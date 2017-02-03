package responses

type AverageProductStatsResponse struct {
	TotalRevenue          float64
	TotalOrders           float64
	TotalPdPViews         float64
	TotalInCarts          float64
	ProductConversionRate float64
}

type ProductStatsResponse struct {
	TotalRevenue          int
	TotalOrders           int
	TotalPdPViews         int
	TotalInCarts          int
	ProductConversionRate float64
	Average               *AverageProductStatsResponse
	ActiveProducts        int
}
