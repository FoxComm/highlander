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

import { setTerm, fetch } from 'modules/search';

type SearchParams = {
  term: string;
}

type SearchResult = {
  total: number,
  pagination: { total: number },
  max_score: ?number,
  result: Array<Product>|Object,
};

type SearchProps = Localized & {
  term: string;
  results: SearchResult;
  params: SearchParams;
  setTerm: Function;
  fetch: Function;
}

class Search extends Component {
  props: SearchProps;

  componentWillMount() {
    if (this.props.term != this.props.params.term) {
      this.props.setTerm(this.props.params.term);
    } else {
      this.props.fetch(this.props.term);
    }
  }

  componentWillReceiveProps(nextProps: SearchProps) {
    if (this.props.term !== nextProps.term) {
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
        <ProductsList list={result}/>
      </div>
    );
  }
}

function mapState({ search }: any): Object {
  return {
    ...search,
  };
}

export default connect(mapState, { setTerm, fetch })(localized(Search));
