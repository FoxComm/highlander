package elastic

type Hits struct {
	Total    int   `json:"total"`
	MaxScore int   `json:"max_score"`
	Hits     []Hit `json:"hits"`
}
