
import React, { Component } from 'react';
import { connect } from 'react-redux';

import * as actions from 'modules/countries';

function mapStateToProps(state) {
  return {
    countries: state.countries.details,
  };
}

class CountryInfo extends Component {

  componentWillMount() {
    this.props.loadCountry(this.props.countryId);
  }

  get country() {
    return this.props.countries && this.props.countries[this.props.countryId];
  }

  render() {
    if (this.country) {
      return <span>{ this.props.display(this.country) } </span>;
    }
    return <span></span>;
  }
}

export default connect(mapStateToProps, actions)(CountryInfo);
