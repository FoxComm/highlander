/* @flow weak */

// libs
import React from 'react';
import { connect } from 'react-redux';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import EditBilling from './edit-billing';
import ViewBilling from './view-billing';

// styles
import styles from './billing.css';

// types
import type { CheckoutBlockProps } from '../types';

const Billing = (props: CheckoutBlockProps) => {
  const { t } = props;

  let content = <EditBilling {...props} />;

  if (!props.isEditing) {
    content = (
      <div styleName="billing-summary">
        <ViewBilling billingData={props.creditCard} />
      </div>
    );
  }

  return (
    <EditableBlock
      {...props}
      styleName="billing"
      title={t('BILLING')}
      content={content}
    />
  );
};

export default connect(state => ({
  billingData: state.checkout.billingData,
  creditCard: state.cart.creditCard,
}))(localized(Billing));
