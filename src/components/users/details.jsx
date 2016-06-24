
import _ from 'lodash';
import React, { PropTypes } from 'react';
import UserContacts from './contacts';
import SectionSubtitle from '../section-title/section-subtitle';
import { connect } from 'react-redux';

@connect((state, props) => ({
  ...state.users.details[props.entity.id],
}), null)
export default class UserDetails extends React.Component {

  render() {
    const user = this.props.entity;
    return (
      <div className="fc-customer-details">
        <SectionSubtitle>Details</SectionSubtitle>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-2">
            <UserContacts userId={user.id} />
          </div>
        </div>
      </div>
    );
  }
};
