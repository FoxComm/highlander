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
import PromoCode from '../../../components/promo-code/promo-code';

// styles
import styles from './billing.css';

// types
import type { CheckoutBlockProps, BillingData } from '../types';

type Props = CheckoutBlockProps & {
  paymentMethods: Array<any>,
  creditCard: BillingData,
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
    const { coupon, promotion, totals, creditCard } = this.props;

    return (
      <div styleName="billing-summary">
        <ViewBilling billingData={creditCard} />

        {coupon &&
          <div styleName="promo-line">
            <PromoCode
              placeholder="Coupon Code"
              coupon={coupon}
              promotion={promotion}
              discountValue={totals.adjustments}
              allowDelete={false}
              editable={false}
              context="billingView"
            />
          </div>}

        {this.giftCards &&
          <div styleName="promo-line">
            <PromoCode
              placeholder="Gift Card Number"
              giftCards={this.giftCards}
              allowDelete={false}
              editable={false}
              context="billingView"
            />
          </div>}
      </div>
    );
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
  creditCard: state.checkout.creditCard,
  ...state.cart,
}))(localized(Billing));
