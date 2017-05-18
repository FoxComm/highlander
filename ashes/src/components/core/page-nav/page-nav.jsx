/* flow */

// libs
import classNames from 'classnames';
import { last } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element as ReactElement } from 'react';
import { withRouter } from 'react-router';

// helpers
import { addResizeListener, removeResizeListener } from 'lib/resize';

// components
import NavDropdown from './page-nav-dropdown';

// styles
import s from './page-nav.css';

type Props = {
  /** List of links */
    children: Array<ReactElement<any>>,
};

/**
 * Object page top navigation
 *
 * [Mockups](https://zpl.io/1p2SEW)
 *
 * @class PageNav
 */
class PageNav extends Component {
  props: Props;

  _nav;

  _itemsRightEdges = [];

  _itemsSumWidth = 0;

  componentDidMount() {
    this._itemsRightEdges = this.itemElements.map(item => item.offsetLeft + item.offsetWidth);
    this._itemsSumWidth = last(this._itemsRightEdges);

    addResizeListener(this.handleResize);
    this.handleResize();
  }

  componentWillUnmount() {
    removeResizeListener(this.handleResize);
  }

  @autobind
  handleResize() {
    const items = React.Children.count(this.props.children);

    if (items.length < 3) {
      return;
    }

    this.forceUpdate();
  }

  @autobind
  renderItem(item, index) {
    const key = `page-nav-item-${item.key ? item.key : index}`;

    if (item.type !== NavDropdown) {
      const child = React.cloneElement(item, {
        activeClassName: s.activeLink,
      });

      return <li className={s.item} key={key}>{child}</li>;
    }

    return item;
  }

  get itemElements() {
    const items = this._nav.getElementsByTagName('li');

    return Array.prototype.slice.call(items, 0);
  }

  get collapseFrom() {
    const itemsCount = React.Children.count(this.props.children);

    if (itemsCount < 3) {
      return itemsCount;
    }

    // find element who's right side is further then nav width
    const from = this._itemsRightEdges.findIndex(right => right > this._nav.offsetWidth);

    // If all items fit inside parent return number of children
    if (from === -1) {
      return React.Children.count(this.props.children);
    }

    // If first or second item do not fit, collapse from second (always show first item)
    if (from <= 1) {
      return 1;
    }

    // Otherwise collapse on previous item (otherwise dropdown label can overflow parent)
    return from - 1;
  }

  get items() {
    const children = React.Children.toArray(this.props.children);
    const items = children.map(this.renderItem);
    const from = this.collapseFrom;
    const flatItems = items.slice(0, from);
    const collapsedItems = items.slice(from);

    const menu = !collapsedItems.length ? null : (
      <NavDropdown title="More" key="dropdown">
        {collapsedItems}
      </NavDropdown>
    );

    return [
      ...flatItems,
      menu,
    ];
  }


  render() {
    return (
      <ul className={classNames(s.block, this.props.className)} ref={n => this._nav = n}>
        {this.items}
      </ul>
    );
  }
}

export default withRouter(PageNav);
