// libs
import React from 'react';
import _ from 'lodash';
import formatCurrency from 'lib/format-currency';

// styles
import styles from './gift-card-form.css';

// components
import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Autocomplete from 'ui/autocomplete';

type Props = {
  product: any,
  addToCart: Function,
  onSkuChange: Function,
  selectedSku: any,
  attributes: Object,
  onAttributeChange: Function,
};

const formatSkuPrice = sku => {
  const price = _.get(sku, 'attributes.salePrice.v', {});
  const value = _.get(price, 'value', 0);
  const currency = _.get(price, 'currency', 'USD');

  return formatCurrency(value, { currency });
};

const GiftCardForm = (props: Props) => {
  const {
    skus,
  } = props.product;

  return (
    <div styleName="card-form-wrap">
      <h1 styleName="title">Digital Gift Card</h1>
      <div styleName="description">
        Give the gift of delicious food!
        To email the gift card on a specific date, select it below.
        To email immediately, select today.
      </div>

      <FormField styleName="form">
        <div styleName="price-selector">
          <Autocomplete
            inputProps={{
              type: 'text',
            }}
            items={skus}
            getItemValue={formatSkuPrice}
            selectedItem={props.selectedSku}
            onSelect={props.onSkuChange}
            sortItems={false}
          />
        </div>
        <TextInput
          styleName="input-field"
          value={props.attributes.recepientName || ''}
          placeholder="Recipient name"
          onChange={props.onAttributeChange('recepientName')}
        />
        <TextInput
          styleName="input-field"
          value={props.attributes.recepientEmail || ''}
          placeholder="Recipient email"
          onChange={props.onAttributeChange('recepientEmail')}
        />
        <textarea
          styleName="message-field"
          placeholder="Your message"
          value={props.attributes.message || ''}
          onChange={props.onAttributeChange('message')}
        />
        <TextInput
          styleName="input-field"
          value={props.attributes.senderName}
          placeholder="Sender name"
          onChange={props.onAttributeChange('senderName')}
        />
        <TextInput
          styleName="input-field"
          value={props.attributes.deliviryDate || ''}
          placeholder="Deliviry date"
          onChange={props.onAttributeChange('deliviryDate')}
        />
        <AddToCartBtn
          styleName="add-to-cart-btn"
          expanded
          onClick={props.addToCart}
        />
      </FormField>
    </div>
  );
};

export default GiftCardForm;
