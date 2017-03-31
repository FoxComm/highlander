/* @flow */

// libs
import _ from 'lodash';
import React from 'react';

// components
import Currency from 'ui/currency';

// styles
import styles from './pdp.css';

type Props = {
  product: any,
};

const ProductDetails = (props: Props) => {
  const {
    currency,
    price,
    skus,
  } = props.product;

  const salePrice = _.get(skus[0], 'attributes.salePrice.v.value', 0);
  const retailPrice = _.get(skus[0], 'attributes.retailPrice.v.value', 0);

  const productPrice = (retailPrice > salePrice) ? (
    <div styleName="price">
      <Currency
        styleName="retail-price"
        value={retailPrice}
        currency={currency}
      />
      <Currency
        styleName="on-sale-price"
        value={salePrice}
        currency={currency}
      />
    </div>
  ) : (
    <div styleName="price">
      <Currency value={price} currency={currency} />
    </div>
  );

  return (
    <div>
      {productPrice}
    </div>
  );
};

export default ProductDetails;
