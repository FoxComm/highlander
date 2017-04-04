/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import formatCurrency from 'lib/format-currency';
import { email } from 'ui/forms/validators';

// styles
import styles from './gift-card-form.css';

// components
import { TextInput } from 'ui/text-input';
import { TextArea } from 'ui/textarea';
import { FormField } from 'ui/forms';
import Select from 'ui/select/select';

// types
import type { TProductView } from 'pages/catalog/types';
import type { Sku } from 'modules/product-details';

type Props = {
  productView: TProductView,
  onSkuChange: Function,
  selectedSku: Sku,
  attributes: Object,
  onAttributeChange: Function,
};

const formatSkuPrice = (sku) => {
  const price = _.get(sku, 'attributes.salePrice.v', {});
  const value = _.get(price, 'value', 0);
  const currency = _.get(price, 'currency', 'USD');

  return formatCurrency(value, { currency });
};

const GiftCardForm = (props: Props) => {
  const {
    skus,
  } = props.productView;

  const recipientName = _.get(props.attributes, 'giftCard.recipientName', '');
  const recipientEmail = _.get(props.attributes, 'giftCard.recipientEmail', '');
  const message = _.get(props.attributes, 'giftCard.message', '').split('<br>').join('\n');
  const senderName = _.get(props.attributes, 'giftCard.senderName', '');

  return (
    <div styleName="block">
      <div styleName="price-selector">
        <Select
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
      <div styleName="recipient-block">
        <FormField label="Recipient name" required>
          <TextInput
            pos="top"
            value={recipientName}
            placeholder="Recipient name"
            onChange={props.onAttributeChange}
            name="giftCard.recipientName"
          />
        </FormField>
        <FormField label="Recipient email" validator={email} required>
          <TextInput
            pos="bottom"
            value={recipientEmail}
            placeholder="Recipient email"
            onChange={props.onAttributeChange}
            name="giftCard.recipientEmail"
          />
        </FormField>
      </div>
      <FormField>
        <TextArea
          styleName="message-field"
          placeholder="Your message"
          value={message}
          onChange={props.onAttributeChange}
          name="giftCard.message"
        />
      </FormField>
      <FormField label="Your name" required>
        <TextInput
          value={senderName}
          placeholder="Your name"
          onChange={props.onAttributeChange}
          name="giftCard.senderName"
        />
      </FormField>
    </div>
  );
};

export default GiftCardForm;
