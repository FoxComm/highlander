/* flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { bindActionCreators } from 'redux';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Product } from 'modules/products';
import styles from './search.css';

import Icon from 'ui/icon';

import { actions } from 'modules/search';
import { toggleSidebar } from 'modules/sidebar';

type SearchProps = {
  term: string,
  results: Array<Product>,
  toggleSidebar: Function,
  onSearch: Function,
  setTerm: Function,
  fetch: Function
}

function mapState({ search }: Object): any {
  return {
    ...search,
  };
}

function mapDispatch(dispatch:Function): any {
  return {
    ...bindActionCreators(actions, dispatch),
    toggleSidebar,
  };
}

class Search extends Component {
  props: SearchProps;

  @autobind
  onChange({ target }: any): void {
    this.props.setTerm(target.value);
  }

  @autobind
  onSearch(): void {
    if (this.props.term.length) {
      this.props.fetch(this.props.term);
      this.props.toggleSidebar();

      browserHistory.push(`/search/${this.props.term}`);
    }
  }

  render():HTMLElement {
    return (
      <div styleName="search">
        <input styleName="search-input" type="text" autoComplete="off" onChange={this.onChange} placeholder="Search"/>

        <Icon styleName="head-icon" name="fc-magnifying-glass" onClick={this.onSearch}/>
      </div>
    );
  }
}

export default connect(mapState, mapDispatch)(Search);
