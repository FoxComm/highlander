import React, { PropTypes } from 'react';
import { dispatch } from '../../lib/dispatcher';
import ResendModal from './resend';

export default class ResendButton extends React.Component {
  showModal() {
    let notification = this.props.model;

    dispatch('toggleModal', <ResendModal notification={notification} />);
  }

  render() {
    return (
      <a className='btn' onClick={this.showModal.bind(this)}>Resend</a>
    );
  }
}

ResendButton.propTypes = {
  model: PropTypes.object
};
