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

type Props = {
  user: Object,
  onChange: Function,
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
    const data = assoc(this.props.user, ['state', 'accountState'], accountState);
    this.props.onChange(data);
  }

  renderAccountState() {
    const { accountState, disabled } = this.props.user.state;

    return (
      <ContentBox title="Account State">
        <Dropdown value={accountState}
                  onChange={(value) => this.handleAccountStateChange(value)}
                  disabled={disabled}>
          <DropdownItem value="active">Active</DropdownItem>
          <DropdownItem value="inactive">Inactive</DropdownItem>
          <DropdownItem value="archived">Archived</DropdownItem>
          <DropdownItem value="invited" isHidden={true} >Invited</DropdownItem>
        </Dropdown>
      </ContentBox>
    );
  }

  renderUserImage() {
    const name = this.props.user.form.attributes.firstAndLastName.v;

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
    const attributes = this.props.user.form.attributes;

    return (
      <ContentBox title="General">
        {this.renderUserImage()}
        <ObjectFormInner onChange={this.handleFormChange}
                         attributes={attributes} />
        <Button type="button">Change Password</Button>
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
          {this.renderAccountState()}

          <ContentBox title="Roles">
            <RoundedPill text="Super Admin"/>
          </ContentBox>
        </aside>
      </Form>
    );
  }
}

export default UserForm;