/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import UserInitials from '../user-initials/initials';
import ContentBox from '../content-box/content-box';
import RoundedPill from '../rounded-pill/rounded-pill';
import { Dropdown, DropdownItem } from '../dropdown';
import { Form, FormField } from '../forms';
import { Button } from '../common/buttons';

// styles
import styles from './user-form.css';

const SELECT_STATE = [
  ['active', 'Active'],
  ['inactive', 'Inactive'],
  ['archived', 'Archived'],
  ['invited', 'Invited', true],
];

type Props = {
  user: Object,
  onChange: Function,
  isNew: bool,
};

class UserForm extends Component {
  props: Props;

  @autobind
  handleFormChange(attributes: Object) {
    const data = assoc(this.props.user, ['form', 'attributes'], attributes);
    this.props.onChange(data);
  }

  @autobind
  handleAccountStateChange(accountState: string) {
    const data = assoc(this.props.user, ['accountState', 'state'], accountState);
    this.props.onChange(data);
  }

  renderAccountState() {
    const { state, disabled } = this.props.user.accountState;

    return (
      <ContentBox title="Account State">
        <Dropdown value={state}
                  onChange={(value) => this.handleAccountStateChange(value)}
                  disabled={disabled}
                  items={SELECT_STATE}
        />
      </ContentBox>
    );
  }

  renderUserImage() {
    const name = this.props.user.form.attributes.firstAndLastName.v || 'New User';

    return (
      <FormField
        className="fc-object-form__field"
        label='Image'
        key={`object-form-attribute-firstAndLastName`} >
        <UserInitials name={name} />
      </FormField>
    );
  }

  renderGeneralForm() {
    const { attributes, options } = this.props.user.form;

    return (
      <ContentBox title="General">
        {this.renderUserImage()}
        <ObjectFormInner onChange={this.handleFormChange}
                         attributes={attributes}
                         options={options}
        />
        {!this.props.isNew && <Button type="button">Change Password</Button>}
      </ContentBox>
    );
  }

  render(): Element {
    return (
      <Form styleName="user-form">
        <section styleName="main">
          {this.renderGeneralForm()}
        </section>

        <aside styleName="aside">
          {!this.props.isNew && this.renderAccountState()}

          <ContentBox title="Roles">
            <RoundedPill text="Super Admin"/>
          </ContentBox>
        </aside>
      </Form>
    );
  }
}

export default UserForm;
