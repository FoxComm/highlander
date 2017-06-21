/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';
import TextInput from 'components/core/text-input';
import Icon from 'components/core/icon';

// styles
import styles from './dropdown-search.css';

import type { Props as GenericProps } from './generic-dropdown';

type Props = GenericProps & {
  searchbarPlaceholder?: string,
  fetchOptions: Function,
  renderOption?: Function,
};

type State = {
  token: string,
  results: Array<any>,
};

function doNothing(event: any) {
  event.stopPropagation();
  event.preventDefault();
}

export default class DropdownSearch extends Component {
  props: Props;

  state: State = {
    token: '',
    results: [],
  };

  componentDidMount() {
    this.fetchOptions(this.state.token);
  }

  fetchOptions(token: string) {
    this.props.fetchOptions(this.state.token).then(data => {
      this.setState({ results: data });
    });
  }

  @autobind
  onTokenChange(token: string) {
    this.setState({ token }, () => {
      this.fetchOptions(token);
    });
  }

  @autobind
  searchBar() {
    return (
      <div styleName="searchbar" onClick={doNothing}>
        <div styleName="searchbar-wrapper">
          <div className="fc-form-field" styleName="searchbar-input-wrapper">
            <TextInput
              placeholder={this.props.searchbarPlaceholder}
              styleName="searchbar-input"
              value={this.state.token}
              onChange={this.onTokenChange}
            />
          </div>
          <div styleName="searchbar-icon-wrapper">
            <Icon name="search" />
          </div>
        </div>
      </div>
    );
  }

  get results(): Array<any> {
    const results = this.state.results;

    if (_.isEmpty(results)) {
      return [];
    }

    return results;
  }

  get searchResults(): any {
    const { renderOption } = this.props;
    return _.map(this.results, result => {
      if (renderOption) {
        return renderOption(result);
      }

      const id = _.get(result, 'id', '');
      const name = _.get(result, 'name', '');

      return (
        <DropdownItem value={id} key={`${id}-${name}`}>
          {name}
        </DropdownItem>
      );
    });
  }

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
    return !_.eq(this.state, nextState);
  }

  render() {
    const restProps = _.omit(this.props, 'children');

    return (
      <GenericDropdown
        placeholder="- Select -"
        {...restProps}
        listClassName="fc-searchable-dropdown__item-list"
        renderPrepend={this.searchBar}
      >
        {this.searchResults}
      </GenericDropdown>
    );
  }
}
