# Suggester
Takes in customer information and generates a list of possible products for a potential upsell to be consumed by external services
such as Email or SMS.

## usage
Add this directory to your `GOPATH`. 

```export GOPATH=$GOPATH:`pwd` ```

Copy the contents of `.env.sample` into another file with the correct url and port of the service, and source this new file.
Run the server.

```go run src/server.go```

| Directory                              | Description                                                                                                  |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [src](src)                             | All source files used by the suggester-service  | 

