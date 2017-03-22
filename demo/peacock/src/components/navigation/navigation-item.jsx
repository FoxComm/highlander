/* @flow */

import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';

import classNames from 'classnames';

import { convertCategoryNameToUrlPart } from 'modules/categories';

import styles from './navigation.css';

type Category = {
  name: string,
  id: number,
  description: string,
  url?: string,
  children?: ?Array<any>,
};

type Props = {
  item: Category,
  path: string,
  t: any,
  onClick: ?Function,
};

type State = {
  expanded: boolean,
};

export default class NavigationItem extends Component {
  props: Props;
  state: State = {
    expanded: false,
  };

  @autobind
  getNavUrl(category : ?Category) {
    let url;

    if (category == undefined) {
      url = '/';
    } else {
      const dashedName = convertCategoryNameToUrlPart(category.name);
      url = `/${dashedName}`;
    }

    return url;
  }

  @autobind
  handleHoverOn() {
    this.setState({ expanded: true });
  }

  @autobind
  handleHoverOff() {
    this.setState({ expanded: false });
  }

  renderSubcategoryItems(subcategory) {
    if (!subcategory.children) return null;

    const items = _.map(subcategory.children, (item) => {
      return (
        <div>
          <Link styleName="drawer-subitem-link" onClick={this.props.onClick}>
            {item.name}
          </Link>
        </div>
      );
    });

    return (
      <div>
        { items }
      </div>
    );
  }

  get drawer() {
    const { item, onClick } = this.props;

    if (!item.children) return null;

    const drawerStyle = classNames(styles.submenu, {
      [styles.open]: this.state.expanded,
    });

    const children = _.map(item.children, (item) => {
      return (
        <div>
          <Link styleName="drawer-item-link" onClick={onClick}>
            {item.name}
          </Link>
          { this.renderSubcategoryItems(item) }
        </div>
      );
    });

    return (
      <div className={drawerStyle}>
        <div styleName="drawer-columns">
          { children }
        </div>
      </div>
    );
  }

  render() {
    const { item, path, t } = this.props;

    if (item.hiddenInNavigation) {
      return null;
    }

    const dashedName = item.name.replace(/\s/g, '-');
    const key = `category-${dashedName}`;
    const url = this.getNavUrl(item);
    const isActive = path.match(new RegExp(dashedName, 'i'));
    const linkClasses = classNames(styles.item, {
      [styles.active]: isActive,
    });

    return (
      <div
        className={linkClasses}
        key={key}
        onMouseEnter={this.handleHoverOn}
        onMouseLeave={this.handleHoverOff}
      >
        <Link
          styleName="item-link"
          to={url}
          onClick={this.props.onClick}
        >
          {t(item.name.toUpperCase())}
        </Link>
        { this.drawer }
      </div>
    );
  }
}
