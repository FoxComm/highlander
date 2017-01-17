package responses

type ProductFunnelResponse struct {
	SearchViews       int
	PdpViews          int
	CartClicks        int
	CheckoutClicks    int
	Purchases         int
	SearchToPdp       float32
	PdpToCart         float32
	CartToCheckout    float32
	CheckoutPurchased float32
}
