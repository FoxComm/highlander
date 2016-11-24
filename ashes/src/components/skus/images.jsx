/* @flow weak */

import React from 'react';
import ImagesPage, { connectImages } from '../object-page/object-images';

import { actions } from 'modules/skus/images';

class SkuImages extends ImagesPage {
  get entityIdName(): string {
    return 'skuCode';
  }

  get contextName(): string {
    return this.props.object.context.name;
  }
}

export default connectImages('sku', actions)(SkuImages);
