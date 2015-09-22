'use strict';

import React from 'react';
import CountryStore from '../../stores/countries';

class AddressDetails extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      countryName: null
    }
  }

  componentDidMount() {
    CountryStore.lazyFetch().then(this.updateCountryName.bind(this));
  }

  updateCountryName() {
    this.setState({
      countryName: CountryStore.countryName(this.props.address.region.countryId)
    });
  }

  render() {
    let address = this.props.address;

    return (
      <ul className="fc-address-details">
        <li className="name"><strong>{address.name}</strong></li>
        <li>{address.address1}</li>
        { address.address2 ? <li>{address.address2}</li> : '' }
        <li>
          {address.city}, <span>{address.region && address.region.name}</span> <span>{address.zip}</span>
        </li>
        <li>{this.state.countryName}</li>
        { address.phoneNumber ? <li>{address.phoneNumber}</li> : '' }
      </ul>
    )
  }
}

AddressDetails.propTypes = {
  address: React.PropTypes.object.isRequired,
};

export default AddressDetails;
