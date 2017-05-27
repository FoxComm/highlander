/* @flow */

// libs
import classNames from 'classnames';
import { map, noop } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import { PrimaryButton } from 'components/core/button';
import { DropdownItem } from 'components/dropdown';

// styles
import s from './button-with-menu.css';

type DropdownItemType = [any, string | Element<any>];

type Props = {
  /** Primary button label */
  title: string | Element<any>;
  /** Menu items array */
  items: Array<DropdownItemType>;
  /** If primary button is disabled */
  buttonDisabled?: boolean;
  /** If menu button is disabled */
  menuDisabled?: boolean;
  /** Icon name that is used to be rendered in a primary button */
  icon?: string;
  /** If to show loading animation */
  isLoading?: boolean;
  /** Additional className */
  className?: string;
  /** Action button className */
  buttonClassName?: string;
  /** Menu button className */
  menuClassName?: string;
  /** Callback called on primary button click */
  onPrimaryClick?: Function;
  /** Callback called on menu item click */
  onSelect?: (value: any, title: string | Element<any>) => any;
}

type State = {
  open: boolean;
}

const transitionProps = {
  component: 'div',
  transitionName: `dd-transition-right`,
  transitionEnterTimeout: 300,
  transitionLeaveTimeout: 300,
};

/**
 * Button component that represents a button with additional action in a dropdown menu.
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @class ButtonWithMenu
 */
export default class ButtonWithMenu extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    items: [],
    buttonDisabled: false,
    menuDisabled: false,
    icon: '',
    className: '',
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
  handleItemClick(value: any, title: string | Element<any>) {
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
    const { items } = this.props;
    const { open } = this.state;

    if (!open) {
      return;
    }

    const ddItems = map(items, ([value, title]) => (
      <DropdownItem value={value} onSelect={this.handleItemClick} key={value}>
        {title}
      </DropdownItem>
    ));

    return (
      <ul className={s.menu}>
        { ddItems }
      </ul>
    );
  }

  render() {
    const {
      icon,
      title,
      buttonDisabled,
      menuDisabled,
      className,
      buttonClassName,
      menuClassName,
      onPrimaryClick,
      isLoading
    } = this.props;

    const { open } = this.state;

    const cls = classNames(s.button, {
      [s.opened]: open,
    }, className);

    const actionButtonClassName = classNames(s.actionButton, buttonClassName);

    const menuButtonClassName = classNames(s.dropdownButton, menuClassName);

    return (
      <div className={cls} onBlur={this.handleBlur} tabIndex="0">
        { open && <div className={s.overlay} onClick={this.handleBlur}></div> }
        <div className={s.controls}>
          <PrimaryButton
            id="fct-primary-save-btn"
            className={actionButtonClassName}
            icon={icon}
            onClick={onPrimaryClick}
            isLoading={isLoading}
            onBlur={this.dontPropagate}
            disabled={buttonDisabled}>
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

        <Transition {...transitionProps}>
          {this.menu}
        </Transition>
      </div>
    );
  }
}
