/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import NewCatalogForm from './new-form';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { transitionTo, transitionToLazy } from 'browserHistory';

// data
import * as CatalogActions from 'modules/catalog/details';
import * as CountryActions from 'modules/countries';

const sortCountries = createSelector(
  state => state.countries,
  (countries = {}) => _.values(countries).sort((a, b) => a.name < b.name ? -1 : 1)
);

function mapStateToProps(state, props) {
  return {
    countries: sortCountries(state),
    createStatus: _.get(state, 'asyncActions.createCatalog'),
  };
}

const mapDispatchToProps = {
  ...CatalogActions,
  ...CountryActions,
};

type Props = {
  createCatalog: Function,
  createStatus: ?{
    err: any,
    inProgress: bool,
  },
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
    this.props.createCatalog(this.state).then((resp) => {
      transitionTo('catalog-details', { catalogId: resp.id });
    });
  };
    
  render() {
    const { countries } = this.props;
    const { name, defaultLanguage, site, countryId } = this.state;
    const isLoading = _.get(this.props, 'createStatus.inProgress', false);
    const err = _.get(this.props, 'createStatus.err');
    
    return (
      <NewCatalogForm
        err={err}
        isLoading={isLoading}
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

export default connect(mapStateToProps, mapDispatchToProps)(NewCatalog);
