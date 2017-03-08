// @flow

import React, { Component } from 'react';
import Currency from '../common/currency';

import type { ProductVariant } from 'modules/product-variants/list';

type Props = {
  model: ProductVariant,
}

export default class SkuResult extends Component {
  props: Props;

  render() {
    let model = this.props.model;
    let imagePath = model.image || 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg';
    return (
      <div className="fc-grid">
      <div className="fc-col-md-2-12"><img className="fc-image-column" src={imagePath} /></div>
        <div className="fc-col-md-4-12">{model.title}</div>
        <div className="fc-col-md-3-12"><strong>SKU</strong><br />{model.skuCode}</div>
        <div className="fc-col-md-3-12">
          <strong>Price</strong><br />
          <Currency value={model.salePrice} currency={model.salePriceCurrency} />
        </div>
      </div>
    );
  }
}
