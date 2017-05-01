/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import localized from 'lib/i18n';
// components
import ProductsList from 'components/products-list/products-list';

// styles
import styles from './search.css';

// types
import type { AsyncStatus } from 'types/async-actions';
import type { Product } from 'modules/products';
import type { Localized } from 'lib/i18n';

import createSearch from 'modules/search';

// actions
const { searchProducts } = createSearch('searchUrl');

type SearchParams = {
  term: string,
};

type SearchResult = {
  total: number,
  pagination: { total: number },
  max_score: ?number,
  result: Array<Product>|Object,
};

type Props = Localized & {
  term: string,
  results: SearchResult,
  params: SearchParams,
  force: boolean,
  searchProducts: (term: string) => Promise<*>,
  searchState: AsyncStatus,
};

function mapStateToProps(state): Object {
  return {
    ...state.searchUrl,
    searchState: _.get(state.asyncActions, 'searchUrl', {}),
  };
}

class Search extends Component {
  props: Props;

  componentWillMount() {
    this.search(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.params.term != nextProps.params.term || nextProps.force) {
      this.search(nextProps);
    }
  }

  search(props: Props) {
    props.searchProducts(props.params.term);
  }

  render(): Element<*> {
    const { params, results, t } = this.props;
    const { term } = params;

    const result = _.isEmpty(results.result) ? [] : results.result;

    return (
      <div styleName="search">
        <h1 styleName="search-title">
          <span styleName="label">{t('Search results for')}</span>
          <strong styleName="term">&quot;{term}&quot;</strong>
        </h1>
        <ProductsList
          sorting={{direction: 1, field: 'salesPrice'}}
          changeSorting={_.noop}
          list={result}
          isLoading={this.props.searchState.inProgress !== false}
          fetchMoreProducts={_.noop}
          moreAvailable={false}
        />
      </div>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, {searchProducts}),
  localized
)(Search);
