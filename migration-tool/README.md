# Migration Tool

## Set up

* Install node packages for `the-perfect-gourmet` repository: `cd .. && npm i && cd -`
* Install node packages for migration tool: `npm i`
* Set up environment:
** copy environment example file: `cp .env.example .env`
** get fresh admin key from Ashes
** fill in DEMO_AUTH_TOKEN and API_URL variables in `.env`
** load environment into shell: `source .env`

## Usage

* To insert or update product data run: `npm run products`
* To insert customers run: `npm run customers`

*N.B.*
* To update customers data you should have `productId`s set in `data/products_new.json`
* Customer data cannot be updated now
