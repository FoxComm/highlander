/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Dropdown, DropdownItem } from 'components/dropdown';

// styles
import styles from './promotion.css';

const SELECT_PROMOTION = [
  ['none', 'None'],
  ['10off', '10% off'],
  ['20off', '20% off'],
  ['30off', '30% off'],
  ['40off', '40% off'],
  ['50off', '50% off'],
];

type State = {
  promotion: string,
};

export default class ProductPromotion extends Component {
  state: State = { promotion: 'none' };

  @autobind
  handleChange(promotion: string) {
    this.setState({ promotion });
  }

  render(): Element {
    return (
      <div styleName="body">
        <div styleName="header">Associated Promotion</div>
        <Dropdown
          styleName="select-control"
          value={this.state.promotion}
          items={SELECT_PROMOTION}
          onChange={this.handleChange} />
      </div>
    );
  }
}
