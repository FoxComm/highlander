
/* @flow */

import _ from 'lodash';
import React, { PropTypes, Element, Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import DropdownItem from './dropdownItem';
import Overlay from '../overlay/overlay';

type Props = {
  name: string,
  value?: string|number,
  className?: string,
  listClassName?: string,
  placeholder?: string,
  open?: bool,
  children?: Element,
  primary?: bool,
  editable?: bool,
  changeable?: bool,
  disabled?: bool,
  renderDropdownInput?: Function,
  renderNullTitle?: Function,
  renderPrepend?: Function,
  onChange: Function,
};

type State = {
  open: bool,
  dropup: bool,
  selectedValue: number|string,
};

export default class GenericDropdown extends Component {

  props: Props;
  state: State;

  static defaultProps = {
    placeholder: '- Select -',
    changeable: true,
    disabled: false,
    primary: false,
    editable: false,
  };

  state: State = {
   open: !!this.props.open,
   dropup: false,
   selectedValue: '',
  };

  componentWillReceiveProps(newProps: Props) {
    this.setState({
      selectedValue: newProps.value,
    });
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (this.state.open && !prevState.open) {
      this.setMenuOrientation();
    }
  }

  setMenuOrientation() {
    const menuNode = this.refs.items;
    const containerNode = this.refs.container;
    const viewportHeight = window.innerHeight;

    const containerPos = containerNode.getBoundingClientRect();
    const spaceAtTop = containerPos.top;
    const spaceAtBottom = viewportHeight - containerPos.bottom;

    let dropup = false;

    if (!menuNode) {
      if (spaceAtBottom < viewportHeight / 2) dropup = true;
    } else {
      const menuRect = menuNode.getBoundingClientRect();
      if (spaceAtBottom < menuRect.height && spaceAtBottom < spaceAtTop) {
        dropup = true;
      }
    }

    this.setState({
      dropup,
    });
  }

  renderNullTitle(value: ?number|string, placeholder: ?string) {
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
      const item = _.findWhere(React.Children.toArray(props.children), { props: { value: value } });
      return item && item.props.children;
    }
  }

  get dropdownClassName(): string {
    const { primary, editable, children, disabled } = this.props;
    const { open, dropup } = this.state;
    const className = classNames(this.props.className, {
      'fc-dropdown': true,
      '_primary': primary,
      '_editable': editable,
      '_open': open,
      '_disabled': disabled
    });
    return className;
  }

  get listClassName(): string {
    const { dropup } = this.state;
    return classNames('fc-dropdown__items', {
      '_dropup': dropup,
      '_dropdown': !dropup,
    });
  }

  get dropdownButton(): Element {
    const className = this.state.open ? 'icon-chevron-up' : 'icon-chevron-down';
    return (
      <div className="fc-dropdown__button"
           disabled={this.props.disabled}
           onClick={this.handleToggleClick}>
        <i className={className}></i>
      </div>
    );
  }

  get dropdownInput(): Element {
    const { name, value, placeholder, renderDropdownInput, editable, disabled } = this.props;
    const actualValue = this.state.selectedValue || value;
    const title = this.findTitleByValue(actualValue, this.props) || this.renderNullTitle(value, placeholder);

    return renderDropdownInput
      ? renderDropdownInput(name, actualValue, placeholder, title, editable, disabled, this.handleToggleClick)
      : (
        <div className="fc-dropdown__value" onClick={this.handleToggleClick}>
          {title}
          <input name={name} type="hidden" value={actualValue} readOnly />
        </div>
      );
  }

  get prependList(): ?Element {
    if (!this.props.renderPrepend) {
      return null;
    }
    return this.props.renderPrepend();
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
    const { children } = this.props;

    return React.Children.map(children, item => {
      if (item.type !== DropdownItem) {
        return item;
      }

      return React.cloneElement(item, {
        onSelect: this.handleItemClick,
      });
    });
  }

  render() {
    const { name, editable } = this.props;
    return (
      <div>
        <Overlay shown={this.state.open} onClick={this.handleToggleClick} />
        <div className={this.dropdownClassName} ref="container" tabIndex="0">
          <div className="fc-dropdown__controls" onClick={editable ? this.handleToggleClick : null}>
            {this.dropdownButton}
            {this.dropdownInput}
          </div>
          <div className={this.listClassName}>
            {this.prependList}
            <ul ref="items" className={this.optionsContainerClass}>
              {this.renderItems()}
            </ul>
          </div>
        </div>
      </div>
    );
  }

}
