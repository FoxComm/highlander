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
import RoundedPill from '../rounded-pill/rounded-pill';
import { Form, FormField } from '../forms';
import { Button } from 'components/core/button';
import AccountState from './account-state';

// styles
import s from './user-form.css';

type Props = {
  user: Object,
  onChange: Function,
  isNew: boolean,
  requestPasswordReset: (email: string) => Promise<*>,
  restoreState: {
    err?: any,
    inProgress?: boolean,
  },
};

export default class UserForm extends Component {
  props: Props;

  @autobind
  handleFormChange(attributes: Object) {
    const data = assoc(this.props.user, ['form', 'attributes'], attributes);
    this.props.onChange(data);
  }

  get changePasswordButton() {
    if (this.props.isNew) return null;


    return (
      <Button
        type="button"
        onClick={this.resetPassword}
        isLoading={this.props.restoreState.inProgress}
      >
        Change Password
      </Button>
    );
  }

  @autobind
  resetPassword() {
    const email = _.get(this.props, 'user.form.attributes.emailAddress.v', null);
    this.props.requestPasswordReset(email).then(() => {
    });
  }

  @autobind
  handleAccountStateChange(accountState: string) {
    const data = assoc(this.props.user, ['accountState', 'state'], accountState);
    this.props.onChange(data);
  }

  renderAccountState() {
    const { state, disabled } = this.props.user.accountState;

    return (
      <AccountState
        userId={this.props.user.id}
        currentValue={state}
        disabled={disabled}
        onChange={(value) => this.handleAccountStateChange(value)}
      />
    );
  }

  renderUserImage() {
    const name = _.get(this.props.user, 'form.attributes.firstAndLastName.v') || 'New User';

    return (
      <FormField
        className="fc-object-form__field"
        label='Image'
        getTargetValue={_.noop}
        key={`object-form-attribute-firstAndLastName`} >
        <UserInitials name={name} />
      </FormField>
    );
  }

  renderGeneralForm() {
    const { options, schema } = this.props.user;
    const { attributes } = this.props.user.form;

    return (
      <Form>
        <ContentBox title="General">
          {this.renderUserImage()}
          <ObjectFormInner
            onChange={this.handleFormChange}
            attributes={attributes}
            options={options}
            schema={schema}
          />
          {this.changePasswordButton}
        </ContentBox>
      </Form>
    );
  }

  render() {
    return (
      <div className={s.userForm}>
        <section className={s.main}>
          {this.renderGeneralForm()}
        </section>

        <aside className={s.aside}>
          {!this.props.isNew && this.renderAccountState()}

          <ContentBox title="Roles" className={s.roles} >
            <RoundedPill text="Super Admin"/>
          </ContentBox>
        </aside>
      </div>
    );
  }
}
