// libs
import _ from 'lodash';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';

// components
import { HalfCheckbox } from './checkbox';
import GenericDropdown from '../dropdown/generic-dropdown';

export default class CheckboxDropdown extends Component {

  static propTypes = {
    id: PropTypes.string.isRequired,
    className: PropTypes.string,
    disabled: PropTypes.bool,
    checked: PropTypes.bool,
    halfChecked: PropTypes.bool,
    onToggle: PropTypes.func,
    onSelect: PropTypes.func,
    children: PropTypes.node,
  };

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
      <HalfCheckbox
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
