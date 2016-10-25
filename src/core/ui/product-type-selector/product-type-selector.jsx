/* flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './product-type-selector.css';
import cx from 'classnames';
import CSSTransitionGroup from 'react-addons-css-transition-group';
import _ from 'lodash';

export default class ProductTypeSelector extends Component {
  props: {
    items: Array<string>,
    activeItem: string,
    onItemClick: Function
  }

  static defaultProps = {
    items: [],
    onClick: _.noop,
  }

  state = {
    open: false,
    activeItem: this.props.items[0],
  }

  @autobind
  toggleDropdown () {
    this.setState({ open: !this.state.open });
  }

  render () {
    const cls = cx(styles.selector, {
      [styles.open]: this.state.open,
    });

    const items =
      this.props.items.map(item => {
        const itemCls = cx(styles.item, {
          [styles.active]: item.toLowerCase() === this.props.activeItem.toLowerCase(),
        });
        const onClick = this.props.onItemClick.bind(this, item);

        return (
          <div className={itemCls} onClick={onClick} key={item}>{item}</div>
        );
      });

    return (
      <div styleName="dropdown">
        <div className={cls} onClick={this.toggleDropdown}>{this.props.activeItem}</div>

        <CSSTransitionGroup
          transitionName={styles.items}
          transitionEnterTimeout={300}
          transitionLeaveTimeout={200}
          component="div"
        >
          {this.state.open &&
            <div styleName="items">
              <div styleName="items-wrap">
                {items}
              </div>
            </div>}
        </CSSTransitionGroup>
      </div>
    );
  }
}
