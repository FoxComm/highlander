/* @flow */

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import localized from 'lib/i18n';

// components
import ProductsList from 'components/products-list/products-list';

// styles
import styles from './search.css';

// types
import type { HTMLElement } from 'types';
import type { AsyncStatus } from 'types/async-actions';
import type { Product } from 'modules/products';
import type { Localized } from 'lib/i18n';

// actions
import { searchProducts } from 'modules/search';

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
  searchProducts: (term: string) => Promise,
  searchState: AsyncStatus,
};

function mapStateToProps(state): Object {
  return {
    ...state.search,
    searchState: _.get(state.asyncActions, 'search', {}),
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

  render(): HTMLElement {
    const { params, results, t } = this.props;
    const { term } = params;

    const result = _.isEmpty(results.result) ? [] : results.result;

    return (
      <div styleName="search">
        <h1 styleName="search-title">
          <span styleName="label">{t('Search results for')}</span>
          <strong styleName="term">"{term}"</strong>
        </h1>
        <ProductsList
          list={result}
          isLoading={this.props.searchState.inProgress !== false}
        />
      </div>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, {searchProducts}),
  localized
)(Search);
