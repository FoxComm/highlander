/* @flow weak */

import React from 'react';
import ImagesPage, { connectImages } from '../object-page/object-images';

import { actions } from 'modules/product-variants/images';

class ProductVariantImages extends ImagesPage {

  get contextName(): string {
    return this.props.object.context.name;
  }
}

export default connectImages('productVariant', actions)(ProductVariantImages);
