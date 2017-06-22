/* @flow */

// libs
import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import Icon from 'components/core/icon';
import { SmartList } from 'components/core/dropdown';

// styles
import s from './search-dropdown.css';

type InternalItem = {
  value: string,
  displayText?: string,
};

type Item = [string, string];

type Props = {
  /** An array of all possible values which will be in a list */
  // $FlowFixMe
  items: Array<Item | InternalItem | string>,
  /** Input name which will be used by form */
  name: string, // input name
  /** Input value */
  value: string | number | null, // input value
  /** Text which is visible when no value */
  placeholder: string,
  /** Additional root className */
  className?: string,
  /** If true, you cant open dropdown or change its value from UI */
  disabled: boolean,
  /** If true, the component can change its value only via props */
  stateless: boolean,
  /** Callback which fires when the value has been changes */
  onChange: Function,
};

type State = {
  open: boolean, // show or hide the menu
  selectedValue: string, // current selected value of menu
};

/**
 * Text Dropdown component.
 * This component is about to render simple text list and current value.
 * This component is not responsible for different skins, TextInputs, infinite lists, or any other complex stuff.
 * If you need any functionality which is not exists here, try different Dropdown or build new one.
 */
export default class SearchDropdown extends Component {
  props: Props;

  static defaultProps = {
    name: '',
    value: '',
    placeholder: '- Select -',
    disabled: false,
    onChange: () => {},
    stateless: false,
    items: [],
  };

  state: State = {
    open: false,
    selectedValue: this.getValue(this.props.value),
  };

  _pivot: HTMLElement;

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.value !== this.props.value) {
      this.setState({ selectedValue: this.getValue(nextProps.value) });
    }
  }

  getValue(value: any) {
    return value ? String(value) : '';
  }

  @autobind
  handleToggleClick(event: any) {
    event.preventDefault();

    if (this.props.disabled) {
      return;
    }

    this.toggleMenu();
  }

  handleItemClick(item: InternalItem) {
    const { stateless } = this.props;
    let nextState = { open: false, selectedValue: this.state.selectedValue };

    if (item.value !== this.state.selectedValue) {
      if (!stateless) {
        nextState.selectedValue = item.value;
      }

      this.props.onChange(item.value);
    }

    this.setState(nextState);
  }

  toggleMenu(nextOpen: ?boolean) {
    const open = nextOpen != null ? nextOpen : !this.state.open;

    this.setState({ open });
  }

  get displayText(): string {
    const { placeholder } = this.props;
    const item = _.find(this.items, item => item.value == this.state.selectedValue); // could be number == string

    return (item && item.displayText) || this.state.selectedValue || placeholder;
  }

  get items(): Array<InternalItem> {
    const { items } = this.props;

    if (Array.isArray(items[0])) {
      return items.map(([value, displayText]) => ({ value, displayText }));
    } else if (typeof items[0] === 'string') {
      return items.map((value: string) => ({ value, displayText: value }));
    }

    return items;
  }

  renderItems() {
    let list = this.items.map(item =>
      <div key={item.value} className={s.item} onClick={() => this.handleItemClick(item)}>
        {item.displayText || item.value}
      </div>
    );

    return (
      <SmartList className={s.menu} onEsc={() => this.toggleMenu(false)} pivot={this._pivot}>
        {list}
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
      [s.empty]: !this.items.length,
    });
    const arrow = this.state.open ? 'chevron-up' : 'chevron-down';

    return (
      <div className={cls} tabIndex="0">
        <div className={s.pivot} ref={p => (this._pivot = p)} onClick={this.handleToggleClick}>
          <div className={s.displayText}>{this.displayText}</div>
          <Icon name={arrow} />
          <input type="hidden" name={name} value={this.state.selectedValue} />
        </div>
        {this.menu}
      </div>
    );
  }
}
