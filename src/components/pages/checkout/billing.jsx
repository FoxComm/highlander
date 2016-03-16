
import React, { Component } from 'react';
import styles from './checkout.css';

import EditableBlock from 'ui/editable-block';

import type { CheckoutBlockProps } from './types';

class EditBilling extends Component {
  render() {
    return (
      <div>edit</div>
    );
  }
}

const Billing = (props: CheckoutBlockProps) => {
  const deliveryContent = (
    <div styleName="checkout-block-content">
      <EditBilling {...props} />
    </div>
  );

  return (
    <EditableBlock
      styleName="checkout-block"
      title="BILLING"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      content={deliveryContent}
    />
  );
};

export default Billing;
