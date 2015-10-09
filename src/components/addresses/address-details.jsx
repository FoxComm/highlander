'use strict';

import React from 'react';
import CountryStore from '../../stores/countries';

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

  formatNumber(value) {
    const numbers = value.replace(/[^\d]/g, '');

    if (numbers.length === 10) {
      return `(${numbers.slice(0, 3)})${numbers.slice(3, 6)}-${numbers.slice(6, 10)}`;
    }
    return value;
  }

  get phoneNumber() {
    const { address } = this.props;

    if (address.phoneNumber) {
      return <li>{this.formatNumber(address.phoneNumber)}</li>;
    }
  }

  render() {
    const address = this.props.address;

    return (
      <ul className="fc-address-details">
        <li className="name"><strong>{address.name}</strong></li>
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
  address: React.PropTypes.object.isRequired
};

export default AddressDetails;
