/* @flow */

import _ from 'lodash';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { Link, routerShape } from 'react-router';
import { humanize, categoryNameToUrl } from 'paragons/categories';
import styles from './navigation.css';

import DumbNavigationItem from './dumb-navigation-item';
import Submenu from './submenu';

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
  isSearchExpanded: boolean,
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
    // do not allow to open submenu on hover in case of search popup is expanded
    if (!this.props.isSearchExpanded) {
      this.handleStateChange(true);
    }
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
      if (subcategory.overrideChildrenLink) {
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

    const children = _.map(item.children, (child) => {
      const url = child.skipLink ? this.baseUrl : `${this.baseUrl}${this.getNavUrl(child)}`;

      return (
        <div styleName="drawer-column" key={`${child.name}-sub-category`}>
          <div styleName="drawer-item-label">{humanize(child.name)}</div>
          { this.renderSubcategoryItems(child, url, this.baseUrl) }
        </div>
      );
    });

    return (
      <Submenu open={this.state.expanded}>
        <div styleName="drawer-columns" style={{ flexBasis: `${100 - item.imgWidth}%` }}>
          { children }
        </div>
        <div styleName="drawer-image" style={{ flexBasis: `${item.imgWidth}%` }}>
          <img src={item.imgSrc} />
          <Link
            styleName="image-link"
            to={this.baseUrl}
            onClick={this.handleClick}
          >
            View All &gt;
          </Link>
        </div>
      </Submenu>
    );
  }

  render() {
    const { item, path } = this.props;
    const { router } = this.context;

    if (item.hiddenInNavigation) {
      return null;
    }

    const url = this.getNavUrl(item);
    const currentUrl = _.isEmpty(item.linkTo) ? url : item.linkTo;
    const basePath = router.createPath({ name: 'category', params: { categoryName: item.name } }, true);
    const isActive = `${path}/`.startsWith(basePath);

    const linkBlockClasses = classNames({
      [styles['with-drawer-open']]: this.state.expanded,
    });

    return (
      <DumbNavigationItem
        isHighlighted={item.isHighlighted}
        className={linkBlockClasses}
        isActive={isActive}
        to={currentUrl}
        onClick={this.handleClick}
        blockProps={{
          onMouseEnter: this.handleHoverOn,
          onMouseLeave: this.handleHoverOff,
        }}
        linkContent={humanize(item.name)}
        extraContent={this.drawer}
      />
    );
  }
}
