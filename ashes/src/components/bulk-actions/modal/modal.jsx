/* @flow */

import React from 'react';

import modal from 'components/modal/wrapper';
import ContentBox from 'components/content-box/content-box';
import SaveCancel from 'components/common/save-cancel';

type Props = {
  title: Element|string;
  label: Element|string;
  onCancel: Function;
  onConfirm: Function;
};

const ChangeStateModal = (props: Props) => {
  const { title, label, onCancel, onConfirm } = props;
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;

  return (
    <ContentBox
      title={title}
      className="fc-bulk-action-modal"
      actionBlock={actionBlock}
    >
      <div className="fc-modal-body">{label}</div>
      <SaveCancel className="fc-modal-footer"
                  cancelTabIndex="2"
                  cancelText="No"
                  onCancel={onCancel}
                  saveTabIndex="1"
                  onSave={onConfirm}
                  saveText="Yes, Change State"
      />
    </ContentBox>
  );
};

export default modal(ChangeStateModal);
