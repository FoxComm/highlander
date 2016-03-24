/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import type { HTMLElement } from 'types';
import type { Product } from 'modules/products';
import styles from './search.css';

import ProductsList from '../../products-list/products-list';

import { setTerm, fetch } from 'modules/search';

type SearchParams = {
  term: string;
}

type SearchProps = {
  term: string;
  results: Array<Product>;
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
    const { term, results } = this.props;

    return (
      <div styleName="search">
        <p styleName="search-title"><span styleName="search-title__uppercase">Search results for</span> "{term}"</p>
        <ProductsList list={results}/>
      </div>
    );
  }
}

function mapState({ search }: any): Object {
  return {
    ...search,
  };
}

export default connect(mapState, { setTerm, fetch })(Search);
