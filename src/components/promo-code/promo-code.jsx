/* @flow */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// localization
import localized from 'lib/i18n';

// components
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';

// styles
import styles from './promo-code.css';

type Props = {
  saveCode: Function,
  buttonLabel?: ?string,
  t: any,
};

type State = {
  code: string,
  error: any,
};

class PromoCode extends Component {
  props: Props;

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
  save() {
    const code = this.state.code.replace(/\s+/g, '');

    this.props.saveCode(code)
      .catch(err => {
        this.setState({ code: '', error: err });
      });
  }

  render() {
    const { t } = this.props;

    return (
      <div styleName="fieldset">
        <FormField styleName="code-field">
          <TextInput
            styleName="code"
            placeholder={t('CODE')}
            value={this.state.code}
            onChange={this.changeCode}
          />
        </FormField>
        <Button styleName="submit" onClick={this.save} type="button">
          {this.buttonLabel}
        </Button>
        <ErrorAlerts error={this.state.error} />
      </div>
    );
  }
}

export default localized(PromoCode);
