package responses

type ProductStatsResponse struct {
	TotalRevenue                 int
	TotalOrders                  int
	TotalPdPViews                int
	TotalInCarts                 int
	ProductConversionRate        float64
	AverageTotalRevenue          float64
	AverageTotalOrders           float64
	AveragePdPViews              float64
	AverageTotalInCarts          float64
	AverageProductConversionRate float64
	ActiveProducts               int
}
