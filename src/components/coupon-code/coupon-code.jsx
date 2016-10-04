/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { saveCouponCode } from 'modules/checkout';
import { connect } from 'react-redux';

// localization
import localized from 'lib/i18n';

// components
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';

// styles
import styles from './coupon-code.css';

type Props = {
  saveCouponCode: Function,
  t: any,
};

type State = {
  code: string,
  error: any,
};

class CouponCode extends Component {
  props: Props;

  state: State = {
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
  save() {
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
      <div styleName="coupon">
        <FormField styleName="code-field" error={this.state.error}>
          <TextInput
            styleName="code"
            placeholder={t('CODE')}
            value={this.state.code}
            onChange={this.changeCode}
          />
        </FormField>
        <Button styleName="submit" onClick={this.save}>
          {t('apply')}
        </Button>
      </div>
    );
  }
}

export default connect(null, { saveCouponCode })(localized(CouponCode));
