/* @flow */

import React, { Component } from 'react';
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

    const drawerStyle = classNames(styles.submenu, {
      [styles.open]: this.state.expanded,
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
        { item.children && <div className={drawerStyle}>Sumbenu { item.name }</div> }
      </div>
    );
  }
}
