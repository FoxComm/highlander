// @flow

import React, { Component } from 'react';
import styles from './navigation.css';

import DumbNavigationItem from './dumb-navigation-item';

type Props = {
  onToggleState: (shouldOpen: ?boolean) => void,
}

class SearchNavigationItem extends Component {
  props: Props;

  get linkContent(): Element<*> {
    return <span styleName="search-link">Search</span>;
  }

  render() {
    return (
      <DumbNavigationItem
        linkContent={this.linkContent}
        onClick={() => this.props.onToggleState()}
      />
    );
  }
}

export default SearchNavigationItem;
