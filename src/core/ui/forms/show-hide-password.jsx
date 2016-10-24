/* @flow */

import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import localized from 'lib/i18n';

import { TextInputWithLabel } from 'ui/inputs';

type State = {
  isShown: boolean
};

type Props = {
  name: string,
  value: string,
  className?: string,
  onChange: Function,
  placeholder?: string,
  linkClassName?: string,
  t: Function,
  minLength?: number,
};

class ShowHidePassword extends Component {
  props: Props;
  state: State = {
    isShown: false,
  };

  get showLink() {
    const { t, linkClassName } = this.props;
    const message = this.state.isShown ? t('HIDE') : t('SHOW');
    return (
      <a href="#" className={linkClassName} onClick={this.toggleState}>
        {message}
      </a>
    );
  }

  @autobind
  toggleState() {
    this.setState({
      isShown: !this.state.isShown,
    });
  }

  render() {
    const { name, className, onChange, placeholder, value, minLength } = this.props;
    const inputType = this.state.isShown ? 'text' : 'password';

    return (
      <TextInputWithLabel
        className={className}
        placeholder={placeholder}
        name={name}
        value={value}
        onChange={onChange}
        type={inputType}
        label={this.showLink}
        minLength={minLength}
      />
    );
  }
}

export default localized(ShowHidePassword);
