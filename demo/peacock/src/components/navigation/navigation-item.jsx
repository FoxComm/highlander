/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { Link, routerShape } from 'react-router';

import classNames from 'classnames';

import { humanize, categoryNameToUrl } from 'paragons/categories';

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
  onClick: ?() => void,
  onShow?: () => void,
};

type State = {
  expanded: boolean,
};

export default class NavigationItem extends Component {
  props: Props;
  state: State = {
    expanded: false,
  };

  static contextTypes = {
    router: routerShape,
  };

  @autobind
  getNavUrl(category : ?Category): string {
    let url;

    if (category == undefined) {
      url = '/';
    } else {
      const dashedName = categoryNameToUrl(category.name);
      url = `/${dashedName}`;
    }

    return url;
  }

  @autobind
  handleStateChange(to: boolean, call: Function = _.noop) {
    this.setState({ expanded: to }, () => {
      if (this.props.onShow) {
        this.props.onShow(to);
      }
      call();
    });
  }

  @autobind
  handleClick() {
    this.handleStateChange(false, () => {
      if (this.props.onClick) {
        this.props.onClick();
      }
    });
  }

  @autobind
  handleHoverOn() {
    this.handleStateChange(true);
  }

  @autobind
  handleHoverOff() {
    this.handleStateChange(false);
  }

  renderSubcategoryItems(subcategory: Category, baseUrl: string): ?Element<*> {
    if (!subcategory.children) return null;

    const items = _.map(subcategory.children, (item) => {
      const url = `${baseUrl}${this.getNavUrl(item)}`;
      return (
        <div key={`${item.name}-leaf-category`}>
          <Link
            styleName="drawer-subitem-link"
            onClick={this.handleClick}
            to={url}
          >
            {humanize(item.name)}
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

  get baseUrl(): string {
    return `/s${this.getNavUrl(this.props.item)}`;
  }

  get drawer(): ?Element<*> {
    const { item } = this.props;

    if (!item.children) return null;

    const drawerStyle = classNames(styles.submenu, {
      [styles.open]: this.state.expanded,
    });

    const children = _.map(item.children, (child) => {
      const url = `${this.baseUrl}${this.getNavUrl(child)}`;
      const childName = child.ignoreCategoryFilter ? (
        <span styleName="drawer-item-link">
          {humanize(child.name)}
        </span>
      ) : (
        <Link
          styleName="drawer-item-link"
          to={url}
          onClick={this.handleClick}
        >
          {humanize(child.name)}
        </Link>
      );
      return (
        <div key={`${child.name}-sub-category`}>
          {childName}
          { this.renderSubcategoryItems(child, url) }
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
    const { item, path } = this.props;
    const { router } = this.context;

    if (item.hiddenInNavigation) {
      return null;
    }

    const dashedName = _.toLower(item.name.replace(/\s/g, '-'));
    const key = `category-${dashedName}`;
    const url = item.url || this.getNavUrl(item);
    const basePath = router.createPath({name: 'category', params: {categoryName: item.name}}, true);
    const isActive = `${path}/`.startsWith(basePath) || path.startsWith(url);
    const linkClasses = classNames(styles.item, {
      [styles.active]: isActive,
      [styles['with-drawer-open']]: this.state.expanded,
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
          onClick={this.handleClick}
        >
          {humanize(item.name)}
        </Link>
        { this.drawer }
      </div>
    );
  }
}
