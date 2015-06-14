'use strict';

import React from 'react';

class ContactMethod extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      edit: false
    };
  }

  toggleEdit(e) {
    e.preventDefault();
    this.setState({edit: !this.state.edit});
  }

  render() {
    let content = (
      <div className='contact'>
        <span>{this.props.value}</span>
        <input type='hidden' name='resend_to' value={this.props.value}/>
        <a onClick={this.toggleEdit.bind(this)}>Edit</a>
      </div>
    );
    if (this.state.edit) {
      content = (
        <div className='contact'>
          <input type={this.props.type === 'Email' ? 'email' : 'tel'} name='resend_to' />
          <a onClick={this.toggleEdit.bind(this)}>Undo</a>
        </div>
      );
    }
    return content;
  }
}

ContactMethod.propTypes = {
  type: React.PropTypes.oneOf(['Email', 'SMS']),
  value: React.PropTypes.string
};

export default ContactMethod;
