/* @flow */

import React  from 'react';

import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';

type Props = {
  title: React$Element|string,
  label: React$Element|string,
  onCancel: Function,
  onConfirm: Function,
  cancelText?: string,
  saveText?: string,
  saveDisabled?: boolean,
};

const ModalBase = (props: Props) => {
  const { title, label, onCancel, onConfirm, cancelText = 'No', saveText = 'Yes', saveDisabled = false } = props;
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;

  return (
    <ContentBox
      title={title}
      className="fc-bulk-action-modal"
      actionBlock={actionBlock}
    >
      <div className="fc-modal-body">{label}</div>
      <SaveCancel
        className="fc-modal-footer"
        cancelTabIndex="2"
        cancelText={cancelText}
        onCancel={onCancel}
        saveTabIndex="1"
        onSave={onConfirm}
        saveText={saveText}
        saveDisabled={saveDisabled}
      />
    </ContentBox>
  );
};

export default ModalBase;
