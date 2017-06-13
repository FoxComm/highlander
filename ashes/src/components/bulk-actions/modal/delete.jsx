/* @flow */
import _ from 'lodash';
import React from 'react';

// helpers
import { numberize } from 'lib/text-utils';

// components
import wrapModal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';

type Props = {
  entity: string;
  stateTitle: string;
  label?: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

const ChangeStateModal = (props: Props) => {
  const {entity, stateTitle, count, label: rawLabel, onCancel, onConfirm} = props;
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to <b>{stateTitle} {count} {entityForm}</b>?</span>;

  return (
    <ContentBox title={`Archive ${_.capitalize(entityForm)}?`}
                className="fc-bulk-action-modal"
                actionBlock={actionBlock}>
      <div className="fc-modal-body">
        {label}
      </div>
      <SaveCancel className="fc-modal-footer"
                  cancelTabIndex="2"
                  cancelText="No"
                  onCancel={onCancel}
                  saveTabIndex="1"
                  onSave={onConfirm}
                  saveText="Yes, Archive" />
    </ContentBox>
  );
};

const Wrapped: Class<React.Component<void, Props, any>> = wrapModal(ChangeStateModal);

export default Wrapped;
