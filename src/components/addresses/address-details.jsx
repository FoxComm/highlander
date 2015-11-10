'use strict';

import React, { PropTypes } from 'react';
import CountryStore from '../../stores/countries';
import PhoneNumber from '../phone-number/phone-number';

class AddressDetails extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      countryName: null
    };
  }

  componentDidMount() {
    CountryStore.lazyFetch().then(this.updateCountryName.bind(this));
  }

  updateCountryName() {
    if (this.props.address.region) {
      this.setState({
        countryName: CountryStore.countryName(this.props.address.region.countryId)
      });
    }
  }

  get address2() {
    const { address } = this.props;

    if (address.address2) {
      return <li>{address.address2}</li>;
    }
  }

  get phoneNumber() {
    const { address } = this.props;

    if (address.phoneNumber) {
      return <li><PhoneNumber>{address.phoneNumber}</PhoneNumber></li>;
    }
  }

  render() {
    const address = this.props.address;

    return (
      <ul className="fc-address-details">
        <li className="name">{address.name}</li>
        <li>{address.address1}</li>
        {this.address2}
        <li>
          {address.city}, <span>{address.region && address.region.name}</span> <span>{address.zip}</span>
        </li>
        <li>{this.state.countryName}</li>
        {this.phoneNumber}
      </ul>
    );
  }
}

AddressDetails.propTypes = {
  address: PropTypes.object.isRequired
};

export default AddressDetails;
