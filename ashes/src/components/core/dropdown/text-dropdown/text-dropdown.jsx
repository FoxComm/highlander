/* @flow */

// libs
import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import Icon from 'components/core/icon';
import { SmartList } from 'components/core/dropdown';

// styles
import s from './text-dropdown.css';

type Item = {
  value: string;
  displayText?: string;
};

type Props = {
  /** Input name which will be used by form */
  name?: string, // input name
  /** Input value */
  value?: string, // input value
  /** Text which is visible when no value */
  placeholder?: string,
  /** Additional root className */
  className?: string,
  /** An array of all possible values which will be in a list */
  items?: Array<Item>,
  /** If true, you cant open dropdown or change its value from UI */
  disabled?: bool,
  /** Callback which fires when the value has been changes */
  onChange?: Function,
  /** Goes to `bodyPortal`, e.g. case with overflow `/customers/10/storecredit` */
  detached?: boolean,
  /** If true, the component can change its value only via props */
  stateless?: boolean,
};

type State = {
  open: bool, // show or hide the menu
  selectedValue: string, // current selected value of menu
};

/**
 * Simple Dropdown component
 *
 * WARNING: It's important to implement shouldComponentUpdate hook in host components
 */
export default class TextDropdown extends Component {
  props: Props;

  static defaultProps = {
    name: '',
    value: '',
    placeholder: 'Select value',
    disabled: false,
    detached: false,
    onChange: () => {},
    stateless: false,
  };

  state: State = {
    open: false,
    selectedValue: this.props.value,
  };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.value !== this.props.value) {
      this.setState({ selectedValue: nextProps.value });
    }
  }

  @autobind
  handleToggleClick(event: any) {
    event.preventDefault();

    if (this.props.disabled) {
      return;
    }

    this.toggleMenu();
  }

  handleItemClick(item) {
    const { stateless } = this.props;
    let nextState = { open: false };

    if (item.value !== this.state.selectedValue) {
      if (!stateless) {
        nextState.selectedValue = item.value;
      }

      this.props.onChange(item);
    }

    this.setState(nextState);
  }

  toggleMenu() {
    this.setState({ open: !this.state.open });
  }

  closeMenu() {
    this.setState({ open: false });
  }

  get displayText() {
    const { items, placeholder } = this.props;
    const item = _.find(items, item => item.value === this.state.selectedValue);

    return (item && item.displayText) || this.state.selectedValue || placeholder;
  }

  renderItems() {
    const { items, detached } = this.props;

    return (
      <SmartList className={s.menu} onEsc={() => this.closeMenu()} detached={detached}>
        {items.map(item => (
          <div key={item.value} className={s.item} onClick={() => this.handleItemClick(item)}>
            {item.displayText || item.value}
          </div>
        ))}
      </SmartList>
    );
  }

  get menu(): ?Element<any> {
    if (!this.state.open) {
      return;
    }

    return this.renderItems();
  }

  render() {
    const { disabled, name, placeholder, className } = this.props;
    const { selectedValue, open } = this.state;
    const cls = classNames(s.block, className, {
      [s.disabled]: disabled,
      [s.open]: open,
    });
    const arrow = this.state.open ? 'chevron-up' : 'chevron-down';

    return (
      <div className={cls} tabIndex="0">
        <div className={s.pivot} onClick={this.handleToggleClick}>
          <div className={s.displayText}>{this.displayText}</div>
          <Icon name={arrow} />
          <input type="hidden" name={name} value={this.state.selectedValue} />
        </div>
        {this.menu}
      </div>
    );
  }
}
