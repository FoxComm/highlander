
// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// localization
import localized from 'lib/i18n';

// components
import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';
import LineItemRow from './summary-line-item';

// styles
import styles from './order-summary.css';

const getState = state => ({ ...state.cart });

class OrderSummary extends Component {

  state = {
    isCollapsed: true,
  };

  renderGiftCard(amount) {
    return (
      <li>
        <TermValueLine>
          <span>{t('GIFT CARD')}</span>
          <span>- &nbsp;<Currency value={amount} /></span>
        </TermValueLine>
      </li>
    );
  }

  renderCoupon(amount) {
    return (
      <li>
        <TermValueLine>
          <span>{t('PROMO CODE')}</span>
          <span>- &nbsp;<Currency value={amount} /></span>
        </TermValueLine>
      </li>
    );
  }

  @autobind
  toggleCollapsed() {
    this.setState({
      isCollapsed: !this.state.isCollapsed,
    });
  }

  render() {
    const props = this.props;
    const { t } = props;
    const rows = _.map(props.skus, (item) => <LineItemRow {...item} key={item.sku} />);

    const giftCardPresent = _.some(props.paymentMethods, {type: 'giftCard'});
    const giftCardAmount = _.get(_.find(props.paymentMethods, {type: 'giftCard'}), 'amount', 0);
    const giftCardBlock = giftCardPresent ? this.renderGiftCard(giftCardAmount) : null;

    const couponAmount = _.get(props, 'totals.adjustments');
    const couponPresent = _.isNumber(couponAmount) && couponAmount > 0;
    const couponBlock = couponPresent ? this.renderCoupon(couponAmount) : null;

    const grandTotal = giftCardPresent ? props.totals.total - giftCardAmount : props.totals.total;
    const grandTotalResult = grandTotal > 0 ? grandTotal : 0;

    const style = this.state.isCollapsed ? 'order-summary-collapsed' : 'order-summary';

    return (
      <section styleName={style}>
        <header styleName="header" onClick={this.toggleCollapsed}>
          <div styleName="title">{t('ORDER TOTAL')}</div>
          <Currency styleName="price" value={grandTotalResult} />
        </header>
        <div styleName="content">
          <table styleName="products-table">
            <thead>
            <tr>
              <th colSpan="2">
                <span styleName="product-info">
                  <span>{t('ITEM')}</span>
                  <span>{t('QTY')}</span>
                </span>
              </th>
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
      </section>
    );
  }
}

export default connect(getState, {})(localized(OrderSummary));

