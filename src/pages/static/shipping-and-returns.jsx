/* @flow */

// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// paragons
import { fieldTypes } from 'paragons/cms';

// styles
import styles from './static.css';

// data
import data from './shipping-and-returns-data.json';

class ShippingAndReturns extends Component {

  @autobind
  renderStatic() {
    return _.map(data, block => {
      return this.renderBlock(block);
    });
  }

  renderBlock(block) {
    switch (block['type']) {
      case fieldTypes.TITLE:
        return (<h2>{block.content}</h2>);
      case fieldTypes.PARAGRAPH:
        return (<p>{block.content}</p>);
      default:
        return block.content;
    }
  }

  render() {
    return (
      <div>
        <div styleName="page-title">
          <h1>Shipping & Returns</h1>
        </div>
        <div styleName="page-content">
          {this.renderStatic()}
        </div>
      </div>
    );
  }
}

export default ShippingAndReturns;
