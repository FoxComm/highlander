/* @flow */

// libs
import _ from 'lodash';
import React from 'react';

// components
import Currency from 'ui/currency';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Select from 'ui/select/select';
import Icon from 'ui/icon';

// styles
import styles from './pdp.css';

const QUANTITY_ITEMS = _.range(1, 1 + 10, 1);

type Props = {
  product: any,
  quantity: number,
  onQuantityChange: Function,
  addToCart: Function,
};

const ProductDetails = (props: Props) => {
  const {
    title,
    description,
    currency,
    price,
    amountOfServings,
    servingSize,
  } = props.product;

  return (
    <div>
      <h1 styleName="title">{title}</h1>
      <div styleName="price">
        <Currency value={price} currency={currency} />
      </div>

      <div styleName="cart-actions">
        <div styleName="quantity">
          <Select
            inputProps={{
              type: 'number',
            }}
            getItemValue={_.identity}
            items={QUANTITY_ITEMS}
            onSelect={props.onQuantityChange}
            selectedItem={props.quantity}
            sortItems={false}
          />
        </div>

        <div styleName="add-to-cart-btn">
          <AddToCartBtn expanded onClick={props.addToCart} />
        </div>
      </div>

      <div
        styleName="description"
        dangerouslySetInnerHTML={{__html: description}}
      />

      <div styleName="servings">
        <div>{amountOfServings}</div>
        <div>{servingSize}</div>
      </div>

      <div styleName="social-sharing">
        <Icon name="fc-instagram" styleName="social-icon"/>
        <Icon name="fc-facebook" styleName="social-icon"/>
        <Icon name="fc-twitter" styleName="social-icon" />
        <Icon name="fc-pinterest" styleName="social-icon"/>
      </div>
    </div>
  );
};

export default ProductDetails;
