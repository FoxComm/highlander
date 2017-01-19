/* @flow */

// libs
import _ from 'lodash';
import React from 'react';
import { Link } from 'react-router';

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
    skus,
    amountOfServings,
    servingSize,
  } = props.product;

  const ProductURL = `http://theperfectgourmet.com${props.product.pathName}`;
  const TwitterHandle = 'perfectgourmet1';
  const retailPrice = skus[0].attributes.retailPrice.v.value;
  const retailPriceEl = retailPrice > price ?
    <Currency
      styleName="retail-price"
      value={retailPrice}
      currency={currency}
    />
    : null;

  return (
    <div>
      <h1 styleName="title">{title}</h1>
      <div styleName="price">
        <Currency value={price} currency={currency} />
        {retailPriceEl}
      </div>

      <div styleName="servings">
        <div>{amountOfServings}</div>
        <div>{servingSize}</div>
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

      <div styleName="social-sharing">
        <Link to={`https://www.facebook.com/sharer/sharer.php?u=${ProductURL}&title=${title}&description=${description}&picture=${props.product.images[0]}`} target="_blank" styleName="social-icon">
          <Icon name="fc-facebook" styleName="social-icon"/>
        </Link>

        <Link to={`https://twitter.com/intent/tweet?text=${title}&url=${ProductURL}&via=${TwitterHandle}`} target="_blank" styleName="social-icon">
          <Icon name="fc-twitter" styleName="social-icon" />
        </Link>

        <Link to={`https://pinterest.com/pin/create/button/?url=${ProductURL}&media=${props.product.images[0]}&description=${description}`} target="_blank" styleName="social-icon">
          <Icon name="fc-pinterest" styleName="social-icon"/>
        </Link>
      </div>
    </div>
  );
};

export default ProductDetails;
