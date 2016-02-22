// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// helpers
import { numberize } from '../../../lib/text-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import WatcherTypeahead from '../../fields/watcher-typeahead';
import { PrimaryButton, CloseButton } from '../../common/buttons';


const SelectUsersModal = ({module, action, entity, count, label, maxUsers, onCancel, onConfirm}) => {
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const actionForm = _.capitalize(action);
  const entityForm = numberize(entity, count);

  return (
    <ContentBox title={`${actionForm} ${_.capitalize(entityForm)}?`}
                className="fc-bulk-action-modal"
                actionBlock={actionBlock}>
      <div className='fc-modal-body'>
        {label}:
        <WatcherTypeahead
          entity={{
            entityType: module,
            entityId: 'list',
          }}
          maxUsers={maxUsers} />
      </div>
      <div className='fc-modal-footer'>
        <a tabIndex="2" className="fc-modal-close" onClick={onCancel}>
          No
        </a>
        <PrimaryButton tabIndex="1"
                       autoFocus={true}
                       onClick={onConfirm}>
          {actionForm}
        </PrimaryButton>
      </div>
    </ContentBox>
  );
};

SelectUsersModal.propTypes = {
  module: PropTypes.string.isRequired,
  action: PropTypes.string.isRequired,
  entity: PropTypes.string.isRequired,
  count: PropTypes.number.isRequired,
  label: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]).isRequired,
  maxUsers: PropTypes.number.isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
};

export default modal(SelectUsersModal);
