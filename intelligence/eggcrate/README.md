# eggcrate
It brings you the delicious statistic eggs from the henhouse.

## usage
Add this directory to your `GOPATH`. 

```export GOPATH=$GOPATH:`pwd` ```

Copy the contents of `.env.sample` into another file with the correct url and port of the `henhouse` service, and source this new file.
Run the server.

```go run src/server.go```

### productFunnel
`GET /productFunnel/:id` to get a representation of the conversion funnel for product with id `:id`.
This will eventually be able to filter down this representation based on different contexts for the purpose of examining changes in the funnel.