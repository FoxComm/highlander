// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { Link } from '../../components/link';
import { PrimaryButton } from './buttons';

/**
 * SaveCancel component implements simple wrapper over 2 components: Save and Cancel
 * It produces either a or Link, depending on cancelTo property
 * It produces custom-handled button or save button depending on whether onSave prop given
 *
 * @class SaveCancel
 */
const SaveCancel = props => {
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
  } = props;

  const cancelClassName = 'fc-save-cancel__cancel';
  const saveClassName = 'fc-save-cancel__save';

  const cancelControl = cancelTo
    ? (
        <Link
          to={cancelTo}
          className={classNames('fc-btn-link', cancelClassName)}
          params={cancelParams}
          tabIndex={cancelTabIndex}
          disabled={cancelDisabled}>
          {cancelText}
        </Link>
    ) : (
      <a
        onClick={onCancel}
        className={classNames('fc-btn-link', cancelClassName)}
        href="javascript:void(0)"
        tabIndex={saveTabIndex}
        disabled={cancelDisabled}>
        {cancelText}
      </a>
    );

  const saveControl = onSave
    ? (
      <PrimaryButton
        onClick={onSave}
        className={saveClassName}
        tabIndex={cancelTabIndex}
        isLoading={isLoading}
        disabled={saveDisabled}>
        {saveText}
      </PrimaryButton>
    ) : (
      <PrimaryButton
        type="submit"
        className={saveClassName}
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
};

SaveCancel.propTypes = {
  className: PropTypes.string,
  cancelTabIndex: PropTypes.string,
  cancelTo: PropTypes.string,
  cancelParams: PropTypes.object,
  onCancel: PropTypes.func,
  saveTabIndex: PropTypes.string,
  cancelText: PropTypes.string,
  cancelDisabled: PropTypes.bool,
  onSave: PropTypes.func,
  saveText: PropTypes.string,
  saveDisabled: PropTypes.bool,
  isLoading: PropTypes.bool,
};

SaveCancel.defaultProps = {
  cancelTabIndex: '0',
  cancelText: 'Cancel',
  saveTabIndex: '1',
  saveText: 'Save',
};

export default SaveCancel;
