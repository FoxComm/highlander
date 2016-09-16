/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';

import ContentBox from 'components/content-box/content-box';
import SkuList from './sku-list';

import styles from './sku-content-box.css';

import type { Product } from 'paragons/product';
import type { Sku } from 'modules/skus/details';

type UpdateFn = (code: string, field: string, value: any) => void;

type Props = {
  fullProduct: ?Product,
  updateField: UpdateFn,
  updateFields: (code: string, toUpdate: Array<Array<any>>) => void,
  variants: Array<any>,
};

class SkuContentBox extends Component {
  props: Props;

  get actions(): Element {
    return (
      <a styleName="add-icon" onClick={this.addAction}>
        <i className="icon-add" />
      </a>
    );
  }

  @autobind
  addAction() {
    console.log("add clicked");
  }

  render() {
    return (
      <ContentBox title="SKUs" actionBlock={ this.actions }>
        <SkuList
          fullProduct={this.props.fullProduct}
          updateField={this.props.onSetSkuProperty}
          updateFields={this.props.onSetSkuProperties}
          variants={this.props.variants}
        />
      </ContentBox>
    );
  }
}

export default SkuContentBox;
