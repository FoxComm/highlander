/* @flow */

// libs
import classnames from 'classnames';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { TextInput } from 'ui/text-input';
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
  context: string,
  editable: boolean,
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
    context: 'light',
    editable: true,
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
      .catch((error) => {
        this.setState({ error });
      });
  }

  removeCode(code?: string) {
    this.props.removeCode(code)
      .catch((error) => {
        this.setState({ error });
      });
  }

  renderGiftCard(card: Object) {
    const { code } = card;
    const formattedCode = code.match(/.{1,4}/g).join(' ');

    return (
      <div styleName="gift-card" key={card.code}>
        <div styleName="gift-card-info">
          <div>Gift Card</div>
          <div>{formattedCode}</div>
        </div>
        {this.props.allowDelete &&
          <Icon
            onClick={() => this.removeCode(code)}
            name="fc-close"
            className={classnames(styles['delete-promo-icon'], styles['delete-promo-btn'])}
          />
        }
      </div>
    );
  }

  renderAttachedCoupon() {
    if (this.props.coupon) {
      const promoCode = _.get(this.props, 'coupon.code');

      return (
        <div styleName="coupon">
          <div>Promo Code</div>
          <div styleName="coupon-code">{promoCode}</div>
          {this.props.allowDelete &&
            <Icon
              name="fc-close"
              className={classnames(styles['delete-promo-icon'], styles['delete-promo-btn'])}
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
    const { placeholder, context, editable } = this.props;

    return (
      <div styleName="root" className={styles[context]}>
        {editable &&
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
              disabled={this.props.disabled || !this.state.code}
            >
              {this.buttonLabel}
            </Button>
          </div>
        }

        {!!this.state.error &&
          <div styleName="error">
            <ErrorAlerts error={this.state.error} />
          </div>
        }

        {this.renderAttachedCoupon()}
      </div>
    );
  }
}

export default PromoCode;
