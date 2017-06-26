# Suggester

Takes in customer information and generates a list of possible products for a potential upsell to be consumed by external services
such as Email or SMS.

## Usage
Add this directory to your `GOPATH`:

    $ export GOPATH=$GOPATH:`pwd`

Copy the contents of `.env.sample` into another file with the correct url and port of the service, and source this new file.
Run the server:

    $ go run src/server.go

## API

| Action      | Endpoint                                                              | Payload                                           |
|-------------|-----------------------------------------------------------------------|---------------------------------------------------|
| GET         | /api/v1/public/suggest/ping                                           |                                                   |
| POST        | /api/v1/public/suggest/customer?channel=[integer]                     | {"customerID": "string", "phoneNumber": "string"} |
| POST        | /api/v1/public/suggest/customer/[international-phone-number]/decline  |                                                   |
| POST        | /api/v1/public/suggest/customer/[international-phone-number]/purchase |                                                   |

## Structure
| Directory                              | Description                                                                                                  |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| [src](src)                             | All source files used by the suggester-service  |

