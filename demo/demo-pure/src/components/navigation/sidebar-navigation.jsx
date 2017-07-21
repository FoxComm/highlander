/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

import NavigationItem from './sidebar-navigation-item';

import * as actions from 'modules/categories';

import styles from './sidebar-navigation.css';

type Props = {
  list: Array<any>,
  fetch: Function,
  onClick?: ?Function,
  path: string,
  renderBack: Function,
};

const getState = state => ({...state.categories});

class SidebarNavigation extends Component {
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
          renderBack={this.props.renderBack}
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

export default connect(getState, actions)(SidebarNavigation);
