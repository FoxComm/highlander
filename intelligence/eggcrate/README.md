# eggcrate
It brings you the delicious statistic eggs from the henhouse.

## usage
Add this directory to your `GOPATH`. 

```export GOPATH=$GOPATH:`pwd` ```

Copy the contents of `.env.sample` into another file with the correct url and port of the `henhouse` service, and source this new file.
Run the server.

```go run src/server.go```

### productFunnel
Use `GET /api/v1/stats/productFunnel/:id` to get a representation of the conversion funnel for product with id `:id`.
This will eventually be able to filter down this representation based on different contexts for the purpose of examining changes in the funnel.

### single product sums
Use `GET /api/v1/stats/productSum/<step>/:id` to get the count of a single event.
Current choices for `<step>` are
- list
- pdp
- cart
- checkout

### date ranges
To get restrict the date range use query params `to` and `from` with dates in unix time format.

```GET /api/v1/stats/productFunnel/1?from=1484315402&to=1484315462```