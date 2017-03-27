# Anthill Recommenders

## KNN purchase recommender
Generates a product similarity matrix for products based on "customers who
purchased this also purchased that".

### API
#### Training
When customer `1` buys products `4, 5, 6` over channel `1`.

*TODO: Make this endpoint private*

> `POST /api/v1/public/recommend/prod-prod/train`

body
```
{
  "points": [
  {
    "custID": 1,
    "prodID": 4,
    "chanID": 1
  },
  {
    "custID": 1,
    "prodID": 5,
    "chanID": 1
  },
  {
    "custID": 1,
    "prodID": 6,
    "chanID": 1
  }
  ]
}
```

### Getting a recommendation
See what products are similar to product 4

> `GET /api/v1/public/recommend/prod-prod/4?channel=1`

Response:
```
{
  "products": [
    {
      "id": 6,
      "score": 1
    },
    {
      "id": 5,
      "score": 1
    },
    {
      "id": 3,
      "score": 0
    },
    {
      "id": 2,
      "score": 0
    },
    {
      "id": 1,
      "score": 0
    },
    {
      "id": 0,
      "score": 0
    }
  ]
}
```

See what products are recommended for customer with id 5

> `GET /api/v1/public/recommend/cust-prod/5?channel=1`

To get replace the product ids in the previous response with full products from elasticsearch, use the endpoint

> `GET /api/v1/public/recommend/prod-prod/full/4?channel=1`
> `GET /api/v1/public/recommend/cust-prod/full/5?channel=1`
