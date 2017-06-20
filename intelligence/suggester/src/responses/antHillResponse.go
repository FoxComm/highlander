package responses

type Image struct {
	Alt     string `json:"alt"`
	BaseUrl string `json:"baseurl"`
	Title   string `json:"title"`
	Src     string `json:"src"`
}

type Album struct {
	Name   string  `json:"name"`
	Images []Image `json:"images"`
}

type ProductInstance struct {
	ElasticSearchId int      `json:"id"`
	ProductId       int      `json:"productId"`
	Slug            string   `json:"slug"`
	Context         string   `json:"context"`
	Currency        string   `json:"currency"`
	Title           string   `json:"title"`
	Description     string   `json:"description"`
	SalePrice       string   `json:"salePrice"`
	Scope           string   `json:"scope"`
	RetailPrice     string   `json:"retailPrice"`
	Tags            []string `json:"tags"`
	Skus            []string `json:"skus"`
	Albums          []Album  `json:"albums"`
}

type ProductData struct {
	Product ProductInstance `json:"product"`
	Score   float64         `json:"score"`
}

type AntHillResponse struct {
	Products []ProductData `json:"products"`
}
