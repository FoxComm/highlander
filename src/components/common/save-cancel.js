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
    cancelTo,
    cancelParams,
    onCancel,
    cancelText,
    cancelClassName,
    onSave,
    saveText,
    saveClassName
  } = props;

  const cancelControl = cancelTo
    ? <Link to={cancelTo}
            className={classNames('fc-btn-link', cancelClassName)}
            params={cancelParams}>{cancelText}</Link>
    : <a onClick={onCancel}
         className={classNames('fc-btn-link', cancelClassName)}
         href="javascript:void(0)">{cancelText}</a>;

  const saveControl = onSave
    ? <PrimaryButton onClick={onSave}
                     className={saveClassName}>{saveText}</PrimaryButton>
    : <PrimaryButton type="submit"
                     className={saveClassName}>{saveText}</PrimaryButton>;

  return (
    <div className={className}>
      {cancelControl}
      {saveControl}
    </div>
  );
};

SaveCancel.propTypes = {
  className: PropTypes.string,
  cancelTo: PropTypes.string,
  cancelParams: PropTypes.object,
  onCancel: PropTypes.func,
  cancelText: PropTypes.string.isRequired,
  cancelClassName: PropTypes.string,
  onSave: PropTypes.func,
  saveText: PropTypes.string.isRequired,
  saveClassName: PropTypes.string
};

SaveCancel.defaultProps = {
  cancelText: 'Cancel',
  saveText: 'Save'
};

export default SaveCancel;
