/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { addAsyncReducer } from '@foxcomm/wings';
import makeLocalStore from 'lib/local-store';
import { Product } from 'modules/products';
import styles from './search-form.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Typeahead from 'components/typeahead/typeahead';
import ProductRow from './product-row';

import reducer, { toggleActive, forceSearch, searchProducts } from 'modules/search';
import { toggleContentOverlay } from 'modules/content-overlay';

type SearchProps = Localized & {
  isActive: boolean,
  result: Array<Product>,
  toggleActive: Function,
  forceSearch: () => void,
  onSearch?: Function,
  isScrolled: boolean,
  setFocus: ?Function,
  onItemSelected?: () => void,
};

type SearchState = {
  term: string,
  focus: boolean,
};

class SearchForm extends Component {
  props: SearchProps;
  state: SearchState = {
    term: '',
    focus: false,
  };

  static defaultProps = {
    isScrolled: false,
  };

  componentWillUpdate(nextProps: SearchProps, nextState: SearchState) {
    if (nextState.focus != this.state.focus) {
      toggleContentOverlay();
    }
  }

  @autobind
  onKeyDown({ keyCode }: { keyCode: number }): void {
    if (keyCode === 13) {
      this.search();
      this.refs.input.blur();
    }

    if (keyCode === 27) {
      this.props.toggleActive();
      this.refs.input.blur();
    }
  }

  @autobind
  onChange({ target }: { target: { value: string }}): void {
    this.setState({ term: target.value });
  }

  @autobind
  setFocus() {
    this.setState({ focus: !this.state.focus });
  }

  @autobind
  onToggleVisibility(show: boolean) {
    this.props.toggleContentOverlay(show);
  }

  render(): Element<*> {
    const { t, results } = this.props;
    const items = _.get(results, 'result', []);

    return (
      <div styleName="search">
        <Typeahead
          className={styles['search-typeahead']}
          inputClassName={styles['search-input']}
          view="products"
          isFetching={_.get(this.props.searchState, 'inProgress', false)}
          fetchItems={this.props.searchProducts}
          minQueryLength={3}
          component={ProductRow}
          items={items}
          name="productsSelect"
          hideOnBlur
          placeholder={t('Search...')}
          onToggleVisibility={this.onToggleVisibility}
          onItemSelected={this.props.onItemSelected}
        />
      </div>
    );
  }
}

function mapState(state: Object, { isActive }: ?Object): Object {
  return {
    ...state,
    searchState: _.get(state.asyncActions, 'search', {}),
    isActive: isActive || state.isActive,
  };
}

export default _.flowRight(
  makeLocalStore(addAsyncReducer(reducer)),
  connect(
    mapState,
    { toggleContentOverlay, toggleActive, forceSearch, searchProducts }
  ),
  localized
)(SearchForm);
