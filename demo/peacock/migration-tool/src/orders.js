const fs = require('fs');
const Api = require('./api');
const moment = require('moment');
const _ = require('lodash');

function createCart(customerId) {
  const data = {
    customerId
  };

  return Api.post('/orders', data)
    .then(
      data => {
        console.log(`Cart: ${data.referenceNumber}`);

        return data.referenceNumber;
      },
      err => console.log(`${err.response.error.status}: ${err.response.error.text}`)
    );
}

function addLineItems(refNumber, order) {

  const items = order.Invoice.LineItemList;

  const data = items.map(item => {
    return {
      "sku": item.PartNumber,
      "quantity": parseInt(item.QtySold),
    }
  });

  console.log(data);

  Api.post(`/orders/${refNumber}/line-items`, data)
    .then(
      data => {
        console.log(data);
        console.log(`Cart: ${data.referenceNumber}`);
        console.log(data.lineItems.skus);
      },
      err => console.log(`${err.response.error.status}: ${err.response.error.text}`)
    );
}

function save() {
  fs.readFile(__dirname + '/data/orders.json', function (err, data) {
    // const orders = JSON.parse(data).slice(0, 1);
    const orders = JSON.parse(data);

    orders.map((order) => {

      // createCart(1018)
      //   .then((response) => {
      //     addLineItems(response, order);
      //   });

      // addLineItems('BR11196', order);

      console.log(order.Shipping.Name);

      //createCart(order.Customer.EmailAddress);

      // Api.post('/products/default', productInfo)
      //   .then(
      //     data => {
      //       console.log(`${data.id} ${data.attributes.title.v}`);
      //     },
      //     err => console.log(`${err.response.error.status}: ${err.response.error.text}`)
      //   );

    });
  });
}

save();