
/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';

import styles from './button-with-menu.css';

import { PrimaryButton } from './buttons';
import { DropdownItem } from '../dropdown';

type DropdownItemType = [any, string|Element];

type Props = {
  onPrimaryClick?: Function;
  onSelect?: (value: any, title: string|Element) => any;
  children?: Element;
  icon?: string;
  items: Array<DropdownItemType>;
  title: string|Element;
  className?: string;
}

type State = {
  open: boolean;
}

export default class ButthonWithMenu extends Component {
  props: Props;

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
  handleItemClick(value: any, title: string|Element) {
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

  render(): Element {
    const { children, icon, title, items } = this.props;
    const { open } = this.state;

    const className = classNames(this.props.className, {
      '_open': open,
    });

    return (
      <div styleName="button-with-menu" className={className} onBlur={this.handleBlur} tabIndex="0">
        <div styleName="controls">
          <PrimaryButton
            className="fc-button-with-menu__left-button"
            icon={icon}
            onClick={this.props.onPrimaryClick}
            onBlur={this.dontPropagate} >
            {title}
          </PrimaryButton>
          <PrimaryButton
            className="fc-button-with-menu__right-button dropdown-button"
            icon="chevron-down"
            onClick={this.handleToggleClick}
            onBlur={this.dontPropagate}
          />
        </div>
        <ul ref="menu" styleName="menu">
          {items && _.map(items, ([value, title]) => (
            <DropdownItem value={value} key={value} onSelect={this.handleItemClick}>
              {title}
            </DropdownItem>
          )) || React.Children.map(children, item => (
              React.cloneElement(item, {
                onSelect: this.handleItemClick,
              })
            )
          )}
        </ul>
      </div>
    );
  }
}
