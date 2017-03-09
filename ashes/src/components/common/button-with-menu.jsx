
/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import Transition from 'react-addons-css-transition-group';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import styles from './button-with-menu.css';

import { PrimaryButton } from './buttons';
import { DropdownItem } from '../dropdown';

type DropdownItemType = [any, string|Element<*>];

type Props = {
  onPrimaryClick?: Function;
  onSelect?: (value: any, title: string|Element<*>) => any;
  children?: Element<*>;
  buttonDisabled?: boolean;
  menuDisabled?: boolean;
  icon?: string;
  items: Array<DropdownItemType>;
  title: string|Element<*>;
  className?: string;
  menuPosition: "left" | "center" | "right";
  animate?: boolean;
  isLoading?: boolean;
}

type State = {
  open: boolean;
}

export default class ButtonWithMenu extends Component {
  props: Props;

  static defaultProps = {
    items: [],
    animate: true,
    menuPosition: 'left',
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
  handleItemClick(value: any, title: string|Element<*>) {
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

    if (!_.isEmpty(items)) {
      ddItems = _.map(items, ([value, title]) => (
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
      <ul styleName="menu" ref="menu">
        { ddItems }
      </ul>
    );
  }

  render() {
    const { props } = this;
    const { icon, title, animate, menuPosition, buttonDisabled, menuDisabled } = props;
    const { open } = this.state;

    const className = classNames(this.props.className, {
      '_open': open,
    });
    const buttonClassName = classNames('fc-button-with-menu__left-button', {
      '_disabled': buttonDisabled,
    });
    const menuButtonClassName = classNames('fc-button-with-menu__right-button', 'dropdown-button', {
      '_disabled': menuDisabled,
    });

    return (
      <div styleName="button-with-menu" className={className} onBlur={this.handleBlur} tabIndex="0">
        { open && <div styleName="overlay" onClick={this.handleBlur}></div> }
        <div styleName="controls">
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

function getTransitionProps(animate, position) {
  return {
    component: 'div',
    transitionName: `dd-transition-${position}`,
    transitionEnter: animate,
    transitionLeave: animate,
    transitionEnterTimeout: 300,
    transitionLeaveTimeout: 300,
  };
}
