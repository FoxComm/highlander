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
    "email": _.toLower(customer.EmailAddress),
    "name": getName(customer.BillingAddress),
    "password": "password",
    "isGuest": false
  };
}

function getUSACode() {
  console.log('Fetching countries');
  return Api.get('/public/countries').then(data => {
    const usa = _.find(data, { alpha2: 'US' });
    return usa.id;
  });
}

function getStates(countryId) {
  console.log('Fetching states');
  const states = require('./data/states.json');
  const namesAbbrs = _.map(states, state => ({ name: _.toLower(state.name), abbr: state.abbreviation }));
  return Api.get(`/public/countries/${countryId}`).then(data => {
    const abbrIds = _.map(data.regions, item => {
      const abbrItem = _.find(namesAbbrs, { name: _.toLower(item.name) });
      return { abbreviation: abbrItem.abbr, id: item.id };
    });
    return abbrIds;
  });
}

function getAddress(address, countryId, stateMap) {
  const region = _.find(stateMap, { abbreviation: address.StateProvince });
  const addressPayload = {
    "name": getName(address),
    "countryId": countryId,
    "regionId": region.id,
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
    const customers = JSON.parse(data);

    let countryId = null;
    getUSACode().then(usaId => {
        countryId = usaId;
        return getStates(usaId);
      }).then(statesIdMap => {
        customers.map((customer) => {

          const customerInfo = getCustomerInfo(customer);
          const shippingAddress = getAddress(customer.ShippingAddress, countryId, statesIdMap);
          const billingAddress = getAddress(customer.BillingAddress, countryId, statesIdMap);

          Api.post('/migration/customers/new', customerInfo)
            .then(
              data => {
                console.log(data);
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
                console.log(`${status}: ${text} ${_.toLower(customer.EmailAddress)}`);
              }
            );


        });
    });
  });
}

save();
