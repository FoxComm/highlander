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
  imgSrc: string,
  imgWidth: number,
  url?: string,
  children?: ?Array<any>,
  isHighlighted: boolean,
  linkTo?: string,
  linkRouter?: (slug: string) => string,
  overrideChildrenLink?: string,
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
  getNavUrl(category: ?Category): string {
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

  renderViewAll(cat: Category, baseUrl: string): Element<*> {
    return (
      <div key={`${cat.name}-view-all-leaf-category`}>
        <Link
          styleName="drawer-subitem-link"
          onClick={this.handleClick}
          to={baseUrl}
        >
          View All
        </Link>
      </div>
    );
  }

  renderSubcategoryItems(subcategory: Category, baseUrl: string, parentUrl: string): ?Element<*> {
    if (!subcategory.children) return null;

    const items = _.map(subcategory.children, (item) => {
      let url;
      if (item.linkTo) {
        url = item.linkTo;
      } else if (subcategory.overrideChildrenLink) {
        url = subcategory.overrideChildrenLink.replace('$slug', categoryNameToUrl(item.name));
      } else if (subcategory.categoryQuery) {
        url = `${parentUrl}?${subcategory.categoryQuery}=${categoryNameToUrl(item.name).toUpperCase()}`;
      } else {
        url = item.skipLink ? baseUrl : `${baseUrl}${this.getNavUrl(item)}`;
      }

      const link = item.skipLink ? (
        <span
          styleName="drawer-subitem-link"
        >
          {humanize(item.name)}
        </span>
        ) : (
          <Link
            styleName="drawer-subitem-link"
            onClick={this.handleClick}
            to={url}
          >
            {humanize(item.name)}
          </Link>
        );
      return (
        <div key={`${item.name}-leaf-category`}>
          {link}
        </div>
      );
    });

    if (subcategory.withViewAll) {
      items.push(this.renderViewAll(subcategory, parentUrl));
    }

    return (
      <div styleName="drawer-subitems">
        { items }
      </div>
    );
  }

  get baseUrl(): string {
    return `/c${this.getNavUrl(this.props.item)}`;
  }

  get drawer(): ?Element<*> {
    const { item } = this.props;

    if (!item.children) return null;

    const drawerStyle = classNames(styles.submenu, {
      [styles.open]: this.state.expanded,
    });

    const children = _.map(item.children, (child) => {
      const url = child.skipLink ? this.baseUrl : `${this.baseUrl}${this.getNavUrl(child)}`;

      let actualUrl = url;
      if (child.linkTo) {
        actualUrl = child.linkTo;
      }

      if (child.imgSrc) {
        return (
          <Link
            to={child.linkTo}
            className={`${styles['drawer-column']} ${styles['_with-image']}`}
            key={`${child.name}-sub-category`}
            style={{width: `${child.width}%`}}
          >
            <div>
              <div styleName="drawer-child-image">
                <img src={child.imgSrc} />
              </div>
              <div className={`${styles['drawer-item-label']} ${styles['_with-image']}`}>
                {humanize(child.name)}
              </div>
            </div>
          </Link>
        );
      }

      return (
        <div styleName="drawer-column" key={`${child.name}-sub-category`}>
          <Link to={url} styleName="drawer-item-label">{humanize(child.name)}</Link>
          { this.renderSubcategoryItems(child, url, this.baseUrl) }
        </div>
      );
    });

    return (
      <div className={drawerStyle}>
        <div styleName="drawer-content">
          <div styleName="drawer-columns" style={{ flexBasis: `${100 - item.imgWidth}%` }}>
            { children }
          </div>
          <div styleName="drawer-image" style={{ flexBasis: `${item.imgWidth}%` }}>
            {item.imgSrc && _.map(item.imgSrc, src => (<img src={src} />))}
          </div>
        </div>
        {!item.withoutViewAll &&
          <Link
            styleName="image-link"
            to={this.baseUrl}
            onClick={this.handleClick}
          >
            View All {item.name}
          </Link>
        }
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
    const url = this.baseUrl;
    const currentUrl = _.isEmpty(item.linkTo) ? url : item.linkTo;
    const basePath = router.createPath({ name: 'category', params: { categoryName: item.name } }, true);
    const isActive = `${path}/`.startsWith(basePath);
    const linkBlockClasses = classNames(styles.item, {
      [styles.active]: isActive,
      [styles['with-drawer-open']]: this.state.expanded,
      [styles['is-highlighted']]: item.isHighlighted,
    });

    const linkClass = classNames(styles['item-link'], {
      [styles['is-highlighted']]: item.isHighlighted,
    });

    return (
      <div
        className={linkBlockClasses}
        key={key}
        onMouseEnter={this.handleHoverOn}
        onMouseLeave={this.handleHoverOff}
      >
        <Link
          className={linkClass}
          to={currentUrl}
          onClick={this.handleClick}
        >
          {humanize(item.name)}
        </Link>
        { this.drawer }
      </div>
    );
  }
}
