import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';

export default class ConfirmationDialog extends React.Component {

  static propTypes = {
    isVisible: PropTypes.bool.isRequired,
    body: PropTypes.node.isRequired,
    cancel: PropTypes.string.isRequired,
    confirm: PropTypes.string.isRequired,
    cancelAction: PropTypes.func.isRequired,
    confirmAction: PropTypes.func.isRequired
  };

  componentDidUpdate() {
    if (this.props.isVisible) {
      this.refs.confirmButton.focus();
    }
  }

  @autobind
  onKeyUp(event) {
    if (event.keyCode === 27) {
      this.props.cancelAction();
    }
  }

  render() {
    const props = this.props;

    if (!props.isVisible) return null;

    return (
      <div className='fc-modal'>
        <div className='fc-modal-container'>
          <div className='fc-modal-confirm'>
            <div className='fc-modal-header'>
              <div className='fc-modal-icon'>
                <i className='icon-warning'></i>
              </div>
              <div className='fc-modal-title'>{props.header}</div>
              <a className='fc-modal-close' onClick={() => props.cancelAction()}>
                <span>&times;</span>
              </a>
            </div>
            <div className='fc-modal-body'>
              {props.body}
            </div>
            <div className='fc-modal-footer'>
              <a tabIndex="2" className='fc-modal-close' onClick={() => props.cancelAction()}>
                {props.cancel}
              </a>
              <button tabIndex="1" className='fc-btn' onClick={() => props.confirmAction()} ref="confirmButton" onKeyUp={this.onKeyUp}>
                {props.confirm}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}
