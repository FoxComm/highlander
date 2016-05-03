
/* @flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';

import type { Props as GenericProps } from './generic-dropdown';

type Props = GenericProps;

export default class Dropdown extends Component {
  props: Props;

  @autobind
  buildInput(value: string|number, title: string, props: Props, handleToggleClick: Function): Element {
    if (props.editable) {
      return (
        <div className="fc-dropdown__value">
          <input
            name={props.name}
            placeholder={props.placeholder}
            disabled={props.disabled}
            defaultValue={title}
            key={`${props.name}-${value}-selected`}
          />
        </div>
      );
    }

    return (
      <div className="fc-dropdown__value" onClick={handleToggleClick}>
        {title}
        <input name={props.name} type="hidden" value={value} readOnly />
      </div>
    );
  }


  renderItems(): ?Element {
    const { name, items, children } = this.props;

    if (!_.isEmpty(items)) {
      return _.map(items, ([value, title]) => (
        <DropdownItem value={value} key={`${name}-${value}`}>
          {title}
        </DropdownItem>
      ));
    }

    return children;
  }

  render(): Element {
    const restProps = _.omit(this.props, 'children');

    return (
      <GenericDropdown
        {...restProps}
        renderDropdownInput={this.buildInput}>
        { this.renderItems() }
      </GenericDropdown>
    );
  }
}
