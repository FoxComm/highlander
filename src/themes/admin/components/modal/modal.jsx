'use strict';

import React from 'react';

import ResendModal from '../notifications/resend.jsx';

class Modal extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      component: <ResendModal closeHandler={this.props.closeHandler} openHandler={this.props.openHandler} />
    };
  }

  closeHandler() {
    this.props.isOpen = false;
  }

  openHandler() {
    this.props.isOpen = true;
  }

  render() {
    return (
      <div>
        <div id='modal-wrap' className={this.props.isOpen ? "show" : "hide"}>
          <div className='modal-overlay'></div>
          <div className='modal'>
            {this.state.component}
          </div>
        </div>
      </div>
    );
  }
}

Modal.propTypes = {
  isOpen: React.PropTypes.bool,
  closeHandler: React.PropTypes.func,
  openHandler: React.PropTypes.func
};

Modal.defaultProps = {
  isOpen: false
};

export default Modal;
