/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import NewCatalogForm from './new-form';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { transitionTo, transitionToLazy } from 'browserHistory';

// data
import * as CountryActions from 'modules/countries';

const sortCountries = createSelector(
  state => state.countries,
  (countries = {}) => _.values(countries).sort((a, b) => a.name < b.name ? -1 : 1)
);

function mapStateToProps(state, props) {
  return {
    countries: sortCountries(state),
  };
}

type Props = {
  countries: Array<Country>,
};

type State = {
  name: string,
  defaultLanguage: string,
  site: string,
  countryId: ?number,
};

class NewCatalog extends Component {
  props: Props;

  state: State = {
    name: '',
    site: '',
    countryId: 234,
    defaultLanguage: 'en',
  };

  handleChange = (field: string, value: any) => {
    if (field == 'countryId') {
      this.setState({
        countryId: value,
        defaultLanguage: 'en',
      });
    } else {
      this.setState({ [field]: value });
    }
  };

  handleSubmit = () => {
    console.log('submitting!');
  };
    
  render() {
    const { countries } = this.props;
    const { name, defaultLanguage, site, countryId } = this.state;
    
    return (
      <NewCatalogForm
        name={name}
        defaultLanguage={defaultLanguage}
        site={site}
        countryId={countryId}
        countries={countries}
        onChange={this.handleChange}
        onCancel={transitionToLazy('catalogs')}
        onSubmit={this.handleSubmit}
      />
    )
  }
}

export default connect(mapStateToProps, CountryActions)(NewCatalog);
