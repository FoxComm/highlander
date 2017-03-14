package api

type Image struct {
	AltText string `json:"alt"`
	BaseURL string `json:"baseUrl"`
	Source  string `json:"src"`
	Title   string `json:"title"`
}
