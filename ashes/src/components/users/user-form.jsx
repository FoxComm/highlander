/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import UserInitials from '../user-initials/initials';
import ContentBox from '../content-box/content-box';
import { RoundedPill } from 'components/core/rounded-pill';
import { Form, FormField } from '../forms';
import { Button } from 'components/core/button';
import AccountState from './account-state';
import { ApiErrors } from 'components/utils/errors';
import Alert from 'components/core/alert';

// styles
import s from './user-form.css';

type Props = {
  user: Object,
  onChange: Function,
  isNew: boolean,
  requestPasswordReset: (email: string) => Promise<*>,
  clearResetState: () => void,
  restoreState: AsyncState,
};

type State = {
  isMessageDisplayed: boolean,
};

export default class UserForm extends Component {
  props: Props;
  state: State = {
    isMessageDisplayed: false,
  };

  @autobind
  handleFormChange(attributes: Object) {
    const data = assoc(this.props.user, ['form', 'attributes'], attributes);
    this.props.onChange(data);
  }

  get changePasswordButton(): ?Element<*> {
    if (this.props.isNew) return null;

    return (
      <Button type="button" onClick={this.resetPassword} isLoading={this.props.restoreState.inProgress}>
        Change Password
      </Button>
    );
  }

  @autobind
  resetPassword() {
    const email = _.get(this.props, 'user.form.attributes.emailAddress.v', null);
    this.setState({ isMessageDisplayed: true }, () => this.props.requestPasswordReset(email));
  }

  @autobind
  handleAccountStateChange(accountState: string) {
    const data = assoc(this.props.user, ['accountState', 'state'], accountState);
    this.props.onChange(data);
  }

  @autobind
  removeAlert() {
    this.setState({ isMessageDisplayed: false }, () => {
      this.props.clearResetState();
    });
  }

  renderAccountState(): Element<*> {
    const { state, disabled } = this.props.user.accountState;

    return (
      <AccountState
        userId={this.props.user.id}
        currentValue={state}
        disabled={disabled}
        onChange={value => this.handleAccountStateChange(value)}
        className={s.accountState}
      />
    );
  }

  renderUserImage(): Element<*> {
    const name = _.get(this.props.user, 'form.attributes.firstAndLastName.v') || 'New User';

    return (
      <FormField
        className="fc-object-form__field"
        label="Image"
        getTargetValue={_.noop}
        key={`object-form-attribute-firstAndLastName`}
      >
        <UserInitials name={name} />
      </FormField>
    );
  }

  renderNotification(): ?Element<*> {
    const { err, inProgress, finished } = this.props.restoreState;
    const { isMessageDisplayed } = this.state;

    if (isMessageDisplayed && err != null) {
      return <ApiErrors type={Alert.SUCCESS} error={err} closeAction={this.removeAlert} />;
    }

    if (isMessageDisplayed && !inProgress && finished) {
      return (
        <Alert type={Alert.SUCCESS} closeAction={this.removeAlert} error={err}>
          Password reset email was successfully sent.
        </Alert>
      );
    }

    return null;
  }

  renderGeneralForm(): Element<*> {
    const { options, schema } = this.props.user;
    const { attributes } = this.props.user.form;

    return (
      <Form>
        <ContentBox title="General">
          {this.renderNotification()}
          {this.renderUserImage()}
          <ObjectFormInner onChange={this.handleFormChange} attributes={attributes} options={options} schema={schema} />
          {this.changePasswordButton}
        </ContentBox>
      </Form>
    );
  }

  render(): Element<*> {
    return (
      <div className={s.userForm}>
        <section className={s.main}>
          {this.renderGeneralForm()}
        </section>

        <aside className={s.aside}>
          {!this.props.isNew && this.renderAccountState()}

          <ContentBox title="Roles" className={s.roles}>
            <RoundedPill text="Super Admin" />
          </ContentBox>
        </aside>
      </div>
    );
  }
}
