const fs = require('fs');
const Api = require('./api');
const moment = require('moment');
const _ = require('lodash');

/* TODO:
   - we need external IDs
   - enabled
   - meta information
   - images ?
   - should we add and archive all old products/ old skus ?
*/

function getPrice(price) {
  return {
    "currency": "USD",
    "value": parseFloat(price.replace(/\D/g, '')),
  };
}

function getTags(tags) {
  return tags.toUpperCase().split(', ');
}

function addCustomFields(product, productPayload) {
  const attributes = _.omit(product, [
    "Name",
    "Description",
    "Tags",
    "SKU code",
    "SKU price",
    "SKU Sale Price (optional)",
    "Link on current site",
    "id"
  ]);

  _.map(attributes, (value, key) => {
    productPayload.attributes[key] = {
      "t": "string",
      "v": value
    };
  });
}

function enableProduct(enabled, product) {
  if (enabled) {
    const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

    product.attributes.activeFrom = {
      t: "datetime",
      v: now,
    };
  }
}

function getProduct(product) {
  const price = getPrice(product["SKU price"]);

  const sale = product["SKU Sale Price (optional)"];
  const salePrice = !!sale ? getPrice(sale) : price;

  const title = {
    "t": "string",
    "v": product.Name
  };

  const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

  const sku = _.get(product, "SKU code");

  let productPayload = {
    "attributes": {
      title,
      "description": {
        "t": "richText",
        "v": product.Description
      },
      "tags": {
        "t": "tags",
        "v": getTags(product.Tags)
      }
    },
    "skus": [{
      "attributes": {
        title,
        "code": {
          "t": "string",
          "v": sku || `SKU-${product.id}`
        },
        "retailPrice": {
          "t": "price",
          "v": price,
        },
        "salePrice": {
          "t": "price",
          "v": salePrice,
        },
        "activeFrom": {
          "t": "datetime",
          "v": now,
        },
      }
    }],
    "context": {"name": "default"}
  };

  enableProduct(true, productPayload);
  addCustomFields(product, productPayload);

  return productPayload;
}

function save() {
  fs.readFile(__dirname + '/data/products_new.json', function (err, data) {
    // const products = JSON.parse(data).slice(8, 9);
    const products = JSON.parse(data);

    products.map((product) => {
      const productPayload = getProduct(product);

      // console.log(productPayload.skus[0].attributes.salePrice.v);

      if (product.productId) {
        Api.patch(`/products/default/${product.productId}`, productPayload)
          .then(
            data => {
              console.log(`${data.id} ${data.attributes.title.v}`);
            },
            err => {
              console.log(err);
              //console.log(`${product.Name} ${err.response.error.status}: ${err.response.error.text}`)
            }
          );
      } else {
        Api.post('/products/default', productPayload)
          .then(
            data => {
              console.log(`${data.id} ${data.attributes.title.v}`);
            },
            err => {
              console.log(err);
              //console.log(`${product.Name} ${err.response.error.status}: ${err.response.error.text}`)
            }
          );
      }

    });
  });
}

save();