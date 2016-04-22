
/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';

type Props = {
  name: string,
  className?: string,
  value: string|number,
  disabled: bool,
  editable: bool,
  changeable: bool,
  primary: bool,
  open: bool,
  placeholder: string,
  onChange: Function,
  items?: Array<any>,
  children?: Element,
  renderNullTitle: Function,
};


export default class Dropdown extends Component {

  props: Props;

  @autobind
  buildInput(name: string,
             value: string|number,
             placeholder: string,
             title: string,
             editable: bool,
             disabled: bool,
             handleToggleClick: Function): Element {
    if (editable) {
      return (
        <div className="fc-dropdown__value">
          <input
            name={name}
            placeholder={placeholder}
            disabled={disabled}
            defaultValue={title}
            key={`${name}-${value}-selected`}
          />
        </div>
      );
    }

    return (
      <div className="fc-dropdown__value" onClick={handleToggleClick}>
        {title}
        <input name={name} type="hidden" value={value} readOnly />
      </div>
    );
  }


  @autobind
  renderItems(): ?Element {
    const { name, items, children } = this.props;

    if (items) {
      return _.map(items, ([value, title]) => (
        <DropdownItem value={value} key={`${name}-${value}`}>
          {title}
        </DropdownItem>
      ));
    }

    return children;
  }

  render(): Element {
    const {
      name, placeholder, value, primary, editable, open, disabled, onChange, renderNullTitle, className
    } = this.props;

    return (
      <GenericDropdown
        name={name}
        value={value}
        placeholder={placeholder}
        className={className}
        primary={primary}
        editable={editable}
        disabled={disabled}
        open={open}
        renderDropdownInput={this.buildInput}
        renderNullTitle={renderNullTitle}
        onChange={onChange} >
        { this.renderItems() }
      </GenericDropdown>
    );
  }
}
