/* @flow */

// libs
import get from 'lodash/get';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Element, Component } from 'react';

// components
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import { ApiErrors } from 'components/utils/errors';

// styles
import s from './confirmation-dialog.css';

type Props = {
  isVisible: boolean,
  body: string | Element<*>,
  title: string | Element<*>,
  cancel: string,
  confirm: string,
  onCancel: () => any,
  confirmAction: () => any,
  asyncState?: AsyncState,
  className?: string,
};

export default class ConfirmationDialog extends Component {
  props: Props;

  componentDidMount() {
    if (this.props.isVisible) {
      window.addEventListener('keydown', this.handleKeyPress);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.isVisible) {
      window.addEventListener('keydown', this.handleKeyPress);
    } else {
      window.removeEventListener('keydown', this.handleKeyPress);
    }
  }

  @autobind
  handleKeyPress(e) {
    if (e.keyCode === 13 /*enter*/) {
      e.preventDefault();

      this.props.confirmAction();
    }
  }

  get footer() {
    const { confirm, confirmAction, onCancel, asyncState } = this.props;

    return (
      <SaveCancel
        onCancel={onCancel}
        onSave={confirmAction}
        saveText={confirm}
        isLoading={get(asyncState, 'inProgress', false)}
      />
    );
  }

  render() {
    const { title, body, isVisible, onCancel, asyncState, className } = this.props;

    return (
      <Modal
        className={classNames(s.modal, className)}
        title={title}
        footer={this.footer}
        isVisible={isVisible}
        onClose={onCancel}
      >
        <ApiErrors response={_.get(asyncState, 'err', null)} />
        {body}
      </Modal>
    );
  }
}
