
import React from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';

import EditableBlock from 'ui/editable-block';
import EditAddress from './edit-address';
import { Form } from 'ui/forms';
import Button from 'ui/buttons';
import ViewAddress from './view-address';

import type { CheckoutBlockProps } from './types';
import { AddressKind } from 'modules/checkout';

const ViewShipping = connect(state => (state.checkout.shippingAddress))(ViewAddress);

const EditShipping = props => {
  return (
    <Form onSubmit={props.continueAction}>
      <EditAddress {...props} />
      <Button styleName="checkout-submit" type="submit">CONTINUE</Button>
    </Form>
  );
};


const Shipping = (props: CheckoutBlockProps) => {
  const content = props.isEditing
    ? <EditShipping {...props} addressKind={AddressKind.shipping} />
    : <ViewShipping />;

  const shippingContent = (
    <div styleName="checkout-block-content">
      {content}
    </div>
  );

  return (
    <EditableBlock
      styleName="checkout-block"
      title="SHIPPING"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      content={shippingContent}
    />
  );
};

export default Shipping;
