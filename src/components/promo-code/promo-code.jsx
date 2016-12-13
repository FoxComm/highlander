/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import Currency from 'ui/currency';
import Icon from 'ui/icon';

// styles
import styles from './promo-code.css';

type Props = {
  saveCode: Function,
  removeCode: Function,
  buttonLabel?: ?string,
  coupon?: ?Object,
  giftCards?: ?Array<Object>,
  promotion?: ?Object,
  discountValue?: ?number,
  allowDelete?: ?boolean,
  disabled?: boolean,
  placeholder?: string,
  theme: string,
};

type State = {
  code: string,
  error: any,
};

class PromoCode extends Component {
  props: Props;

  static defaultProps = {
    allowDelete: true,
    saveCode: _.noop,
    removeCode: _.noop,
    disabled: false,
    theme: 'light',
  };

  state: State = {
    code: '', // input value
    error: false,
  };

  get buttonLabel(): string {
    return this.props.buttonLabel || 'Apply';
  }

  @autobind
  changeCode({ target }: Object) {
    this.setState({
      code: target.value,
      error: false,
    });
  }

  @autobind
  onKeyPress(e: Object) {
    if (e.key === 'Enter') {
      e.preventDefault();

      this.saveCode();
    }
  }

  @autobind
  saveCode() {
    const code = this.state.code.replace(/\s+/g, '');

    this.props.saveCode(code)
      .then(() => this.setState({ code: '', error: false }))
      .catch(error => {
        this.setState({ error });
      });
  }

  removeCode(code?: string) {
    this.props.removeCode(code)
      .catch(error => {
        this.setState({ error });
      });
  }

  renderGiftCard(card: Object) {
    const { code, amount: discountValue, currentBalance } = card;

    return (
      <div styleName="promo-description" key={card.code}>
        <div styleName="promo-description-wrapper">
          <div styleName="gift-title">{`Gift Card ${code}`}</div>
          <div>
            <Currency prefix={'Current Balance: '} value={currentBalance} />
          </div>
        </div>
        <div styleName="subtotal-price">
          <Currency value={discountValue} prefix={'– '} />
        </div>

        {this.props.allowDelete &&
          <a styleName="delete-promo-btn delete-promo-icon" onClick={() => this.removeCode(code)}>
            <Icon
              name="fc-close"
              styleName="delete-promo-icon"
              styleName="delete-promo-btn"
            />
          </a>}
      </div>
    );
  }

  renderAttachedPromo() {
    if (this.props.coupon) {
      const promoCode = _.get(this.props, 'coupon.code');
      const discountValue = this.props.discountValue;

      return (
        <div styleName="promo-description">
          <div styleName="promo-code">{promoCode}</div>
          <Currency styleName="subtotal-price" value={discountValue} prefix={'– '} />

          {this.props.allowDelete &&
            <Icon
              name="fc-close"
              styleName="delete-promo-icon"
              styleName="delete-promo-btn"
              onClick={() => this.removeCode()}
            />
          }
        </div>
      );
    }

    if (this.props.giftCards) {
      return this.props.giftCards.map(card => this.renderGiftCard(card));
    }

    return null;
  }

  render() {
    const { placeholder, theme } = this.props;

    return (
      <div styleName="root" className={styles[theme]}>
        <div styleName="fieldset">
          <FormField styleName="code-field">
            <TextInput
              styleName="code"
              placeholder={placeholder}
              value={this.state.code}
              onChange={this.changeCode}
              onKeyPress={this.onKeyPress}
            />
          </FormField>
          <Button
            type="button"
            styleName="submit"
            onClick={this.saveCode}
            disabled={this.props.disabled}
          >
            {this.buttonLabel}
          </Button>
        </div>

        {!!this.state.error &&
          <div styleName="error">
            <ErrorAlerts error={this.state.error} />
          </div>
        }

        {this.renderAttachedPromo()}
      </div>
    );
  }
}

export default PromoCode;
