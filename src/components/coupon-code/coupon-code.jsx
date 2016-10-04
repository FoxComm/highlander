
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from '../../pages/checkout/checkout.css';
import { saveCouponCode } from 'modules/checkout';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';

class CouponCode extends Component {

  state = {
    code: '',
    error: false,
  };

  @autobind
  changeCode({target}) {
    this.setState({
      code: target.value,
      error: false,
    });
  }

  @autobind
  onSave() {
    const { t } = this.props;

    const code = this.state.code.replace(/\s+/g, '');

    this.props.saveCouponCode(code).then(() => {
      this.setState({code: ''});
    }).catch(() => {
      this.setState({code: '', error: t('Please enter a valid coupon code and try again.')});
    });
  }

  render() {
    const { t } = this.props;

    return (
      <div styleName="coupon-content">
        <FormField styleName="coupon-code-field" error={this.state.error}>
          <TextInput
            styleName="coupon-code"
            placeholder={t('CODE')}
            value={this.state.code}
            onChange={this.changeCode}
          />
        </FormField>
        <Button styleName="coupon-submit" onClick={this.onSave}>
          {t('apply')}
        </Button>
      </div>
    );
  }
}

export default connect(null, { saveCouponCode })(localized(CouponCode));
