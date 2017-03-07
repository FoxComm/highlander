# cross-sell
## KNN product-product recommender
Generates a product similarity matrix for products based on "customers who
purchased this also purchased that".

### API
#### Training
When customer `1` buys products `4, 5, 6` over channel `1`.

*TODO: Make this endpoint private*

`POST /api/v1/public/recommend/prod-prod/train`
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

`GET /api/v1/public/recommend/prod-prod/4`

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

### TODO:
- only send the top `k` suggestions
- create an activity consumer to train the recommender

### Dependencies
- python3
  - `brew install python3` or `sudo apt-get install python3`
- numpy, scipy, Flask, matplotlib
  - `pip3 install --upgrade pip`
  - `pip3 install numpy scipy Flask matplotlib`
