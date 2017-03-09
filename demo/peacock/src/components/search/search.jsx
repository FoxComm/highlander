/* flow */

import React, { Component } from 'react';
import classNames from 'classnames';
import type { HTMLElement } from 'types';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Product } from 'modules/products';
import styles from './search.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Icon from 'ui/icon';

import { toggleActive, forceSearch } from 'modules/search';

type SearchProps = Localized & {
  isActive: boolean,
  result: Array<Product>,
  toggleActive: Function,
  forceSearch: () => void,
  onSearch?: Function,
  isScrolled: boolean,
};

type SearchState = {
  term: string,
};

class Search extends Component {
  props: SearchProps;
  state: SearchState = {
    term: '',
  };

  static defaultProps = {
    isScrolled: false,
  };

  @autobind
  onKeyDown({ keyCode }: any): void {
    if (keyCode === 13) {
      console.log("Enter");
      this.search();
      this.refs.input.blur();
    }

    if (keyCode === 27) {
      this.props.toggleActive();
      this.refs.input.blur();
    }
  }

  @autobind
  search(): void {
    if (!this.props.isActive) {
      return;
    }
    const { term } = this.state;

    if (term.length) {
      if (this.props.onSearch) this.props.onSearch();
      this.props.toggleActive();
      this.setState({ term: '' });
      // we do want make new request even if there is same term
      this.props.forceSearch();

      browserHistory.push(`/search/${term}`);
    }
  }

  @autobind
  handleClickSearch(): void {
    if (!this.props.isActive) {
      this.props.toggleActive();
      this.refs.input.focus();
    } else {
      this.search();
    }
  }

  @autobind
  onChange({ target }: any): void {
    this.setState({ term: target.value });
  }

  render(): HTMLElement {
    const searchStyle = this.props.isActive ? 'search-expanded' : 'search';

    const { t } = this.props;

    return (
      <div styleName='search'>
        <form action="." >
          <input value={this.state.term}
            onChange={this.onChange}
            onKeyDown={this.onKeyDown}
            styleName="search-input"
            autoComplete="off"
            placeholder={t('Search...')}
            ref="input"
            type="search"
          />
        </form>
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
