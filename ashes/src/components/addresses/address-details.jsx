import React, { PropTypes } from 'react';
import PhoneNumber from '../phone-number/phone-number';
import CountryInfo from './country-info';

const AddressDetails = props => {
  const address = props.address;

  let countryInfo = null;

  if (address.region) {
    countryInfo = (
      <li>
        <CountryInfo id="country" display={country => country.name} countryId={address.region.countryId} />
      </li>
    );
  }

  return (
    <ul className="fc-address-details">
      {address.name && <li className="name">{address.name}</li>}
      <li className="address1">{address.address1}</li>
      {address.address2 && <li className="address2">{address.address2}</li>}
      <li className="city">
        {address.city}, <span className="region">{address.region && address.region.name}</span> <span className="zip">{address.zip}</span>
      </li>
      {countryInfo}
      {address.phoneNumber && <li><PhoneNumber>{address.phoneNumber}</PhoneNumber></li>}
    </ul>
  );
};

AddressDetails.propTypes = {
  address: PropTypes.shape({
    region: PropTypes.shape({
      countryId: PropTypes.number
    }).isRequired
  }).isRequired
};

export default AddressDetails;
