/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import classNames from 'classnames';

// components
import { TextInput } from 'ui/text-input';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './promo-code.css';

type Props = {
  removeCode: Function,
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
    this.props.removeCode(code)
      .catch((error) => {
        this.setState({ error });
      });
  }

  getRemoveLink(code?: string) {
    if (!this.props.allowDelete) return null;

    return (
      <ActionLink
        title="Remove"
        action={() => this.removeCode(code ? code : null)}
        styleName="action-link-remove"
      />
    );
  }

  get renderGiftCards() {
    if (!this.props.giftCards) return null;

    const { className } = this.props;
    const classes = classNames(styles['gift-card'], {
      [className]: className,
    });

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

  get renderCoupon() {
    if (!this.props.coupon) return null;

    const { className } = this.props;
    const promoCode = _.get(this.props, 'coupon.code');
    const classes = classNames(styles.coupon, {
      [className]: className,
    });

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

  get displayErrors() {
    if (!!this.state.error) return null;

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
