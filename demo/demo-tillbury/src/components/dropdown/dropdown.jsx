/* @flow */

import React, { Component, Element } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';

import Icon from 'ui/icon';

import styles from './dropdown.css';

type Item = {
  onSelect: () => void,
  component: string|Element<*>,
};
type Props = {
  items: Array<Item>,
  className?: string,
};
type State = {
  selected: Item,
  isOpen: boolean,
};

class Dropdown extends Component {
  props: Props;
  state: State = {
    selected: this.props.items[0],
    isOpen: false,
  };

  @autobind
  onItemSelect(item: Item) {
    this.setState({ selected: item, isOpen: false }, () => {
      item.onSelect();
    });
  }

  @autobind
  openDropdown() {
    this.setState({ isOpen: true });
  }

  @autobind
  closeDropdown() {
    this.setState({ isOpen: false });
  }

  get items(): Array<Element<*>> {
    return _.map(this.props.items, (item, idx) => {
      return (
        <li
          onClick={() => this.onItemSelect(item)}
          styleName="item"
          key={`${idx}-${item.component.toString}`}
        >
          {item.component}
        </li>
      );
    });
  }

  render () {
    const klass = classNames(this.props.className, styles.container);

    const dropdownClass = classNames(styles.dropdown, {
      [styles._open]: this.state.isOpen,
    });
    const overlayClass = classNames(styles.overlay, {
      [styles._open]: this.state.isOpen,
    });
    return (
      <div className={klass}>
        <div className={overlayClass} onClick={this.closeDropdown} />
        <div styleName="handle" onClick={this.openDropdown}>
          <div styleName="value">{this.state.selected.component}</div>
          <div styleName="indicator">
            <Icon name="fc-chevron-down" styleName="chevron" />
          </div>
        </div>
        <ul className={dropdownClass}>
          {this.items}
        </ul>
      </div>
    );
  }
}

export default Dropdown;
