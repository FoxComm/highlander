/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

import NavigationItem from './navigation-item';

import { fetch } from 'modules/categories';

import styles from './navigation.css';

type Props = {
  list: Array<any>,
  fetch: Function,
  onClick?: ?Function,
  path: string,
};

const getState = state => ({...state.categories});

class Navigation extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetch();
  }

  render(): Element<*> {
    const path = decodeURIComponent(this.props.path);

    const categoryItems = _.map(this.props.list, (item) => {
      return (
        <NavigationItem
          key={`${item.name}-navigation-item`}
          item={item}
          path={path}
          onClick={this.props.onClick}
        />
      );
    });

    return (
      <div styleName="list">
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, { fetch })(Navigation);
