package elastic

type ElasticResult struct {
	Pagination elasticPagination `json:"pagination"`
}

type elasticPagination struct {
	Total int
}
