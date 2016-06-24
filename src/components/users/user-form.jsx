/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import ObjectForm from '../object-form/object-form';
import UserInitials from '../user-initials/initials';
import ContentBox from '../content-box/content-box';
import RoundedPill from '../rounded-pill/rounded-pill';
import { Dropdown, DropdownItem } from '../dropdown';

class UserForm extends Component {

  render(): Element {
    const image = <UserInitials name={this.props.entity.name} />
    const attributes = {
      'profile image': {
        v: image,
        t: 'element'
      },
      'First & last name': {
        v: this.props.entity.name,
        t: 'string'
      },
      'Email Address': {
        v: this.props.entity.email,
        t: 'string'
      },
      'Phone Number': {
        v: '',
        t: 'string'
      }
    };

    return (
      <div className="fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-3-5">
          <ObjectForm
            onChange={this.handleProductChange}
            attributes={attributes}
            title="General" />
        </div>

        <div className="fc-col-md-2-5">
          <ContentBox title="Account State">
            <Dropdown value="active">
              <DropdownItem value="active">Active</DropdownItem>
              <DropdownItem value="inactive">Inactive</DropdownItem>
            </Dropdown>
          </ContentBox>
          <ContentBox title="Roles">
            <RoundedPill text="Super Admin"/>
          </ContentBox>
        </div>
      </div>
    );
  }
}

export default UserForm