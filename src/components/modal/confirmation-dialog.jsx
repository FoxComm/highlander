'use strict';

import React from 'react';

let ConfirmationDialog = (props) => {
  if (props.isVisible) {
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
              <a className='fc-modal-close' onClick={() => props.cancelAction()}>
                {props.cancel}
              </a>
              <button className='fc-btn' onClick={() => props.confirmAction()}>
                {props.confirm}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  } else {
    return <div></div>;
  }
};

export default ConfirmationDialog;
