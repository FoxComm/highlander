/* flow */

import React, { Component } from 'react';
import classNames from 'classnames';
import type { HTMLElement } from 'types';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { Product } from 'modules/products';
import styles from './search.css';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import Icon from 'ui/icon';

import { setTerm, toggleActive } from 'modules/search';

type SearchProps = Localized & {
  isActive: boolean;
  term: string,
  result: Array<Product>,
  toggleActive: Function,
  onSearch: Function,
  setTerm: Function,
}

type SearchState = {
  term: string;
}

class Search extends Component {
  props: SearchProps;
  state: SearchState = {
    term: '',
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ term: nextProps.term });
  }

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
  search(): void {
    if (!this.props.isActive) {
      return;
    }
    const { term } = this.state;

    if (term.length) {
      this.props.onSearch();
      this.props.setTerm(term);

      browserHistory.push(`/search/${term}`);
    }
  }

  @autobind
  onSearch(): void {
    if (!this.props.isActive) {
      this.props.toggleActive();
      this.refs.input.focus();

      return;
    }

    this.search();
  }

  @autobind
  onChange({ target }: any): void {
    this.setState({ term: target.value });
  }

  render(): HTMLElement {
    const cls = classNames({
      search: !this.props.isActive,
      'search-expanded': this.props.isActive,
    });

    const { t } = this.props;

    return (
      <div styleName={cls}>
        <input value={this.state.term}
          onChange={this.onChange}
          onKeyDown={this.onKeyDown}
          styleName="search-input"
          autoComplete="off"
          placeholder={t('Search')}
          ref="input"
        />

        <Icon styleName="head-icon" name="fc-magnifying-glass" onClick={this.onSearch}/>
        <Icon styleName="close-icon" name="fc-close" onClick={this.props.toggleActive}/>
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

export default connect(mapState, { setTerm, toggleActive })(localized(Search));
