/* @flow */

// libs
import _ from 'lodash';
import React, { Element, Component } from 'react';
import createFragment from 'react-addons-create-fragment';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import DropdownItem from './dropdownItem';
import { Button } from 'components/core/button';
import BodyPortal from '../body-portal/body-portal';

// styles
import s from './generic-dropdown.css';

export type ValueType = ?string | number;

export type DropdownItemType = [ValueType, string | Element<*>, ?boolean];

export type MouseHandler = (e: MouseEvent) => void;

export type RenderDropdownFunction = (
  value: any, title: ?string | Element<*>, props: Props, handleToggleClick: MouseHandler
) => Element<*>;

export type Props = {
  id?: string,
  dropdownValueId?: string,
  name: string,
  value: ValueType,
  className?: string,
  listClassName?: string,
  placeholder?: string | Element<*>,
  emptyMessage?: string | Element<*>,
  open?: bool,
  children?: Element<*>,
  items?: Array<any>,
  primary?: bool,
  editable?: bool,
  changeable?: bool,
  disabled?: bool,
  inputFirst?: bool,
  renderDropdownInput?: RenderDropdownFunction,
  renderNullTitle?: Function,
  renderPrepend?: Function,
  renderAppend?: Function,
  onChange?: Function,
  dropdownProps?: Object,
  detached?: boolean,
  noControls?: boolean,
  toggleColumnsBtn?: boolean,
  buttonClassName?: string,
};

type State = {
  open: bool,
  dropup: bool,
  selectedValue: ValueType,
  pointedValueIndex: number,
};

function getNewItemIndex(itemsCount, currentIndex, increment = 1) {
  const startIndex = increment > 0 ? -1 : 0;
  const index = Math.max(currentIndex, startIndex);

  return (itemsCount + index + increment) % itemsCount;
}

/**
 * Generic Dropdown component
 *
 * WARNING: It's important to implement shouldComponentUpdate hook in host components
 */
export default class GenericDropdown extends Component {
  props: Props;

  static defaultProps = {
    placeholder: '',
    changeable: true,
    disabled: false,
    primary: false,
    editable: false,
    inputFirst: true,
    dropdownProps: {},
    name: '',
    value: '',
    detached: false,
    noControls: false,
  };

  state: State = {
    open: !!this.props.open,
    dropup: false,
    selectedValue: this.props.value,
    pointedValueIndex: -1,
  };

  _menu: HTMLElement;
  _items: HTMLElement;
  _block: HTMLElement;

  componentDidMount() {
    window.addEventListener('keydown', this.handleKeyPress, true);
    window.addEventListener('click', this.handleClickOutside, true);
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress, true);
    window.removeEventListener('click', this.handleClickOutside, true);
  }

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

  @autobind
  handleClickOutside({ target }: { target: HTMLElement }) {
    if (this._block && !this._block.contains(target) && this.state.open) {
      this.closeMenu();
    }
  }

  setMenuPosition() {
    if (!this.props.detached) {
      return;
    }

    const parentDim = this._block.getBoundingClientRect();

    this._menu.style.minWidth = `${this._block.offsetWidth}px`;
    this._menu.style.top = `${parentDim.top + parentDim.height + window.scrollY}px`;
    this._menu.style.left = `${parentDim.left}px`;
  }

  setMenuOrientation() {
    const viewportHeight = window.innerHeight;

    const containerPos = this._block.getBoundingClientRect();
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

  renderNullTitle(value: ?number | string, placeholder: ?string | Element<*>): ?string | Element<*> {
    if (this.props.renderNullTitle) {
      return this.props.renderNullTitle(value, placeholder);
    }

    return placeholder;
  }

  findTitleByValue(value: ?string | number, props: Props): string {
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

  get dropdownButton() {
    const icon = this.state.open ? 'chevron-up' : 'chevron-down';
    const { toggleColumnsBtn } = this.props;

    const className = classNames(s.downArrowBtn, this.props.buttonClassName, {
      [s.toggleBtn]: toggleColumnsBtn != null,
    });
    // @todo consider to not use <Button> component here, too specific styles
    return (
      <Button
        icon={icon}
        className={className}
        disabled={this.props.disabled}
        onClick={this.handleToggleClick}
        {...this.props.dropdownProps}
      />
    );
  }

  get dropdownInput(): Element<*> {
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

  get prependList(): ?Element<*> {
    if (!this.props.renderPrepend) {
      return null;
    }
    return this.props.renderPrepend();
  }

  get appendList(): ?Element<*> {
    if (!this.props.renderAppend) {
      return null;
    }
    return this.props.renderAppend(this.handleToggleClick);
  }

  get optionsContainerClass(): string {
    return classNames('fc-dropdown__item-container', this.props.listClassName);
  }

  @autobind
  scrollViewport(movingUp: boolean = false) {
    const newIndex = this.state.pointedValueIndex;
    const item = this._items.children[newIndex];

    const containerTop = this._items.scrollTop;
    const containerVisibleHeight = this._items.clientHeight;
    const itemTop = item.offsetTop;
    const itemHeight = item.offsetHeight;

    // shift height when compare to viewport top position - item height if moving up, zero otherwise
    const heightShift = movingUp ? itemHeight : 0;

    const elementBelowViewport = containerTop + containerVisibleHeight <= itemTop + itemHeight;
    const elementAboveViewport = containerTop > itemTop + heightShift;

    if (elementBelowViewport) {
      this._items.scrollTop = itemTop + itemHeight - containerVisibleHeight;
    }
    if (elementAboveViewport) {
      this._items.scrollTop = itemTop;
    }
  }

  @autobind
  handleKeyPress(e: KeyboardEvent) {
    const { open, pointedValueIndex: currentIndex } = this.state;

    if (open) {
      const itemsCount = React.Children.count(this.props.children);

      switch (e.keyCode) {
        // enter
        case 13:
          e.stopPropagation();
          e.preventDefault();

          if (currentIndex > -1) {
            this._items.children[currentIndex].click();
          }

          break;
        // esc
        case 27:
          this.setState({ open: false, pointedValueIndex: -1 });

          break;
        // up
        case 38:
          e.preventDefault();

          this.setState({
            pointedValueIndex: getNewItemIndex(itemsCount, currentIndex, -1),
          }, this.scrollViewport.bind(this, true));

          break;
        // down
        case 40:
          e.preventDefault();

          this.setState({
            pointedValueIndex: getNewItemIndex(itemsCount, currentIndex),
          }, this.scrollViewport);

          break;
      }
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

  @autobind
  handleItemClick(value: number | string, title: string) {
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
  toggleMenu() {
    this.setState({ open: !this.state.open, pointedValueIndex: -1 });
  }

  @autobind
  closeMenu() {
    this.setState({ open: false, pointedValueIndex: -1 });
  }

  @autobind
  openMenu() {
    this.setState({ open: true });
  }

  @autobind
  renderItems() {
    const { children, emptyMessage } = this.props;

    if (_.isEmpty(children) && emptyMessage) {
      return (
        <li className="fc-dropdown__blank-item" onClick={this.closeMenu}>
          {emptyMessage}
        </li>
      );
    }

    return React.Children.map(children, (item, index) => {
      const className = classNames('fc-dropdown__item', { _active: index === this.state.pointedValueIndex });

      const props: any = {
        className,
      };

      if (item.type === DropdownItem) {
        props.onSelect = this.handleItemClick;
      }

      return React.cloneElement(item, props);
    });
  }

  get controls(): Element<*> {
    const { inputFirst, noControls, placeholder } = this.props;

    if (noControls) {
      return this.dropdownInput;
    }
    const rightInput = inputFirst ? this.dropdownButton : this.dropdownInput;

    return createFragment({
      left: inputFirst ? this.dropdownInput : this.dropdownButton,
      right: placeholder ? rightInput : null,
    });
  }

  get menu(): ?Element<*> {
    if (!this.state.open) {
      return;
    }

    return (
      <BodyPortal active={this.props.detached}>
        <div className={this.listClassName} ref={m => this._menu = m}>
          {this.prependList}
          <ul className={this.optionsContainerClass} ref={i => this._items = i}>
            {this.renderItems()}
          </ul>
          {this.appendList}
        </div>
      </BodyPortal>
    );
  }

  render() {
    const { editable, id } = this.props;

    const cls = classNames(s.controls, 'fc-dropdown__controls');

    return (
      <div id={id} className={this.dropdownClassName} ref={c => this._block = c} tabIndex="0">
        <div className={cls} onClick={editable ? this.handleToggleClick : null}>
          {this.controls}
        </div>
        {this.menu}
      </div>
    );
  }
}
