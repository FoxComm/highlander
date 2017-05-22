// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import { PartialCheckbox } from 'components/core/checkbox';
import GenericDropdown from 'components/dropdown/generic-dropdown';

type Props = {
  id: string,
  className?: string,
  disabled?: boolean,
  checked?: boolean,
  halfChecked?: boolean,
  onToggle?: Function,
  onSelect?: Function,
  children?: Element<*>,
};

export default class CheckboxDropdown extends Component {
  props: Props;

  static defaultProps = {
    checked: false,
    halfChecked: false,
    onToggle: _.noop,
    onSelect: _.noop,
  };

  @autobind
  renderCheckbox() {
    const { id, halfChecked, onToggle, checked, disabled } = this.props;
    return (
      <PartialCheckbox
        inline={true}
        id={id}
        docked="left"
        disabled={disabled}
        checked={checked}
        halfChecked={halfChecked}
        onChange={onToggle}
      />
    );
  }

  @autobind
  handleChange(value) {
    this.props.onSelect(value);
  }

  render() {
    return (
      <GenericDropdown
        placeholder="- Select -"
        className="fc-checkbox-dropdown"
        renderDropdownInput={this.renderCheckbox}
        onChange={this.handleChange}
        dropdownProps={{
          className: '_inline _small'
        }}
        {...this.props}
      />
    );
  }
}
