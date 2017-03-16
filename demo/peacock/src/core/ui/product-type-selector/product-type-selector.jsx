/* flow */

import React, { Component } from 'react';
import styles from './product-type-selector.css';
import classNames from 'classnames';
import _ from 'lodash';

export default class ProductTypeSelector extends Component {
  props: {
    items: Array<string>,
    activeItem: string,
    onItemClick: Function
  };

  static defaultProps = {
    items: [],
    onClick: _.noop,
  }

  state = {
    activeItem: this.props.items[0],
  }

  render () {
    const items =
      this.props.items.map(item => {
        const itemCls = classNames(styles.item, {
          [styles.active]: item.toLowerCase() === this.props.activeItem.toLowerCase(),
        });
        const onClick = this.props.onItemClick.bind(this, item);

        return (
          <div className={itemCls} onClick={onClick} key={item}>{item}</div>
        );
      });

    return (
      <div styleName="dropdown">
        <div styleName="items">
          <div styleName="items-wrap">
            {items}
          </div>
        </div>
      </div>
    );
  }
}
