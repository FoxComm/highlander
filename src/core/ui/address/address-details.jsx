import React from 'react';
import styles from './address.css';

import PhoneNumber from 'ui/forms/phone-number';
import CountryInfo from './country-info';

const AddressDetails = props => {
  const address = props.address;

  let countryInfo = null;

  if (address.region) {
    countryInfo = (
      <li>
        <CountryInfo display={country => country.name} countryId={address.region.countryId} />
      </li>
    );
  }

  return (
    <ul styleName="address-details">
      {address.name && <li>{address.name}</li>}
      <li>{address.address1}</li>
      {address.address2 && <li>{address.address2}</li>}
      <li>
        {address.city}, <span>{address.region && address.region.name}</span> <span>{address.zip}</span>
      </li>
      {countryInfo}
      {address.phoneNumber && <li><PhoneNumber>{address.phoneNumber}</PhoneNumber></li>}
    </ul>
  );
};

export default AddressDetails;
