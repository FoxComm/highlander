/* @flow */

import _ from 'lodash';
import React, { PropTypes, Element, Component, Children } from 'react';
import createFragment from 'react-addons-create-fragment';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import DropdownItem from './dropdownItem';
import Overlay from '../overlay/overlay';
import { Button } from '../common/buttons';
import BodyPortal from '../body-portal/body-portal';

export type ValueType = ?string|number;

export type DropdownItemType = [ValueType, string|Element, bool];

export type Props = {
  name?: string,
  value: ValueType,
  className?: string,
  listClassName?: string,
  placeholder?: string,
  emptyMessage?: string|Element,
  open?: bool,
  children?: Element,
  items?: Array<DropdownItemType>,
  primary?: bool,
  editable?: bool,
  changeable?: bool,
  disabled?: bool,
  inputFirst?: bool,
  renderDropdownInput?: Function,
  renderNullTitle?: Function,
  renderPrepend?: Function,
  renderAppend?: Function,
  onChange?: (value: any, title: string) => void,
  dropdownProps?: Object,
  detached?: boolean,
};

type State = {
  open: bool,
  dropup: bool,
  selectedValue: ValueType,
};

/**
 * Generic Dropdown component
 *
 * WARNING: It's important to implement shouldComponentUpdate hook in host components
 */
export default class GenericDropdown extends Component {
  props: Props;

  static defaultProps = {
    placeholder: '- Select -',
    changeable: true,
    disabled: false,
    primary: false,
    editable: false,
    inputFirst: true,
    dropdownProps: {},
    value: '',
    detached: false,
  };

  state: State = {
    open: !!this.props.open,
    dropup: false,
    selectedValue: this.props.value,
  };

  _menu: HTMLElement;
  _container: HTMLElement;

  componentWillReceiveProps(newProps: Props) {
    this.setState({
      selectedValue: newProps.value,
    });
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (this.state.open && !prevState.open) {
      this.setMenuPosition();
      this.setMenuOrientation();
    }
  }

  setMenuPosition() {
    if (!this.props.detached) {
      return;
    }

    const parentDim = this._container.getBoundingClientRect();

    this._menu.style.minWidth = `${this._container.offsetWidth}px`;
    this._menu.style.top = `${parentDim.top + parentDim.height + window.scrollY}px`;
    this._menu.style.left = `${parentDim.left}px`;
  }

  setMenuOrientation() {
    const viewportHeight = window.innerHeight;

    const containerPos = this._container.getBoundingClientRect();
    const spaceAtTop = containerPos.top;
    const spaceAtBottom = viewportHeight - containerPos.bottom;

    let dropup = false;

    if (!this._menu) {
      if (spaceAtBottom < viewportHeight / 2) dropup = true;
    } else {
      const menuRect = this._menu.getBoundingClientRect();
      if (spaceAtBottom < menuRect.height && spaceAtBottom < spaceAtTop) {
        dropup = true;
      }
    }

    this.setState({
      dropup,
    });
  }

  renderNullTitle(value: ?number|string, placeholder: ?string): ?string|Element {
    if (this.props.renderNullTitle) {
      return this.props.renderNullTitle(value, placeholder);
    }

    return placeholder;
  }

  findTitleByValue(value: ?string|number, props: Props): string {
    if (props.items) {
      const item = _.find(props.items, item => item[0] == value);
      return item && item[1];
    } else {
      const item = _.find(React.Children.toArray(props.children), { props: { value: value } });
      return item && item.props.children;
    }
  }

  get dropdownClassName(): string {
    const { primary, editable, disabled, className } = this.props;

    return classNames(className, 'fc-dropdown', {
      '_primary': primary,
      '_editable': editable,
      '_disabled': disabled,
    });
  }

  get listClassName(): string {
    const { open, dropup } = this.state;
    return classNames('fc-dropdown__items', {
      '_open': open,
      '_dropup': dropup,
      '_dropdown': !dropup,
    });
  }

  get dropdownButton(): Element {
    const icon = this.state.open ? 'chevron-up' : 'chevron-down';
    return (
      <Button
        icon={icon}
        docked={this.props.inputFirst ? 'right' : 'left'}
        className="_dropdown-size"
        disabled={this.props.disabled}
        onClick={this.handleToggleClick}
        {...this.props.dropdownProps}
      />
    );
  }

  get dropdownInput(): Element {
    const { name, placeholder, value, renderDropdownInput } = this.props;
    const actualValue = this.state.selectedValue;
    const title = this.findTitleByValue(actualValue, this.props) || this.renderNullTitle(value, placeholder);
    const valueForInput = actualValue === null ? '' : actualValue;

    return renderDropdownInput
      ? renderDropdownInput(actualValue, title, this.props, this.handleToggleClick)
      : (
      <div className="fc-dropdown__value" onClick={this.handleToggleClick}>
        {title}
        <input name={name} type="hidden" value={valueForInput} readOnly />
      </div>
    );
  }

  get prependList(): ?Element {
    if (!this.props.renderPrepend) {
      return null;
    }
    return this.props.renderPrepend();
  }

  get appendList(): ?Element {
    if (!this.props.renderAppend) {
      return null;
    }
    return this.props.renderAppend();
  }

  get optionsContainerClass(): string {
    return classNames('fc-dropdown__item-container', this.props.listClassName);
  }

  @autobind
  handleToggleClick(event: any) {
    event.preventDefault();
    if (this.props.disabled) {
      return;
    }
    this.setState({
      open: !this.state.open
    });
  }

  @autobind
  handleItemClick(value: number|string, title: string) {
    let state = { open: false };
    if (this.props.changeable) {
      state = { ...state, selectedValue: value };
    }

    this.setState(state, () => {
      if (this.props.onChange) {
        this.props.onChange(value, title);
      }
    });
  }

  @autobind
  closeMenu() {
    this.setState({ open: false });
  }

  @autobind
  renderItems(): Element {
    const { children, emptyMessage } = this.props;

    if (_.isEmpty(children) && emptyMessage) {
      return (
        <li className="fc-dropdown__blank-item" onClick={this.closeMenu}>
          {emptyMessage}
        </li>
      );
    }

    return React.Children.map(children, item => {
      if (item.type !== DropdownItem) {
        return item;
      }

      return React.cloneElement(item, {
        onSelect: this.handleItemClick,
      });
    });
  }

  get controls(): Element[] {
    const { inputFirst } = this.props;

    return createFragment({
      left: inputFirst ? this.dropdownInput : this.dropdownButton,
      right: inputFirst ? this.dropdownButton : this.dropdownInput,
    });
  }

  get menu(): ?Element {
    if (!this.state.open) {
      return;
    }

    return (
      <BodyPortal active={this.props.detached}>
        <div className={this.listClassName} ref={m => this._menu = m}>
          {this.prependList}
          <ul className={this.optionsContainerClass}>
            {this.renderItems()}
          </ul>
          {this.appendList}
        </div>
      </BodyPortal>
    );
  };

  render() {
    const { editable } = this.props;

    return (
      <div className={this.dropdownClassName} ref={c => this._container = c} tabIndex="0">
        <Overlay shown={this.state.open} onClick={this.handleToggleClick} />
        <div className="fc-dropdown__controls" onClick={editable ? this.handleToggleClick : null}>
          {this.controls}
        </div>
        {this.menu}
      </div>
    );
  }
}
