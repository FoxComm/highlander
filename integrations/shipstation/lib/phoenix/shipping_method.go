package phoenix

type ShippingMethod struct {
	ID                    int    `json:"id"`
	ShippingMethodID      int    `json:"shipping_method_id"`
	AdminDisplayName      string `json:"admin_display_name"`
	StorefrontDisplayName string `json:"storefront_display_name"`
	Price                 int    `json:"price"`
}
