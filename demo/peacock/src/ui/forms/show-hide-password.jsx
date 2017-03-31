/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './show-hide-password.css';
import classNames from 'classnames';

import localized from 'lib/i18n';

import TextInput from 'ui/text-input/text-input';

type State = {
  isShown: boolean
};

type Props = {
  name: string,
  value: string,
  className: ?string,
  onChange: ?Function,
  placeholder: ?string,
  linkClassName: ?string,
  t: Function,
  minLength: ?number,
};

class ShowHidePassword extends Component {
  props: Props;
  state: State = {
    isShown: false,
  };

  get showLink() {
    const { t, linkClassName } = this.props;
    const message = this.state.isShown ? t('HIDE') : t('SHOW');
    const linkClass = classNames(styles['toggle-link'], linkClassName);
    return (
      <span className={linkClass} onClick={this.toggleState}>
        {message}
      </span>
    );
  }

  @autobind
  toggleState(event) {
    event.preventDefault();
    event.stopPropagation();
    this.setState({
      isShown: !this.state.isShown,
    });
  }

  render() {
    const inputProps = _.omit(this.props, ['linkClassName', 't']);
    const inputType = this.state.isShown ? 'text' : 'password';

    return (
      <TextInput
        {...inputProps}
        type={inputType}
        label={this.showLink}
      />
    );
  }
}

export default localized(ShowHidePassword);
