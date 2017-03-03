const fs = require('fs');
const Api = require('./api');
const moment = require('moment');
const _ = require('lodash');

/* TODO:
   - meta information
   - images ?
*/

function now() {
  return moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');
}

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

function enableProduct(product, enabled = true) {
  if (enabled) {
    product.attributes.activeFrom = {
      t: "datetime",
      v: now(),
    };
  }
}

function getProduct(product) {
  const price = getPrice(product["Price"]);

  const sale = product["SKU Sale Price (optional)"];
  const salePrice = !!sale ? getPrice(sale) : price;

  const title = {
    "t": "string",
    "v": product.Name
  };

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
          "v": sku || `PG-SKU-${product.id}`
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
          "v": now(),
        },
      }
    }],
    "context": {"name": "default"}
  };

  enableProduct(productPayload);
  addCustomFields(product, productPayload);

  return productPayload;
}

function updateInventory(code) {
  Api.get(`inventory/summary/${code}`)
    .then(
      data => {
        const sellable = _.find(data.summary, 'type', 'Sellable');
        const {id} = sellable.stockItem;
        const sellableUpdate = {
          "qty": 1000,
          "type": "Sellable",
          "status": "onHand"
        };

        Api.patch(`inventory/stock-items/${id}/increment`, sellableUpdate)
          .then(
            () => {
              console.log(`Sellable items amount for ${code} is updated to 1000`);
            },
            err => {
              console.log(err);
            }
          );
      },
      err => {
        console.log(err);
      });
}

function save(productPayload, id) {
  if (id) {
    return Api.patch(`/products/default/${id}`, productPayload)
      .then(
        data => {
          console.log(`${data.id} ${data.attributes.title.v}`);
          return data;
        },
        err => {
          console.log(err);
          //console.log(`${product.Name} ${err.response.error.status}: ${err.response.error.text}`)
        }
      );
  } else {
    return Api.post('/products/default', productPayload)
      .then(
        data => {
          console.log(`${data.id} ${data.attributes.title.v}`);
          return data;
        },
        err => {
          console.log(err);
          //console.log(`${product.Name} ${err.response.error.status}: ${err.response.error.text}`)
        }
      );
  }
}

function saveProducts() {
  fs.readFile(__dirname + '/data/products_new.json', function (err, data) {
    // const products = JSON.parse(data).slice(4, 5);
    const products = JSON.parse(data);

    products.map((product, i) => {
      const productPayload = getProduct(product);

      // console.log(productPayload.skus[0].attributes.salePrice.v);
      // console.log(productPayload.skus[0].attributes.code.v);

      const id = _.get(product, 'productId');

      save(productPayload, id).then(
        (data) => {
          const code = data.skus[0].attributes.code.v;
          products[i].productId = data.id;

          if (i == products.length - 1) {
            fs.writeFile(__dirname + '/data/products_new.json', JSON.stringify(products, null, 2), function(err) {
              if(err) {
                return console.log(err);
              }

              console.log("Products IDs were added to json, you can update them now!");
            });
          }

          setTimeout(() => {
            updateInventory(code);
          }, 10000);
        }
      );
    });
  });
}

function saveGiftCard() {
  fs.readFile(__dirname + '/data/gift_card.json', function (err, data) {
    const giftCard = JSON.parse(data);

    enableProduct(giftCard);

    giftCard.skus.map((sku, i) => {
      enableProduct(giftCard.skus[i]);
    });

    save(giftCard).then(
      (data) => {
        setTimeout(() => {
          _.map(data.skus, sku => {
            const code = sku.attributes.code.v;
            updateInventory(code);
          });
        }, 10000);
      }
    );
  });
}

saveGiftCard();
saveProducts();
