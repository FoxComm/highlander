
import _ from 'lodash';
import { createAsyncActions } from 'wings';
import { loadCountry, usaDetails } from 'modules/countries';

const addressFields = [
  'name',
  'address1',
  'address2',
  'city',
  'zip',
  'phoneNumber',
  'isDefault',
];

const _initAddressData = createAsyncActions(
  'initAddressData',
  function(address) {
    let uiAddressData;

    const isAddressValid = !_.isEmpty(address) && address.region;

    return new Promise(resolve => {
      if (!isAddressValid) {
        uiAddressData = _.reduce(addressFields, (acc, field) => {
          return {
            [field]: field == 'isDefault' ? false : '',
            ...acc,
          };
        }, {});
        uiAddressData.country = usaDetails;
        uiAddressData.state = usaDetails.regions[0];
        resolve(uiAddressData);
      } else {
        this.dispatch(loadCountry(address.region.countryId)).then(() => {
          const countryInfo = this.getState().countries.details[address.region.countryId];

          uiAddressData = _.pick(address, addressFields);
          uiAddressData.country = countryInfo;
          uiAddressData.state = _.find(countryInfo.regions, { id: address.region.id });

          resolve(uiAddressData);
        });
      }
    });
  }
);

export const initAddressData = _initAddressData.perform;
