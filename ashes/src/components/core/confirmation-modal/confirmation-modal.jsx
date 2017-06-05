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
  /** If modal is active or not */
  isVisible: boolean,
  /** Header string */
  title?: string | Element<any>,
  /** Confirmation text (use children if markup needed) */
  label?: string,
  /** Cancel button label */
  cancelLabel?: string,
  /** Confirm button label */
  confirmLabel?: string,
  /** Callback called on close (overlay/esc/cancel click) */
  onCancel: () => any,
  /** Callback called on confirmation */
  onConfirm: () => any,
  /** If confirm button is disabled */
  saveDisabled?: boolean,
  /** AsyncState object that represents confirmation state (inProgress/failed/etc) */
  asyncState?: AsyncState,
  /** Additional className */
  className?: string,
  /** Modal content (in case of plain string `label` can be used instead) */
  children?: Element<any>,
};

/**
 * ConfirmationModal modal represents modal window that provide `Cancel|OK` buttons in footer.
 * It's main purpose is to show confirmation warning for some action (e.g. delete/update/save entity)
 *
 * @class ConfirmationModal
 */
export default class ConfirmationModal extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    title: 'Confirm',
    cancelLabel: 'Cancel',
    confirmLabel: 'OK',
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
    const { title, label, isVisible, onCancel, asyncState, className, children } = this.props;

    return (
      <Modal
        className={classNames(s.modal, className)}
        title={title}
        footer={this.footer}
        isVisible={isVisible}
        onClose={onCancel}
      >
        <ErrorAlerts error={get(asyncState, 'err', null)} />
        {label || children}
      </Modal>
    );
  }
}
