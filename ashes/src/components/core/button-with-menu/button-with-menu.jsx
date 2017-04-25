/* @flow */

// libs
import classNames from 'classnames';
import { isEmpty, map, noop } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import { PrimaryButton } from 'components/core/button';
import { DropdownItem } from 'components/dropdown';

// styles
import s from './button-with-menu.css';

type DropdownItemType = [any, string|Element<any>];

type Props = {
  /** Primary button label */
  title: string|Element<any>;
  /** Menu items array */
  items?: Array<DropdownItemType>;
  /** Dropdown menu position. Affects animation start position (css's transform-origin) */
  menuPosition?: "left" | "center" | "right";
  /** If primary button is disabled */
  buttonDisabled?: boolean;
  /** If menu button is disabled */
  menuDisabled?: boolean;
  /** Icon name that is used to be rendered in a primary button */
  icon?: string;
  /** If to animate menu appearance */
  animate?: boolean;
  /** If to show loading animation */
  isLoading?: boolean;
  /** Additional className */
  className?: string;
  /** Callback called on primary button click */
  onPrimaryClick?: Function;
  /** Callback called on menu item click */
  onSelect?: (value: any, title: string|Element<any>) => any;
  /** Array of elements used to render menu items in case `items` prop is empty */
  children?: Array<Element<any>>;
};

type State = {
  open: boolean;
};

/**
 * Button component that represents a button with additional action in a dropdown menu.
 *
 * @class ButtonWithMenu
 */
export default class ButtonWithMenu extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    items: [],
    menuPosition: 'right',
    buttonDisabled: false,
    menuDisabled: false,
    icon: '',
    className: '',
    animate: true,
    isLoading: false,
    onPrimaryClick: noop,
    onSelect: noop,
  };

  state: State = {
    open: false,
  };

  @autobind
  handleToggleClick(event: SyntheticEvent) {
    event.preventDefault();
    this.setState({
      open: !this.state.open,
    });
  }

  @autobind
  handleItemClick(value: any, title: string|Element<any>) {
    const newState = { open: false };

    this.setState(newState, () => {
      if (this.props.onSelect) {
        this.props.onSelect(value, title);
      }
    });
  }

  @autobind
  handleBlur() {
    this.setState({
      open: false,
    });
  }

  dontPropagate(event: SyntheticEvent) {
    event.stopPropagation();
  }

  get menu() {
    const { children, items } = this.props;
    const { open } = this.state;

    if (!open) {
      return;
    }

    let ddItems = null;

    if (!isEmpty(items)) {
      ddItems = map(items, ([value, title]) => (
        <DropdownItem value={value} key={value} onSelect={this.handleItemClick}>
          {title}
        </DropdownItem>
      ));
    } else {
      ddItems = React.Children.map(children, item =>
        React.cloneElement(item, {
          onSelect: this.handleItemClick,
        })
      );
    }
    return (
      <ul className={s.menu} ref="menu">
        { ddItems }
      </ul>
    );
  }

  render() {
    const { props } = this;
    const { icon, title, animate, menuPosition, buttonDisabled, menuDisabled } = props;
    const { open } = this.state;

    const className = classNames(s.button, this.props.className, {
      [s.opened]: open,
    });

    const buttonClassName = classNames('fc-button-with-menu__left-button', {
      [s._disabled]: buttonDisabled,
    });

    const menuButtonClassName = classNames(s.dropdownButton, 'fc-button-with-menu__right-button', {
      [s._disabled]: menuDisabled,
    });

    return (
      <div className={className} onBlur={this.handleBlur} tabIndex="0">
        { open && <div className={s.overlay} onClick={this.handleBlur}></div> }
        <div className={s.controls}>
          <PrimaryButton
            id="fct-primary-save-btn"
            className={buttonClassName}
            icon={icon}
            onClick={props.onPrimaryClick}
            isLoading={props.isLoading}
            onBlur={this.dontPropagate}
            disabled={buttonDisabled} >
            {title}
          </PrimaryButton>
          <PrimaryButton
            id="fct-primary-save-menu-btn"
            className={menuButtonClassName}
            icon="chevron-down"
            onClick={this.handleToggleClick}
            onBlur={this.dontPropagate}
            disabled={menuDisabled}
          />
        </div>

        <Transition {...(getTransitionProps(animate, menuPosition))}>
          {this.menu}
        </Transition>
      </div>
    );
  }
}

function getTransitionProps(animate, position = 'right') {
  return {
    component: 'div',
    transitionName: `dd-transition-${position}`,
    transitionEnter: animate,
    transitionLeave: animate,
    transitionEnterTimeout: 300,
    transitionLeaveTimeout: 300,
  };
}
