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
  isCollapsed: boolean,
  header?: any,
  className?: string,
  embedded?: boolean,
  totalTitle?: string,
};

type State = {
  isCollapsed: boolean,
};

class OrderSummary extends Component {
  props: Props;

  static defaultProps = {
    paymentMethods: {},
    isCollapsed: true,
    isScrolled: false,
    embedded: false,
    totalTitle: 'GRAND TOTAL',
  };

  state: State = {
    isCollapsed: this.props.isCollapsed,
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
          <span>{t('GIFT CARD')}</span>
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
          <span>{t('PROMO CODE')}</span>
          <Currency prefix="– " value={amount} />
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

    const style = classNames({
      [styles.collapsed]: this.state.isCollapsed,
      [styles.scrolled]: this.props.isScrolled,
      [styles.embedded]: this.props.embedded,
    }, props.className);

    const header = (
      <header styleName="header" onClick={this.toggleCollapsed}>
        <div styleName="title">{t('ORDER TOTAL')}</div>
        <Currency styleName="price" value={grandTotalResult} />
      </header>
    );

    return (
      <section styleName="order-summary" className={style}>
        { this.props.header !== void 0 ? this.props.header : header }

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
            {this.renderGiftCard(giftCardAmount)}
            {couponBlock}
          </ul>
          <TermValueLine styleName="grand-total">
            <span>{this.props.totalTitle}</span>
            <Currency value={grandTotalResult} />
          </TermValueLine>
        </div>
      </section>
    );
  }
}

export default connect(null, {})(localized(OrderSummary));
