package services

import (
    "fmt"
    "os"
    "encoding/json"

    "context"

    elastic "gopkg.in/olivere/elastic.v3"
)

const (
    ElasticURLKey   = "ELASTIC_URL"
)

type ElasticService struct {
    esClient      *elastic.Client
}

type pagination struct {
    Total int64 `json:"total"`
}

type FormattedResponse struct {
    Results []*json.RawMessage `json:"result"`
    Pagination pagination `json:"pagination"`
}

func NewElasticService() (*ElasticService) {
    elasticURL := os.Getenv(ElasticURLKey)
    esClient, err := elastic.NewClient(elastic.SetURL(elasticURL))

    if err != nil {
        panic(fmt.Errorf("Unable to create ES client with error %s", err.Error()))
    }

    return &ElasticService{ esClient }
}

func (service *ElasticService) GetEsResponse(index string, view string, from int, size int, query json.RawMessage) (result FormattedResponse){
    ctx := context.Background()
    esQuery := elastic.RawStringQuery(query)
    res, err := service.esClient.
        Search().
        Index(index).
        Type(view).
        Query(esQuery).
        From(from).
        Size(size).
        Do(ctx)

    if err != nil {
        fmt.Println("Error: %s", err.Error())
        return
    }

    results := make([]*json.RawMessage, size)
    for i, hit := range res.Hits.Hits {
        results[i] = hit.Source
    }

    result = FormattedResponse{ results, pagination{ res.Hits.TotalHits } }
    return result
}
