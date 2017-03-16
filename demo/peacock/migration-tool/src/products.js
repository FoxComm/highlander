const fs = require('fs');
const Api = require('./api');
const moment = require('moment');
const _ = require('lodash');

function getPrice(price) {
  return parseInt(price.replace(/\D/g, ''));
}

function getCategoryName(id) {
  const data =  fs.readFileSync(__dirname + '/data/categories.json', 'utf8');
  const categories = JSON.parse(data);

  return categories[id].Name;
}

function getCategories(list) {
  let categories = [];

  if (_.isArray(list)) {
    categories = list.map((category) => {
      const id = category.$.CategoryId;
      return getCategoryName(id);
    });
  } else {
    const id = list.$.CategoryId;
    categories.push(getCategoryName(id));
  }

  return categories;
}

function setAttribute(newAttribute, attributes) {
  const name = newAttribute.Name;
  const value = newAttribute.AttributeValueList.Value;

  attributes[name] = {
    "t": "string",
    "v": value,
  }
}

function addCustomFields(attrList, product) {
  if (attrList) {
    if (_.isArray(attrList)) {
      attrList.map(attribute => {
        setAttribute(attribute, product.attributes)
      });
    } else {
      setAttribute(attrList, product.attributes)
    }
  }
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
  const price = {
    "currency": product.Price.BasePrice.$.Currency,
    "value": getPrice(product.Price.BasePrice._)
  };

  const title = {
    "t": "string",
    "v": product.Name
  };

  const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSS[Z]');

  let productInfo = {
    "attributes": {
      title,
      "metaDescription": {
        "t": "string",
        "v": _.get(product, ['SearchInformation', 'MetaDescription'], "")
      },
      "metaTitle": {
        "t": "string",
        "v": _.get(product, ['SearchInformation', 'PageTitle'], "")
      },
      "metaKeyword": {
        "t": "string",
        "v": _.get(product, ['SearchInformation', 'MetaKeyword'], "")
      },
      "description": {
        "t": "richText",
        "v": product.FullDescription
      },
      "shortDescription": {
        "t": "richText",
        "v": product.Description
      },
      "tags": {
        "t": "tags",
        "v": getCategories(product.CategoryList)
      }
    },
    "skus": [{
      "attributes": {
        "title": {
          "t": "string",
          "v": product.Name
        },
        "code": {
          "t": "string",
          "v": product.PartNumber
        },
        "retailPrice": {
          "t": "price",
          "v": price,
        },
        "salePrice": {
          "t": "price",
          "v": price
        },
        "activeFrom": {
          "t": "datetime",
          "v": now,
        },
      }
    }],
    "context": {"name": "default"}
  };

  enableProduct(product.Enabled, productInfo);
  addCustomFields(product.ProductAttributeList, productInfo);

  // console.log(`${product.PartNumber} ${product.Name}`);
  return productInfo;
}

function save() {
  fs.readFile(__dirname + '/data/products.json', function (err, data) {
    const products = JSON.parse(data).slice(0, (data.length)/2);
    // const products = JSON.parse(data);

    products.map((product) => {
      //TODO: gifts
      if (product.ProductClass === 'GiftCertificate') return;

      const productInfo = getProduct(product);

      console.log(productInfo);

      Api.post('/products/default', productInfo)
        .then(
          data => {
            console.log(`${data.id} ${data.attributes.title.v}`);
          },
          err => {
            console.log(err);
            console.log(`${product.$.ProductId} ${err.response.error.status}: ${err.response.error.text}`);
          }
        );

    });
  });
}

save();
