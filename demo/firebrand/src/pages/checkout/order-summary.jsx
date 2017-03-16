
import _ from 'lodash';
import React from 'react';
import styles from './order-summary.css';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';
import LineItemRow from './summary-line-item';

const getState = state => ({ ...state.cart });

const OrderSummary = props => {
  const { t } = props;
  const rows = _.map(props.skus, (item) => <LineItemRow {...item} key={item.sku} />);

  const renderGiftCard = (amount) => {
    return (
      <li>
        <TermValueLine>
          <span>{t('GIFT CARD')}</span>
          <span>- &nbsp;<Currency value={amount} /></span>
        </TermValueLine>
      </li>
    );
  };

  const renderCoupon = (amount) => {
    return (
      <li>
        <TermValueLine>
          <span>{t('PROMO CODE')}</span>
          <span>- &nbsp;<Currency value={amount} /></span>
        </TermValueLine>
      </li>
    );
  };

  const giftCardPresent = _.some(props.paymentMethods, {type: 'giftCard'});
  const giftCardAmount = _.get(_.find(props.paymentMethods, {type: 'giftCard'}), 'amount', 0);
  const giftCardBlock = giftCardPresent ? renderGiftCard(giftCardAmount) : null;

  const couponAmount = _.get(props, 'totals.adjustments');
  const couponPresent = _.isNumber(couponAmount) && couponAmount > 0;
  const couponBlock = couponPresent ? renderCoupon(couponAmount) : null;

  const grandTotal = giftCardPresent ? props.totals.total - giftCardAmount : props.totals.total;
  const grandTotalResult = grandTotal > 0 ? grandTotal : 0;

  return (
    <div styleName="order-summary">
      <div styleName="title">{t('ORDER SUMMARY')}</div>
      <table styleName="products-table">
        <thead>
          <tr>
            <th styleName="product-image">{t('ITEM')}</th>
            <th styleName="product-name" />
            <th styleName="product-qty">{t('QTY')}</th>
            <th styleName="product-price">{t('PRICE')}</th>
          </tr>
        </thead>
        <tbody>
          {rows}
        </tbody>
      </table>
      <ul styleName="price-summary">
        <li>
          <TermValueLine>
            <span>{t('SUBTOTAL')}</span>
            <Currency value={props.totals.subTotal} />
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>{t('SHIPPING')}</span>
            <Currency value={props.totals.shipping} />
          </TermValueLine>
        </li>
        <li>
          <TermValueLine>
            <span>{t('TAX')}</span>
            <Currency value={props.totals.taxes} />
          </TermValueLine>
        </li>
        {giftCardBlock}
        {couponBlock}
      </ul>
      <TermValueLine styleName="grand-total">
        <span>{t('GRAND TOTAL')}</span>
        <Currency value={grandTotalResult} />
      </TermValueLine>
    </div>
  );
};

export default connect(getState, {})(localized(OrderSummary));
