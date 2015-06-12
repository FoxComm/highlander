'use strict';

import React from 'react';

class ResendNotification extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      edit: false
    };
  }

  toggleEdit(e) {
    e.preventDefault();
    this.setState({edit: true});
  }

  render() {
    let edit = <div className='contact'><input type="email" /></div>;
    let existing = (
      <div className='contact'>
        <span>jim@bob.com</span>
        <a href="" onClick={this.toggleEdit}>Edit</a>
      </div>
    );
    return (
      <div className='modal resend-notification'>
        <div className='modal-header'>
          Resend Email?
        </div>
        <div className='modal-body'>
          You will send another copy of this email to:
          {this.state.edit ? edit : existing}
        </div>
        <div className='modal-footer'>
          <a href="">Cancel</a>
          <a href="">Resend</a>
        </div>
      </div>
    );
  }
}

export default ResendNotification;
