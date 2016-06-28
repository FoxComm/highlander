/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectFormInner from '../object-form/object-form-inner';
import UserInitials from '../user-initials/initials';
import ContentBox from '../content-box/content-box';
import RoundedPill from '../rounded-pill/rounded-pill';
import { Dropdown, DropdownItem } from '../dropdown';
import { Form } from '../forms';
import { Button } from '../common/buttons';

// styles
import styles from './user-form.css';

type Props = {
  user: Object,
};

class UserForm extends Component {
  props: Props;

  static defaultProps = {
    user: {
      name: '',
      email: '',
      phone: '',
      accountState: '',
    }
  };

  state: Object = {
    ...this.props.user
  };

  @autobind
  handleFormChange(attributes: Object) {
    this.setState({
      name: attributes.firstAndLastName.v,
      email: attributes.emailAddress.v,
      phone: attributes.phoneNumber.v
    });
  }

  @autobind
  handleAccountStateChange(accountState: string) {
    this.setState({
      accountState
    });
  }

  @autobind
  getFormData() {
    return this.state;
  }

  renderAccountState() {
    const { accountState } = this.state;
    const savedAccountState = this.props.user.accountState;
    const accountStateDisabled = (accountState === 'invited') || (savedAccountState === 'archived');

    return (
      <ContentBox title="Account State">
        <Dropdown value={this.state.accountState}
                  onChange={(value) => this.handleAccountStateChange(value)}
                  disabled={accountStateDisabled}>
          <DropdownItem value="active">Active</DropdownItem>
          <DropdownItem value="inactive">Inactive</DropdownItem>
          <DropdownItem value="archived">Archived</DropdownItem>
          <DropdownItem value="invited" isHidden={true} >Invited</DropdownItem>
        </Dropdown>
      </ContentBox>
    );
  }

  renderGeneralForm() {
    const { name, email, phone } = this.state;
    const image = <UserInitials name={name} />;

    const attributes = {
      'profileImage': {
        v: image,
        t: 'element'
      },
      'firstAndLastName': {
        v: name,
        t: 'string'
      },
      'emailAddress': {
        v: email,
        t: 'string'
      },
      'phoneNumber': {
        v: phone,
        t: 'string'
      }
    };

    return (
      <ContentBox title="General">
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