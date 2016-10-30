

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
  renderStatic(blocks) {
    return _.map(blocks, block => {
      return this.renderBlock(block);
    });
  }

  @autobind
  renderBlock(block) {
    switch (block.type) {
      case fieldTypes.TITLE:
        return (
          <h2 styleName="title" key={block.id}>{block.content}</h2>
        );
      case fieldTypes.PARAGRAPH:
        return (
          <p styleName="paragraph" key={block.id}>{block.content}</p>
        );
      case fieldTypes.PARAGRAPH_MARGINLESS:
        return (
          <p styleName="paragraph-marginless" key={block.id}>{block.content}</p>
        );
      case fieldTypes.PARAGRAPH_TITLE:
        return (
          <p>
            <strong styleName="paragraph-title" key={block.id}>{block.content}</strong>
          </p>
        );
      case fieldTypes.WELL:
        return (
          <div styleName="well" key={block.id}>
            {this.renderStatic(block.content)}
          </div>
        );
      case fieldTypes.EMAIL:
        return <a href={`mailto:${block.content}`}>{block.content}</a>;
      case fieldTypes.PHONE:
        return <a href={`tel:${block.content}`}>{block.content}</a>;
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
          {this.renderStatic(data)}
        </div>
      </div>
    );
  }
}

export default ShippingAndReturns;
