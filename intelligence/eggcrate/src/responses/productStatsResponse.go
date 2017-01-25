package responses

type ProductStatsResponse struct {
	TotalRevenue                 int
	TotalOrders                  int
	AverageNumberPerOrder        float64
	TotalInCarts                 int
	ProductConversionRate        float64
	AverageTotalRevenue          float64
	AverageTotalOrders           float64
	AverageAverageNumberPerOrder float64
	AverageTotalInCarts          float64
	AverageProductConversionRate float64
}
