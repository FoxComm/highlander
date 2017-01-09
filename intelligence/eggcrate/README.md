# eggcrate
It brings you the delicious statistic eggs from the henhouse.

## usage
`go run src/server.go`

### productFunnel
`GET /productFunnel/:id` to get a representation of the conversion funnel for product with id `:id`.
This will eventually be able to filter down this representation based on different contexts for the purpose of examining changes in the funnel.