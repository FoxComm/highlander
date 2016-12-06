/* @flow weak */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './select.css';
import { autobind } from 'core-decorators';

/* eslint react/sort-comp: 0 */

type Props = {
  items: Array<any>,
  selectedItem: any,
  onSelect: Function,
  sortItems: boolean,
};

class Select extends Component {

  props: Props;

  static defaultProps = {
    onSelect() {},
  };

  renderItem(item) {
    return (<option styleName="option" value={item}>{item}</option>);
  }

  @autobind
  handleChange(event) {
    this.props.onSelect(event.target.value);
  }

  render() {
    return (
      <div styleName="select-box">
        <select styleName="select" value={this.props.selectedItem} onChange={this.handleChange}>
          {_.map(this.props.items, this.renderItem)}
        </select>
      </div>
    );
  }
}

export default Select;
