package api

type Album struct {
	Name   string  `json:"name"`
	Images []Image `json:"images"`
}
