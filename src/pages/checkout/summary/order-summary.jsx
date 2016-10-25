/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import classNames from 'classnames/dedupe';

// localization
import localized from 'lib/i18n';

// components
import TermValueLine from 'ui/term-value-line';
import Currency from 'ui/currency';
import ProductTable from './product-table';

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
  isScrolled: boolean,
};

type State = {
  isCollapsed: boolean,
};

class OrderSummary extends Component {
  props: Props;

  state: State = {
    isCollapsed: true,
  };

  static defaultProps = {
    paymentMethods: {},
  };

  renderGiftCard(amount) {
    const { t } = this.props;

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
    const { t } = this.props;

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

    const giftCardPresent = _.some(props.paymentMethods, {type: 'giftCard'});
    const giftCardAmount = _.get(_.find(props.paymentMethods, {type: 'giftCard'}), 'amount', 0);
    const giftCardBlock = giftCardPresent ? this.renderGiftCard(giftCardAmount) : null;

    const couponAmount = _.get(props, 'totals.adjustments');
    const couponPresent = _.isNumber(couponAmount) && couponAmount > 0;
    const couponBlock = couponPresent ? this.renderCoupon(couponAmount) : null;

    const grandTotal = giftCardPresent ? props.totals.total - giftCardAmount : props.totals.total;
    const grandTotalResult = grandTotal > 0 ? grandTotal : 0;

    const style = classNames({
      [styles.collapsed]: this.state.isCollapsed,
      [styles.scrolled]: this.props.isScrolled,
    });

    return (
      <section styleName="order-summary" className={style}>
        <header styleName="header" onClick={this.toggleCollapsed}>
          <div styleName="title">{t('ORDER TOTAL')}</div>
          <Currency styleName="price" value={grandTotalResult} />
        </header>
        <div styleName="content">
          <ProductTable skus={props.skus} />

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

const mapStateToProps = state => state.cart;

export default connect(mapStateToProps, {})(localized(OrderSummary));
