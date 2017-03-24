/* @flow */

import React, { Component, Element } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { Link } from 'react-router';

import classNames from 'classnames';

import { humanize, categoryNameToUrl } from 'paragons/categories';

import styles from './sidebar-navigation.css';

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
  onClick: ?Function,
};

type State = {
  expanded: number,
};

export default class NavigationItem extends Component {
  props: Props;
  state: State = {
    expanded: [],
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
  handleClick() {
    console.log('handle click');
    if (this.props.onClick) {
      this.props.onClick();
    }
  }

  get baseUrl(): string {
    return this.getNavUrl(this.props.item);
  }

  @autobind
  drawer(item: Category, parentUrl: string, parentName: string): ?Element<*> {
    const level = this.state.expanded.length;
    const drawerStyle = classNames(styles.submenu, {
      [styles['submenu-open']]: this.state.expanded.indexOf(item.name) >= 0,
    });

    const children = _.map(item.children, (child) => {
      const url = `${parentUrl}${this.getNavUrl(child)}`;
      const name = humanize(child.name);

      if (_.isEmpty(child.children)) {
        return (
          <div key={`${child.name}-leaf-category`}>
            <Link
              styleName="submenu-link-item"
              to={url}
            >
              {name}
            </Link>
          </div>
        );
      }

      return (
        <div key={`${child.name}-sub-category`}>
          <Link
            styleName="submenu-link-item"
            onClick={e => this.goDown(e, child.name)}
          >
            {name}
          </Link>
          {this.drawer(child, url, name)}
        </div>
      );
    });

    let style = {zIndex: 10 * level};
    if (this.state.expanded.indexOf(item.name) >= 1) {
      style.top = 0;
    }

    return (
      <div className={drawerStyle} style={style}>
        <div styleName="drawer-columns">
          <Link
            styleName="item-link"
            to={parentUrl}
          >
            {parentName}
          </Link>
          { children }
        </div>
      </div>
    );
  }

  @autobind
  goDown(e: SyntheticEvent, next: string) {
    e.preventDefault();
    e.stopPropagation();
    console.log('Go down', this.state.expanded);
    this.setState({ expanded: [...this.state.expanded, next] });
  }

  render() {
    const { item, path } = this.props;

    if (item.hiddenInNavigation) {
      return null;
    }

    const dashedName = _.toLower(item.name.replace(/\s/g, '-'));
    const key = `category-${dashedName}`;
    const url = this.getNavUrl(item);
    const isActive = path.match(new RegExp(dashedName, 'i'));
    const linkClasses = classNames(styles.item, {
      [styles.active]: isActive,
    });
    const name = humanize(item.name);

    return (
      <div
        className={linkClasses}
        key={key}
      >
        <Link
          styleName="item-link"
          onClick={e => this.goDown(e, item.name)}
        >
          {name}
        </Link>
        {this.drawer(item, url, name)}
      </div>
    );
  }
}
