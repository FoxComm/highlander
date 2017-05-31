/* @flow */

// libs
import get from 'lodash/get';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Element, Component } from 'react';

// components
import Modal from 'components/core/modal';
import SaveCancel from 'components/core/save-cancel';
import ErrorAlerts from 'components/alerts/error-alerts';

// styles
import s from './confirmation-modal.css';

type Props = {
  isVisible: boolean,
  title?: string | Element<any>,
  body?: string | Element<any>,
  cancelLabel?: string,
  confirmLabel?: string,
  onCancel: () => any,
  onConfirm: () => any,
  saveDisabled?: boolean,
  asyncState?: AsyncState,
  className?: string,
  children?: Element<any>,
};

export default class ConfirmationModal extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    title: 'Confirm',
    cancelLabel: 'Cancel',
    confirmLabel: 'Confirm',
  };

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
  handleKeyPress(e: KeyboardEvent) {
    if (e.keyCode === 13 /*enter*/) {
      e.preventDefault();

      this.props.onConfirm();
    }
  }

  get footer() {
    const { confirmLabel, cancelLabel, onConfirm, onCancel, saveDisabled, asyncState } = this.props;

    return (
      <SaveCancel
        saveLabel={confirmLabel}
        cancelLabel={cancelLabel}
        onSave={onConfirm}
        onCancel={onCancel}
        isLoading={get(asyncState, 'inProgress', false)}
        saveDisabled={saveDisabled}
      />
    );
  }

  render() {
    const { title, body, isVisible, onCancel, asyncState, className, children } = this.props;

    return (
      <Modal
        className={classNames(s.modal, className)}
        title={title}
        footer={this.footer}
        isVisible={isVisible}
        onClose={onCancel}
      >
        <ErrorAlerts error={get(asyncState, 'err', null)} />
        {body || children}
      </Modal>
    );
  }
}
