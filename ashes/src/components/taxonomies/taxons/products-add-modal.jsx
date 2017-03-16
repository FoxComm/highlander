/* @flow */

// libs
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';

type Props = {
  isVisible: boolean,
  onCancel: () => void,
  onConfirm: (users: Array<TUser>) => void,
  title: string|Element<*>,
}

class ProductsAddModal extends Component {
  props: Props;

  get actionBlock() {
    return (
      <a className="fc-modal-close" onClick={this.props.onCancel}>
        <i className="icon-close" />
      </a>
    );
  }

  @autobind
  handleSave() {
    this.props.onConfirm();
  }

  get footer() {
    return (
      <SaveCancel
        className="fc-modal-footer fc-add-watcher-modal__footer"
        onCancel={this.props.onCancel}
        onSave={this.handleSave}
        saveText="Confirm"
      />
    );
  }

  render() {
    const { isVisible, title } = this.props;

    return (
      <ModalContainer isVisible={isVisible}>
        <ContentBox
          className="fc-add-watcher-modal"
          title={title}
          actionBlock={this.actionBlock}
          footer={this.footer}
        >
          <div className="fc-modal-body">
            Products add component goes here
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default ProductsAddModal;
