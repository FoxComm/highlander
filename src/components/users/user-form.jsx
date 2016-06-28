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

class UserForm extends Component {

  state = {
    ...this.props.user
  };

  @autobind
  handleFormChange(attributes) {
    this.setState({
      name: attributes.firstAndLastName.v,
      email: attributes.emailAddress.v,
      phone: attributes.phoneNumber.v
    })
  }

  @autobind
  getFormData() {
    return this.state;
  }

  render(): Element {
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
        v: phone || '',
        t: 'string'
      }
    };

    return (
      <Form styleName="user-form">
        <section styleName="main">
          <ContentBox title="General">
            <ObjectFormInner onChange={this.handleFormChange}
                             attributes={attributes} />
            <Button type="button">Change Password</Button>
          </ContentBox>
        </section>

        <aside styleName="aside">
          <ContentBox title="Account State">
            <Dropdown value="active">
              <DropdownItem value="active">Active</DropdownItem>
              <DropdownItem value="inactive">Inactive</DropdownItem>
            </Dropdown>
          </ContentBox>
          <ContentBox title="Roles">
            <RoundedPill text="Super Admin"/>
          </ContentBox>
        </aside>
      </Form>
    );
  }
}

export default UserForm