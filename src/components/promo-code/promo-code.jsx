/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';
import Currency from 'ui/currency';
import Icon from 'ui/icon';

// styles
import styles from './promo-code.css';

type Props = {
  saveCode: ?Function,
  removeCode: ?Function,
  buttonLabel?: ?string,
  coupon: ?Object,
  giftCard: ?Object,
  promotion: ?Object,
  discountValue: ?number,
  allowDelete: ?boolean,
};

type State = {
  code: string,
  error: any,
};

class PromoCode extends Component {
  props: Props;

  static defaultProps = {
    allowDelete: true,
  };

  state: State = {
    code: '',
    error: false,
  };

  get buttonLabel() {
    return this.props.buttonLabel || 'Apply';
  }

  @autobind
  changeCode({target}) {
    this.setState({
      code: target.value,
      error: false,
    });
  }

  @autobind
  onKeyPress(e) {
    if (e.key === 'Enter') {
      this.saveCode();
    }
  }

  @autobind
  saveCode() {
    const code = this.state.code.replace(/\s+/g, '');

    this.props.saveCode(code)
      .catch(error => {
        this.setState({ error });
      });
  }

  @autobind
  removeCode() {
    this.props.removeCode()
      .then(() => this.setState({ code: '' }))
      .catch(error => {
        this.setState({ error });
      });
  }

  renderApplyPromoView() {
    return (
      <div styleName="fieldset">
        <FormField styleName="code-field">
          <TextInput
            styleName="code"
            placeholder={'CODE'}
            value={this.state.code}
            onChange={this.changeCode}
            onKeyPress={this.onKeyPress}
          />
        </FormField>
        <Button styleName="submit" onClick={this.saveCode} type="button">
          {this.buttonLabel}
        </Button>
        <ErrorAlerts error={this.state.error} />
      </div>
    );
  }

  renderAttachedPromo() {
    let promoTitle;
    let promoCode;
    let discountDescription;
    let discountValue;

    if (this.props.coupon) {
      promoTitle = 'Coupon code';
      promoCode = _.get(this.props, 'coupon.code');
      discountDescription = _.get(this.props, 'promotion.attributes.name.v');
      discountValue = this.props.discountValue;
    } else if (this.props.giftCard) {
      promoTitle = 'Gift card';
      promoCode = this.props.giftCard.code;
      discountDescription = (
        <span>
          <span>Balance: </span>
          <Currency value={this.props.giftCard.currentBalance} />
        </span>
      );

      discountValue = this.props.giftCard.amount;
    }

    return (
      <div styleName="promo-description">
        <div styleName="promo-description-wrapper">
          <div styleName="promo-title">{promoTitle}</div>
          <div>{promoCode}</div>
          <div>{discountDescription}</div>
        </div>
        <div styleName="subtotal-price">
          <span>- &nbsp;<Currency value={discountValue} /></span>
        </div>

        {this.props.allowDelete &&
          <a styleName="delete-promo-btn" onClick={this.removeCode}>
            <Icon name="fc-close" styleName="delete-promo-icon" />
          </a>}
      </div>
    );
  }

  render() {
    return (this.props.giftCard || this.props.coupon) ?
      this.renderAttachedPromo() :
      this.renderApplyPromoView();
  }
}

export default PromoCode;
