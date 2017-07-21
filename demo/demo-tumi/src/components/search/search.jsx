/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Product } from 'modules/products';
import clickOutsideEnhancer from 'react-click-outside';
import { responsiveConnect } from 'css/responsive-connect';
import makeLocalStore from 'lib/local-store';
import { addAsyncReducer } from '@foxcomm/wings/lib/redux/make-local-store';
import { browserHistory } from 'lib/history';

import styles from './search.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Form from 'ui/forms/form';
import Typeahead from 'components/typeahead/typeahead';
import ProductsMenu from './products-menu';
import SearchInput from './input';

// actions

import productsReducer, { fetch, resetSearch } from 'modules/products';

type SearchProps = Localized & {
  result: Array<Product>,
  toggleActive: Function,
  onSearch?: Function,
  searchState: AsyncState,
  fetch: (term) => Promise,
  resetSearch: () => void,
  onClose?: () => void,
  onClickOutside: (e: SyntheticEvent) => void,
  isMedium: boolean,
  visible: boolean,
};

type SearchState = {
  term: string,
  focus: boolean,
};

function mapStateToProps(state) {
  return {
    searchState: _.get(state.asyncActions, 'products', {}),
    results: state.list,
  };
}

class Search extends Component {
  props: SearchProps;
  state: SearchState = {
    term: '',
  };

  componentWillReceiveProps(nextProps: SearchProps) {
    if (this.props.visible && !nextProps.visible) {
      this.props.resetSearch();
      this.setState({
        term: '',
      });
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
  handleInputChange({ target }: { target: { value: string }}): void {
    this.setState({ term: target.value });
  }

  @autobind
  searchProducts(term) {
    return this.props.fetch([], {}, term, {
      from: 0,
      toLoad: 4,
    });
  }

  handleClickOutside(e: SyntheticEvent) {
    if (this.props.isMedium) {
      this.props.onClickOutside(e);
    }
  }

  get inputElement(): SearchInput {
    return (
      <SearchInput
        onChange={this.handleInputChange}
        value={this.state.term}
        name="text"
        styleName="search-input"
      />
    );
  }

  @autobind
  submit() {
    browserHistory.push({
      name: 'search',
      query: {
        text: this.state.term,
      },
    });
  }

  render(): Element<*> {
    const { props } = this;
    const { t, results } = props;

    const menu = <ProductsMenu items={results} term={this.state.term} />;

    return (
      <Form styleName="search" method="get" action="/search" onSubmit={this.submit}>
        <Typeahead
          className={styles['search-typeahead']}
          inputElement={this.inputElement}
          view="search"
          isFetching={_.get(props.searchState, 'inProgress', false)}
          fetchItems={this.searchProducts}
          minQueryLength={3}
          itemsElement={menu}
          itemsVisible={results.length > 0 && props.searchState.finished && props.visible}
          name="productsSelect"
          hideOnBlur
          placeholder={t('Search')}
        />
        <div styleName="search-icon" onClick={this.submit} />
        <div styleName="close-icon" onClick={props.onClose} />
      </Form>
    );
  }
}

export default _.flowRight(
  localized,
  responsiveConnect(['isMedium']),
  makeLocalStore(addAsyncReducer(productsReducer)),
  connect(mapStateToProps, { fetch, resetSearch }),
  clickOutsideEnhancer
)(Search);
