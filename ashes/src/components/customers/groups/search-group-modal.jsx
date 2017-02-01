/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';

export default class SearchGroupModal extends Component {

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.onCancel}>
        <i className='icon-close'></i>
      </a>
    );
  }

  get footer() {
    return (
      <SaveCancel
        className="fc-modal-footer fc-add-watcher-modal__footer"
        onCancel={this.props.onCancel}
        onSave={this.props.handleSave}
        saveDisabled={this.isSaveDisabled}
      />
    );
  }

  render() {
    const props = this.props;
    return (
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox
          title="Select Groups"
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal"
        >
          <div className="fc-modal-body fc-add-watcher-modal__content">
            Content
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}
