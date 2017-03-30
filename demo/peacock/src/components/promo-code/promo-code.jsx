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
import ActionLink from 'ui/action-link/action-link';

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
            <div>Gift Card</div>
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
          <div>Promo Code</div>
          <div styleName="coupon-code">{promoCode}</div>
        </div>
        {this.getRemoveLink()}
      </div>
    );
  }

  get editCode() {
    if (!this.props.editable) return null;

    return (
      <div styleName="fieldset">
        <FormField styleName="code-field">
          <TextInput
            styleName="code"
            placeholder={this.props.placeholder}
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
    const { context } = this.props;

    return (
      <div styleName="root" className={styles[context]}>
        {this.displayErrors}
        {this.renderGiftCards}
        {this.renderCoupon}
        {this.editCode}
      </div>
    );
  }
}

export default PromoCode;
