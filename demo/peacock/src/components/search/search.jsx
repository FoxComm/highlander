/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Product } from 'modules/products';
import styles from './search.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Typeahead from 'components/typeahead/typeahead';
import ProductRow from './product-row';

import { toggleActive, forceSearch } from 'modules/search';

type SearchProps = Localized & {
  isActive: boolean,
  result: Array<Product>,
  toggleActive: Function,
  forceSearch: () => void,
  onSearch?: Function,
  isScrolled: boolean,
  setFocus: ?Function,
};

type SearchState = {
  term: string,
  focus: boolean,
};

class Search extends Component {
  props: SearchProps;
  state: SearchState = {
    term: '',
    focus: false,
  };

  static defaultProps = {
    isScrolled: false,
  };

  @autobind
  onKeyDown({ keyCode }: any): void {
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
  onChange({ target }: any): void {
    this.setState({ term: target.value });
  }

  @autobind
  setFocus() {
    if (this.props.setFocus) {
      this.props.setFocus(!this.state.focus);
      this.setState({ focus: !this.state.focus });
    }
  }
  render(): Element<*> {
    const { t } = this.props;

    return (
      <div styleName="search">
        <Typeahead
          inputClassName={styles['search-input']}
          view="products"
          isFetching={_.get(this.props.suggestState, 'inProgress', false)}
          fetchItems={this.props.suggestUsers}
          minQueryLength={3}
          component={ProductRow}
          items={this.props.suggested}
          name="productsSelect"
          hideOnBlur={this.hideOnBlur}
          onItemSelected={this.handleSelectItem}
          placeholder={t('Search...')}
        />
      </div>
    );
  }
}

function mapState({ search }: Object, { isActive }: ?Object): Object {
  return {
    ...search,
    isActive: isActive || search.isActive,
  };
}

export default connect(mapState, { toggleActive, forceSearch })(localized(Search));
