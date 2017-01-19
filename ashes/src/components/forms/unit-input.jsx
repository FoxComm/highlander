// @flow

import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './css/unit-input.css';

import { Dropdown } from '../dropdown';

type Props = {
  inputValue: string,
  onInputChange: (value: string, e: SyntheticEvent) => void,
  onUnitChange: (value: any, title: string) => void,
  unit: any,
  units: Array<[any, string]>,
  inputClass?: string,
}

class UnitInput extends Component {
  props: Props;

  @autobind
  handleInputChange(e: SyntheticEvent) {
    this.props.onInputChange(e.target.value, e);
  }

  @autobind
  handleUnitChange(value: any, title: string) {
    this.props.onUnitChange(value, title);
  }

  render() {
    const { props } = this;
    return (
      <div styleName="root">
        <input
          className={props.inputClass}
          type="text"
          value={props.inputValue || ''}
          onChange={this.handleInputChange}
        />
        <Dropdown
          value={props.unit}
          items={props.units}
          onChange={this.handleUnitChange}
        />
      </div>
    );
  }
}

export default UnitInput;
