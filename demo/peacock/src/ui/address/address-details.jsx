// @flow

import React from 'react';
import styles from './address.css';
import type { Address } from 'types/address';

import PhoneNumber from 'ui/forms/phone-number';
import CountryInfo from './country-info';

type Props = {
  address: Address,
  hideName?: boolean,
  className?: string,
}

const AddressDetails = (props: Props) => {
  const address = props.address;

  let countryInfo = null;

  if (address.region) {
    countryInfo = (
      <CountryInfo display={country => `, ${country.name}`} countryId={address.region.countryId} />
    );
  }

  const nameField = !props.hideName && address.name ? <li>{address.name}</li> : null;
  const className = props.className || styles['address-details'];

  return (
    <ul className={className}>
      {nameField}
      <li>{address.address1}</li>
      {address.address2 && <li>{address.address2}</li>}
      <li>
        {address.city}, <span>{address.region && address.region.name}</span> <span>{address.zip}</span>
        {countryInfo}
      </li>

      {address.phoneNumber && <li><PhoneNumber>{address.phoneNumber}</PhoneNumber></li>}
    </ul>
  );
};

export default AddressDetails;
