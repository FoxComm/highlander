import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './dropdown.css';
import cx from 'classnames';

type DropdownItem = {
  id: int,
  title: string
};

export default class Dropdown extends Component {
  props: {
    items: Array<DropdownItem>
  }

  constructor() {
    super();
    this.state = {
      open: false,
    };
  }

  @autobind
  toggleDropdown () {
    this.setState({ open: !this.state.open });
  }

  render () {
    const cls = cx(styles.selector, {
      [styles.open]: this.state.open,
    });

    return (
      <div styleName="dropdown">
        <div className={cls} onClick={this.toggleDropdown}>All</div>

        {this.state.open &&
          <div styleName="items">
            <div styleName="items-wrap">
              <div className={cx(styles.item, styles.active)}>All</div>
              <div styleName="item">Poultry</div>
              <div styleName="item">Seafood</div>
              <div styleName="item">Beef</div>
              <div styleName="item">Vegitarian</div>
            </div>
          </div>}
      </div>
    );
  }
}
