/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';

import NavigationItem from './navigation-item';
import SearchNavigationItem from './search-navigation-item';

import { fetch } from 'modules/categories';

import styles from './navigation.css';

type Props = {
  list: Array<any>,
  fetch: Function,
  onClick?: Function,
  onToggleSearch: (shouldOpen: ?boolean) => void,
  isSearchExpanded: boolean,
  path: string,
};

const mapState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetch();
  }

  render(): Element<*> {
    const path = decodeURIComponent(this.props.path);

    let categoryItems = _.map(this.props.list, (item) => {
      return (
        <NavigationItem
          key={item.name}
          item={item}
          path={path}
          isSearchExpanded={this.props.isSearchExpanded}
          onClick={this.props.onClick}
        />
      );
    });

    const searchItem = (
      <SearchNavigationItem
        onToggleState={this.props.onToggleSearch}
        key="search"
        linkContent="Search"
      />
    );

    categoryItems = [
      ...categoryItems,
      searchItem,
    ];

    return (
      <div styleName="list">
        {categoryItems}
      </div>
    );
  }
}

export default connect(mapState, { fetch })(Navigation);
