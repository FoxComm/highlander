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
  fetch: Function,
}

type SearchState = {
  term: string;
}

function mapState({ search }: Object):any {
  return {
    ...search,
  };
}

function mapDispatch(dispatch:Function):any {
  return {
    ...bindActionCreators({ ...actions, toggleSidebar }, dispatch),
  };
}

class Search extends Component {
  props:SearchProps;
  state:SearchState = {
    term: '',
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ term: nextProps.term });
  }

  @autobind
  onChange({ target }: any):void {
    this.setState({ term: target.value });
  }

  @autobind
  onSearch():void {
    const { term } = this.state;

    if (term.length) {
      this.props.setTerm(term);
      this.props.toggleSidebar();

      browserHistory.push(`/search/${term}`);
    }
  }

  render():HTMLElement {
    return (
      <div styleName="search">
        <input value={this.state.term}
          onChange={this.onChange}
          styleName="search-input"
          autoComplete="off"
          placeholder="Search"
          ref="input"
        />

        <Icon styleName="head-icon" name="fc-magnifying-glass" onClick={this.onSearch}/>
      </div>
    );
  }
}

export default connect(mapState, mapDispatch)(Search);
