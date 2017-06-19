/* @flow */

// libs
import React, { Element } from 'react';

// styles
import styles from './shipped-item.css';

// components
import Currency from 'components/utils/currency';
import ProductImage from 'components/imgix/product-image';

//types
import type { TShipmentLineItem } from 'paragons/shipment';

const ShippedItem = (props: TShipmentLineItem): Element<*> => (
  <div styleName="row">
    <div styleName="name">
      <ProductImage
        className="fc-image-column"
        src={props.imagePath}
        width={64}
        height={64}
      />
      {props.name}
    </div>
    <div styleName="sku">{props.sku}</div>
    <div styleName="price">
      <Currency value={props.price} />
    </div>
    <div styleName="quantity">1</div>
    <div styleName="state">{props.state}</div>
    <div styleName="total">
      <Currency value={props.price} />
    </div>
  </div>
);

export default ShippedItem;
