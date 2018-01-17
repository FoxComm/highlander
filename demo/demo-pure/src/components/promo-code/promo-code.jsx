/* @flow */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import ErrorAlerts from '@foxcommerce/wings/lib/ui/alerts/error-alerts';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './promo-code.css';

type Props = {
  removeCode?: () => Promise<*>,
  coupon?: ?Object,
  giftCards?: ?Array<Object>,
  allowDelete?: ?boolean,
  className?: string,
};

type State = {
  error: any,
};

class PromoCode extends Component {
  props: Props;

  static defaultProps = {
    allowDelete: true,
  };

  state: State = {
    error: false,
  };

  removeCode(code?: string) {
    if (this.props.removeCode) {
      this.props.removeCode(code)
        .catch((error) => {
          this.setState({ error });
        });
    }
  }

  getRemoveLink(code?: string): Element<*> | null {
    if (!this.props.allowDelete) return null;

    return (
      <ActionLink
        title="Remove"
        action={() => this.removeCode(code)}
        styleName="action-link-remove"
      />
    );
  }

  get renderGiftCards(): Array<Element<*>> | null {
    if (_.isEmpty(this.props.giftCards)) return null;

    const { className } = this.props;
    const classes = classNames(styles['gift-card'], className);

    return _.map(this.props.giftCards, (card) => {
      const { code } = card;
      const formattedCode = code.match(/.{1,4}/g).join(' ');

      return (
        <div className={classes} key={card.code}>
          <div styleName="gift-card-info">
            <div styleName="title">Gift Card</div>
            <div>{formattedCode}</div>
          </div>
          {this.getRemoveLink(code)}
        </div>
      );
    });
  }

  get renderCoupon(): Element<*> | null {
    if (_.isEmpty(this.props.coupon)) return null;

    const { className } = this.props;
    const promoCode = _.get(this.props, 'coupon.code');
    const classes = classNames(styles.coupon, className);

    return (
      <div className={classes}>
        <div styleName="coupon-info">
          <div styleName="title">Coupon Code</div>
          <div styleName="coupon-code">{promoCode}</div>
        </div>
        {this.getRemoveLink()}
      </div>
    );
  }

  get displayErrors(): Element<*> | null {
    if (_.isEmpty(this.state.error)) return null;

    return (
      <div styleName="error">
        <ErrorAlerts error={this.state.error} />
      </div>
    );
  }

  render() {
    return (
      <div styleName="promo-code">
        {this.displayErrors}
        {this.renderGiftCards}
        {this.renderCoupon}
      </div>
    );
  }
}

export default PromoCode;
