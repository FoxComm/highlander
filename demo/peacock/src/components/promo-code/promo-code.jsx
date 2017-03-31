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
  code: string,
  error: any,
};

class PromoCode extends Component {
  props: Props;

  static defaultProps = {
    allowDelete: true,
    removeCode: _.noop,
  };

  state: State = {
    code: '',
    error: false,
  };

  removeCode(code?: string) {
    console.log('Removing the code -> ', code);
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

    return _.map(this.props.giftCards, (card) => {
      const { code } = card;
      const formattedCode = code.match(/.{1,4}/g).join(' ');

      return (
        <div styleName="gift-card" key={card.code}>
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

    const promoCode = _.get(this.props, 'coupon.code');

    return (
      <div styleName="coupon">
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
    const { context, className } = this.props;
    const classes = classNames(styles['promo-code'],{
      [className]: className,
    });
    return (
      <div className={classes}>
        {this.displayErrors}
        {this.renderGiftCards}
        {this.renderCoupon}
      </div>
    );
  }
}

export default PromoCode;
