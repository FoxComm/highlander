
import React, { PropTypes } from 'react';
import PhoneNumber from '../phone-number/phone-number';
import * as CountriesActions from '../../modules/countries';
import { connect } from 'react-redux';

function mapStateToProps(state, props) {
  return {
    country: props.address && state.countries[props.address.region.countryId]
  };
}

@connect(mapStateToProps, CountriesActions)
export default class AddressDetails extends React.Component {

  static propTypes = {
    address: PropTypes.shape({
      region: PropTypes.shape({
        countryId: PropTypes.number
      }).isRequired
    }).isRequired
  };

  componentDidMount() {
    this.props.fetchCountry(this.props.address.region.countryId);
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
        <li>{this.props.country && this.props.country.name}</li>
        {this.phoneNumber}
      </ul>
    );
  }
}
