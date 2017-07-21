// @flow

import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import styles from './selectbox.css';

type Props = {
  value: string,
  items: Array<[string, string]>,
  onChange: (value: string) => void,
}

class SelectBox extends Component {
  props: Props;

  renderValues() {
    const { items } = this.props;
    const selectedValue = this.props.value;

    return _.map(items, ([value, title]) => {
      const selected = selectedValue === value;
      return (
        <option key={value} value={value} selected={selected}>
          {title}
        </option>
      );
    });
  }

  @autobind
  handleChange(event: SyntheticInputEvent) {
    this.props.onChange(event.target.value);
  }

  render() {
    return (
      <div styleName="wrapper">
        <select styleName="select" onChange={this.handleChange}>
          {this.renderValues()}
        </select>
        <i styleName="dropdown" />
      </div>
    );
  }
}

export default SelectBox;
