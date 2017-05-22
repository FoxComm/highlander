/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import localized from 'lib/i18n';

// components
import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';
import ProductTable from './product-table';
import GoogleConversion from 'ui/google/conversion';

// styles
import styles from './order-summary.css';

type Totals = {
  subTotal: number,
  total: number,
  shipping: number,
  taxes: number,
};

type Props = {
  totals: Totals,
  skus: Array<any>,
  paymentMethods?: Object,
  t: any,
  className?: string,
  embedded?: boolean,
  orderPlaced?: ?boolean,
  referenceNumber: string,
};

class OrderSummary extends Component {
  props: Props;

  static defaultProps = {
    paymentMethods: {},
  };

  get giftCards() {
    return _.filter(this.props.paymentMethods, {type: 'giftCard'});
  }

  renderGiftCard(amount) {
    const { t } = this.props;

    if (!amount) {
      return null;
    }

    return (
      <li>
        <TermValueLine>
          <span>{t('Gift card')}</span>
          <Currency prefix="– " value={amount} />
        </TermValueLine>
      </li>
    );
  }

  renderCoupon(amount) {
    const { t } = this.props;

    return (
      <li>
        <TermValueLine>
          <span>{t('Promo code')}</span>
          <Currency prefix="– " value={amount} />
        </TermValueLine>
      </li>
    );
  }

  getOrderPlacedTrackingCode(grandTotal) {
    if (grandTotal <= 0 || !this.props.orderPlaced) {
      return null;
    }

    const params = {
      id: 868108231,
      value: grandTotal / 100,
      label: 'AkdhCPzhm20Qx4_5nQM',
      orderId: this.props.referenceNumber,
    };

    return <GoogleConversion params={params} />;
  }

  render() {
    const props = this.props;
    const { t } = props;
    const giftCardAmount = _.reduce(this.giftCards, (a, card) => {
      return a + card.amount;
    }, 0);

    const couponAmount = _.get(props, 'totals.adjustments');
    const couponPresent = _.isNumber(couponAmount) && couponAmount > 0;
    const couponBlock = couponPresent ? this.renderCoupon(couponAmount) : null;

    // @todo figure out why
    // const grandTotal = props.totals.total;
    const grandTotal = props.totals.total - giftCardAmount;
    const grandTotalResult = grandTotal > 0 ? grandTotal : 0;

    const className = classNames({
      [styles['order-summary']]: !this.props.embedded,
    }, props.className);

    return (
      <section className={className}>
        {this.getOrderPlacedTrackingCode(grandTotalResult)}

        <div styleName="content">
          <ProductTable skus={props.skus} />

          <ul styleName="price-summary">
            <li>
              <TermValueLine>
                <span>{t('Subtotal')}</span>
                <Currency
                  value={props.totals.subTotal}
                  styleName="currency"
                />
              </TermValueLine>
            </li>
            <li>
              <TermValueLine>
                <span>{t('Shipping')}</span>
                <Currency
                  value={props.totals.shipping}
                  styleName="currency"
                />
              </TermValueLine>
            </li>
            <li>
              <TermValueLine>
                <span>{t('Tax')}</span>
                <Currency
                  value={props.totals.taxes}
                  styleName="currency"
                />
              </TermValueLine>
            </li>
            {this.renderGiftCard(giftCardAmount)}
            {couponBlock}
          </ul>
          <TermValueLine styleName="grand-total">
            <span>{t('Total')}</span>
            <Currency
              value={grandTotalResult}
              styleName="currency"
            />
          </TermValueLine>
        </div>
      </section>
    );
  }
}

export default connect(null, {})(localized(OrderSummary));
