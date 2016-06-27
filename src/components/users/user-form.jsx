/* @flow */

// libs
import React, { Component, Element } from 'react';

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

  render(): Element {
    const image = <UserInitials name={this.props.entity.name} />;

    const attributes = {
      'profileImage': {
        v: image,
        t: 'element'
      },
      'firstAndLastName': {
        v: this.props.entity.name,
        t: 'string'
      },
      'emailAddress': {
        v: this.props.entity.email,
        t: 'string'
      },
      'phoneNumber': {
        v: '',
        t: 'string'
      }
    };

    return (
      <Form styleName="user-form">
        <section styleName="main">
          <ContentBox title="General">
            <ObjectFormInner onChange={this.handleProductChange}
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