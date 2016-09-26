package elastic

type ElasticProduct struct {
	Result []product `json:"result"`
}

type product struct {
	ProductID int `json:"productId"`
}
