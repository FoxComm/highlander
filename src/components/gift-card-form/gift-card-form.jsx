/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import formatCurrency from 'lib/format-currency';

// styles
import styles from './gift-card-form.css';

// components
import { TextInput } from 'ui/inputs';
import { Form, FormField } from 'ui/forms';
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
        Email a gift card today!
      </div>

      <Form styleName="form" onSubmit={props.addToCart}>
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
        <FormField label="Recipient name" required>
          <TextInput
            styleName="input-field"
            value={props.attributes.recipientName || ''}
            placeholder="Recipient name"
            onChange={props.onAttributeChange}
            name="recipientName"
          />
        </FormField>
        <FormField label="Recipient email" required>
          <TextInput
            styleName="input-field"
            value={props.attributes.recipientEmail || ''}
            placeholder="Recipient email"
            onChange={props.onAttributeChange}
            name="recipientEmail"
          />
        </FormField>
        <FormField>
          <textarea
            styleName="message-field"
            placeholder="Your message"
            value={props.attributes.message || ''}
            onChange={props.onAttributeChange}
            name="message"
          />
        </FormField>
        <FormField label="Sender name" required>
          <TextInput
            styleName="input-field"
            value={props.attributes.senderName || ''}
            placeholder="Sender name"
            onChange={props.onAttributeChange}
            name="senderName"
          />
        </FormField>
        <AddToCartBtn
          styleName="add-to-cart-btn"
          type="submit"
          expanded
        />
      </Form>
    </div>
  );
};

export default GiftCardForm;
