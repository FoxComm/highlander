package responses

type ProductFunnelResponse struct {
	SearchViews       int
	PdpViews          int
	CartClicks        int
	CheckoutClicks    int
	Purchases         int
	SearchToPdp       float64
	PdpToCart         float64
	CartToCheckout    float64
	CheckoutPurchased float64
}
