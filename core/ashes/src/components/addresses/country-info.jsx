
import _ from 'lodash';
import React, { PropTypes } from 'react';
import PhoneNumber from '../phone-number/phone-number';
import { connect } from 'react-redux';

@connect(state => state)
export default class CountryInfo extends React.Component {
  static propTypes = {
    display: PropTypes.func.isRequired,
    countryId: PropTypes.number.isRequired,
    countries: PropTypes.object
  };

  get country() {
    return this.props.countries && this.props.countries[this.props.countryId];
  }

  render() {
    if (this.country) {
      return <div>{ this.props.display(this.country) } </div>;
    }
    return <div></div>;
  }
}
