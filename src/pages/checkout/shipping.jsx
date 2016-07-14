
import React from 'react';
import styles from './checkout.css';
import { connect } from 'react-redux';
import localized from 'lib/i18n';

import EditableBlock from 'ui/editable-block';
import EditAddress from './edit-address';
import { Form } from 'ui/forms';
import Button from 'ui/buttons';
import ViewAddress from './view-address';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';

import type { CheckoutBlockProps } from './types';
import { AddressKind } from 'modules/checkout';

const ViewShipping = connect(state => (state.checkout.shippingAddress))(ViewAddress);

const EditShipping = localized(props => {
  const { t } = props;

  return (
    <Form onSubmit={props.continueAction}>
      <EditAddress {...props} />
      <ErrorAlerts error={props.error} />
      <Button isLoading={props.inProgress} styleName="checkout-submit" type="submit">{t('CONTINUE')}</Button>
    </Form>
  );
});


const Shipping = (props: CheckoutBlockProps) => {
  const content = props.isEditing
    ? <EditShipping {...props} addressKind={AddressKind.SHIPPING} />
    : <ViewShipping />;

  const shippingContent = (
    <div styleName="checkout-block-content">
      {content}
    </div>
  );

  const { t } = props;

  return (
    <EditableBlock
      {...props}
      styleName="checkout-block"
      title={t('SHIPPING')}
      content={shippingContent}
    />
  );
};

export default localized(Shipping);
