/* @flow */

// libs
import noop from 'lodash/noop';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import { withRouter } from 'react-router';

// components
import { Button, PrimaryButton } from './buttons';

type Props = {
  className?: string;
  cancelTabIndex: string;
  cancelTo?: string;
  cancelParams: Object;
  onCancel?: Function;
  saveTabIndex: string;
  cancelText: string;
  cancelDisabled?: boolean;
  onSave?: Function;
  saveText: string;
  saveDisabled?: boolean;
  isLoading?: boolean;
};

/**
 * SaveCancel component implements simple wrapper over 2 components: Save and Cancel
 * It produces either a or Link, depending on cancelTo property
 * It produces custom-handled button or save button depending on whether onSave prop given
 *
 * @class SaveCancel
 */
class SaveCancel extends Component {
  props: Props;

  static defaultProps = {
    cancelTabIndex: '0',
    cancelText: 'Cancel',
    cancelParams: {},
    saveTabIndex: '1',
    saveText: 'Save',
  };

  render() {
    const {
      className,
      cancelTabIndex,
      cancelTo,
      cancelParams,
      onCancel,
      saveTabIndex,
      cancelText,
      cancelDisabled,
      onSave,
      saveText,
      saveDisabled,
      isLoading,
    } = this.props;

    const { push } = this.props.router;

    const cancelControl = (
      <Button
        id="modal-cancel-btn"
        type="button"
        onClick={onCancel ? onCancel : () => push({name: cancelTo, params: cancelParams})}
        className="fc-save-cancel__cancel"
        tabIndex={cancelTabIndex}
        disabled={cancelDisabled}>
        {cancelText}
      </Button>
    );

    const saveControl = (
      <PrimaryButton
        id="modal-confirm-btn"
        type={onSave ? "button" : "submit"}
        onClick={onSave ? onSave : noop}
        className="fc-save-cancel__save"
        tabIndex={saveTabIndex}
        isLoading={isLoading}
        disabled={saveDisabled}>
        {saveText}
      </PrimaryButton>
    );

    return (
      <div className={classNames('fc-save-cancel', className)}>
        {cancelControl}
        {saveControl}
      </div>
    );
  }
};

export default withRouter(SaveCancel);
