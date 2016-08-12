// @flow

import React, { Element } from 'react';
import Currency from '../common/currency';

import styles from './sku-suggest-row.css';

import type { Sku } from 'modules/skus/details';

type Props = {
  sku: Sku,
}

const SkuSuggestRow = (props: Props) => {
  const { sku } = props;
  const imagePath = sku.image
    || 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg';

  return (
    <div styleName="block">
      <img styleName="img" src={imagePath} />
      <div><strong>SKU</strong><br/> {sku.code}</div>
      <div styleName="title"><strong>Title</strong><br/> {sku.title}</div>
      <div styleName="price"><strong>Price</strong><br /><Currency value={sku.price}/></div>
    </div>
  );
};

export default SkuSuggestRow
