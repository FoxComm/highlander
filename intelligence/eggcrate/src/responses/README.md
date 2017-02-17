#Responses

Each response is specified as a struct. 

Example

`
    package responses

    type ProductSumResponse struct {
        Step string
        Sum  int
    }
`

This gets converted automatically into JSON that looks like this

`
    {
        "step": "blah blah",
        "sum" : 100
    }
`
