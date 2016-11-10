  /* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './search.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import ProductsList from '../../components/products-list/products-list';

import { setTerm, fetch, resetSearchResults } from 'modules/search';

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
  setTerm: Function,
  fetch: Function,
  isLoading?: boolean,
};

class Search extends Component {
  props: Props;

  componentWillMount() {
    if (this.props.term != this.props.params.term) {
      this.props.setTerm(this.props.params.term);
    } else {
      this.props.fetch(this.props.term);
    }
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.term !== nextProps.term) {
      this.props.resetSearchResults();
      this.props.fetch(nextProps.term);
    }
  }

  render(): HTMLElement {
    const { term, results, t } = this.props;
    const result = _.isEmpty(results.result) ? [] : results.result;

    return (
      <div styleName="search">
        <p styleName="search-title">
          <span styleName="search-title__uppercase">{t('Search results for')}</span> "{term}"
        </p>
        <ProductsList
          list={result}
          isLoading={this.props.isLoading}
        />
      </div>
    );
  }
}

function mapState(state): Object {
  const async = state.asyncActions.search;

  return {
    ...state.search,
    isLoading: !!async ? async.inProgress : true,
  };
}

export default connect(mapState, {
  setTerm,
  fetch,
  resetSearchResults,
})(localized(Search));
