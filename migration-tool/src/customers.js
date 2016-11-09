const _ = require('lodash');
const fs = require('fs');
const Api = require('./api');

function getName(address) {
  return `${address.FirstName} ${address.LastName}`;
}

function getPhone(phone) {
  return phone.replace(/\D/g,'');
}

function getCustomerInfo(customer) {
  return {
    "email": customer.EmailAddress,
    "name": getName(customer.BillingAddress),
    "password": "password",
    "isGuest": false
  };
}

function getAddress(address) {
  const addressPayload = {
    "name": getName(address),
    // "countryId": 234,            //TODO: region!
    "regionId": 4156,            //TODO: region!
    "address1": address.Address1,
    "address2": address.Address2 || "",
    "city": address.City,
    "zip": address.PostalCode,
    "isDefault": false
  };

  const phone = getPhone(_.get(address, "Phone", ""));
  if (phone && phone.length == 10) addressPayload.phoneNumber = phone;

  return addressPayload;
}

function addAddress(id, address, shipping = false) {
  Api.post(`/customers/${id}/addresses`, address)
    .then(
      data => {
        console.log(`${data.id} Address is added`);

        if (shipping) {
          Api.post(`/customers/${id}/addresses/${data.id}/default`)
            .then(
              data => {
                console.log(`${data.id} is set Default`);
              },
              err => console.log(`${err.response.error.status}: ${err.response.error.text}`)
            );
        }
      },
      err => console.log(`${id}: ${err.response.error.status}: ${err.response.error.text}`)
    );
}

function save() {
  fs.readFile(__dirname + '/data/customers.json', function(err, data) {
    // const customers = JSON.parse(data).slice(0, 2);
    const customers = JSON.parse(data);

    customers.map((customer) => {

      const customerInfo = getCustomerInfo(customer);
      const shippingAddress = getAddress(customer.ShippingAddress);
      const billingAddress = getAddress(customer.BillingAddress);

      // console.log(shippingAddress);

      Api.post('/customers', customerInfo)
        .then(
          data => {
            console.log(`${data.id}: ${data.name} is created`);

            if (_.isEqual(shippingAddress, billingAddress)) {
              addAddress(data.id, shippingAddress, true);
            } else {
              addAddress(data.id, shippingAddress, true);
              addAddress(data.id, billingAddress);
            }
          },
          err => {
            const {status, text} = err.response.error;
            console.log(`${status}: ${text} ${customer.EmailAddress}`);

            // if (text.indexOf('The email address you entered is already in use"') != -1) {
            //   console.log('update customer by id');
            // }
          }
        );


    });
  });
}

save();