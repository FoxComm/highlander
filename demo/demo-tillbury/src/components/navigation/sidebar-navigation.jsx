/* @flow */

import _ from 'lodash';
import { reduce } from 'lodash/fp';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { update } from 'sprout-data';
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

type State = {
  expanded: boolean,
};

const getState = state => ({ ...state.categories });

const flattenChildren = (res, item) => {
  if (_.isEmpty(item.children)) {
    res.push(item);

    return res;
  }

  return item.children.reduce(flattenChildren, res);
};

class SidebarNavigation extends Component {
  props: Props;

  state: State = {
    expanded: false,
  };

  componentWillMount() {
    this.props.fetch();
  }

  @autobind
  handleExpand() {
    this.setState({ expanded: true });
  }

  @autobind
  handleCollapse() {
    this.setState({ expanded: false });
  }

  @autobind
  renderItem(item) {
    const path = decodeURIComponent(this.props.path);

    return (
      <NavigationItem
        key={`${item.name}-navigation-item`}
        item={item}
        path={path}
        onClick={this.props.onClick}
        renderBack={this.props.renderBack}
        onGoBack={this.handleCollapse}
        onGoDeeper={this.handleExpand}
      />
    );
  }

  render(): Element<*> {
    const categoryItems = this.props.list
      .map(item => update(item, 'children', reduce(flattenChildren, [])))
      .map(this.renderItem);

    const cls = classNames(styles.list, {
      [styles.expanded]: this.state.expanded,
    });

    return (
      <div className={cls}>
        {categoryItems}
      </div>
    );
  }
}

export default connect(getState, actions)(SidebarNavigation);
