/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import localized from 'lib/i18n';

// components
import EditableBlock from 'ui/editable-block';
import EditBilling from './edit-billing';
import ViewBilling from './view-billing';
import ViewGiftCards from './view-gift-cards';

// styles
import styles from './billing.css';

// types
import type { CheckoutBlockProps, BillingData } from '../types';

type Props = CheckoutBlockProps & {
  paymentMethods: Array<any>,
  creditCard: BillingData,
  billingData: BillingData,
};

class Billing extends Component {
  props: Props;

  get giftCards() {
    return _.filter(this.props.paymentMethods, {type: 'giftCard'});
  }

  get editBilling() {
    return (
      <EditBilling
        {...this.props}
        giftCards={this.giftCards}
      />
    );
  }

  get viewBilling() {
    const hasPaymentMethods = !_.isEmpty(this.props.creditCard) || !_.isEmpty(this.giftCards);
    return (hasPaymentMethods) ? (
      <div styleName="billing-summary">
        <ViewBilling billingData={this.props.creditCard} />
        <ViewGiftCards paymentMethods={this.giftCards} />
      </div>
    ) : null;
  }

  renderContent() {
    return (this.props.isEditing) ? this.editBilling : this.viewBilling;
  }

  render() {
    const { t } = this.props;

    return (
      <EditableBlock
        isEditing={this.props.isEditing}
        editAction={this.props.editAction}
        editAllowed={this.props.editAllowed}
        styleName="billing"
        title={t('BILLING')}
        content={this.renderContent()}
      />
    );
  }
}

export default connect(state => ({
  billingData: state.checkout.billingData,
  creditCard: state.cart.creditCard,
}))(localized(Billing));
