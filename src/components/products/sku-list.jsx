/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { getIlluminatedSkus } from '../../paragons/product';
import _ from 'lodash';

// components
import MultiSelectTable from '../table/multi-select-table';

import type { FullProduct } from '../../modules/products/details';
import type { IlluminatedSku } from '../../paragons/product';

type Props = {
  fullProduct: ?FullProduct,
};

export default class SkuList extends Component<void, Props, void> {
  static tableColumns = [
    { field: 'image', text: 'Image' },
    { field: 'price', text: 'Price', type: 'currency' },
    { field: 'sku', text: 'SKU' },
    { field: 'upc', text: 'UPC' },
  ];

  get illuminatedSkus(): Array<IlluminatedSku> {
    return this.props.fullProduct
      ? getIlluminatedSkus(this.props.fullProduct)
      : [];
  }

  get emptyContent(): Element {
    return (
      <div className="fc-content-box__empty-text">
        No SKUs.
      </div>
    );
  }

  skuContent(skus: Array<IlluminatedSku>): Element {
    return (
      <div className="fc-content-box__empty-text">
        SKUs.
      </div>
    );
  }

  render(): Element {
    return _.isEmpty(this.illuminatedSkus)
      ? this.emptyContent
      : this.skuContent(this.illuminatedSkus);
  }
}
