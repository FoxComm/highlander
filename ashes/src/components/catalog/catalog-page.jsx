/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { transitionTo, transitionToLazy } from 'browserHistory';

// components
import PageNav from 'components/core/page-nav';
import SaveCancel from 'components/core/save-cancel';
import Spinner from 'components/core/spinner';
import { IndexLink } from 'components/link';
import { PageTitle } from 'components/section-title';

// data
import * as CatalogActions from 'modules/catalog/details';
import * as CountryActions from 'modules/countries';

const sortCountries = createSelector(
  state => state.countries,
  (countries = {}) => _.values(countries).sort((a, b) => a.name < b.name ? -1 : 1)
);

function mapStateToProps(state, props) {
  return {
    catalog: state.catalogs.details.catalog,
    countries: sortCountries(state),
    fetchStatus: _.get(state, 'asyncActions.fetchCatalog'),
    createStatus: _.get(state, 'asyncActions.createCatalog'),
    updateStatus: _.get(state, 'asyncActions.updateCatalog'),
  };
}

const mapDispatchToProps = {
  ...CatalogActions,
  ...CountryActions,
};

type AsyncStatus = {
  err: any,
  inProgress: bool,
};

type Props = {
  children: ?any,
  catalog: ?Catalog,
  fetchCatalog: Function,
  createCatalog: Function,
  updateCatalog: Function,
  createStatus: ?AsyncStatus,
  updateStatus: ?AsyncStatus,
  countries: Array<Country>,
  params: {
    catalogId: number|string,
  },
};

type State = {
  name: string,
  site: ?string,
  countryId: number,
  defaultLanguage: string,
};

class CatalogPage extends Component {
  props: Props;

  state: State = {
    name: '',
    site: '',
    countryId: 234,
    defaultLanguage: 'en',
  };

  componentDidMount() {
    const { catalogId } = this.props.params;
    if (catalogId !== 'new') {
      this.props.fetchCatalog(catalogId);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.params.catalogId !== 'new' && nextProps.catalog) {
      const { name, site, countryId, defaultLanguage } = nextProps.catalog;
      this.setState({ name, site, countryId, defaultLanguage });
    }
  }

  get localNav() {
    const { catalogId } = this.props.params;
    const params = { catalogId };

    return (
      <PageNav>
        <IndexLink
          to="catalog-details"
          params={params}
        >
          Details
        </IndexLink>
      </PageNav>
    );
  }

  get inProgress() {
    const isCreating = _.get(this.props, 'createStatus.inProgress', false);
    const isUpdating = _.get(this.props, 'updateStatus.inProgress', false);
    return isCreating || isUpdating;
  }

  get isNew() {
    return this.props.params.catalogId === 'new';
  }

  get pageTitle() {
    if (this.isNew) {
      return 'New Catalog';
    }

    return this.state.name;
  }

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
    const submitAction = this.isNew
      ? (state) => this.props.createCatalog(state)
      : (state) => this.props.updateCatalog(this.props.params.catalogId, state);

    submitAction(this.state).then((resp) => {
      transitionTo('catalog-details', { catalogId: resp.id });
    });
  };

  render() {
    const { name, site, countryId, defaultLanguage } = this.state;
    const countries = this.props.countries || [];

    const upChildren = React.Children.map(this.props.children, child => {
      return React.cloneElement(child, {
        name,
        site,
        countries,
        countryId,
        defaultLanguage,
        onChange: this.handleChange,
      });
    });

    const isFetching = _.get(this.props, 'fetchStatus.inProgress', false);
    if (isFetching) {
      return <Spinner />;
    }

    return (
      <div>
        <PageTitle title={this.pageTitle}>
        <SaveCancel
          saveText="Save"
          onSave={this.handleSubmit}
          onCancel={transitionToLazy('catalogs')}
          isLoading={this.inProgress}
        />
        </PageTitle>
        {this.localNav}
        {upChildren}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(CatalogPage);
