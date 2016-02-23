// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { numberize } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import SaveCancel from '../../common/save-cancel';


const ChangeStateModal = ({entity, stateTitle, count, label: rawLabel, onCancel, onConfirm}) => {
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to change the state to <b>{stateTitle}</b> for <b>{count} {entityForm}</b>?</span>;

  return (
    <ContentBox title={`Change ${_.capitalize(entityForm)} state to ${stateTitle}?`}
                className="fc-bulk-action-modal"
                actionBlock={actionBlock}>
      <div className="fc-modal-body">
        {label}
      </div>
      <SaveCancel className="fc-modal-footer"
                  cancelTabIndex="2"
                  cancelClassName="fc-modal-close"
                  cancelText="No"
                  onCancel={onCancel}
                  saveTabIndex="1"
                  onSave={onConfirm}
                  saveText="Yes, Change State" />
    </ContentBox>
  );
};

ChangeStateModal.propTypes = {
  entity: PropTypes.string.isRequired,
  stateTitle: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  count: PropTypes.number.isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
};

export default modal(ChangeStateModal);
