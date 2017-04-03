/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';

// localization
import localized from 'lib/i18n';

// components
import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';

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
  paymentMethods?: Object,
  t: any,
  header?: any,
  className?: string,
  totalTitle?: string,
  orderPlaced?: ?boolean,
};

class OrderTotals extends Component {
  props: Props;

  static defaultProps = {
    paymentMethods: {},
    totalTitle: 'Grand Total',
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
        <TermValueLine className={styles.value}>
          <span>{t('Gift Card')}</span>
          <Currency prefix="– " value={amount} />
        </TermValueLine>
      </li>
    );
  }

  renderCoupon(amount) {
    const { t } = this.props;

    return (
      <li>
        <TermValueLine className={styles.value}>
          <span>{t('Coupon Code')}</span>
          <Currency prefix="– " value={amount} />
        </TermValueLine>
      </li>
    );
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

    return (
      <div>
        <ul styleName="price-summary">
          <li>
            <TermValueLine className={styles.value}>
              <span>{t('Subtotal')}</span>
              <Currency value={props.totals.subTotal} />
            </TermValueLine>
          </li>
          <li>
            <TermValueLine className={styles.value}>
              <span>{t('Shipping')}</span>
              <Currency value={props.totals.shipping} />
            </TermValueLine>
          </li>
          <li>
            <TermValueLine className={styles.value}>
              <span>{t('Tax')}</span>
              <Currency value={props.totals.taxes} />
            </TermValueLine>
          </li>
          {this.renderGiftCard(giftCardAmount)}
          {couponBlock}
        </ul>
        <TermValueLine className={`${styles['grand-total']} ${styles.value}`}>
          <span>{this.props.totalTitle}</span>
          <Currency value={grandTotalResult} />
        </TermValueLine>
      </div>
    );
  }
}

export default localized(OrderTotals);
