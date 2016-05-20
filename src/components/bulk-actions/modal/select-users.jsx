// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

// helpers
import { numberize } from '../../../lib/text-utils';
import { getStorePath } from '../../../lib/store-utils';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import WatcherTypeahead from '../../watcher-typeahead/watcher-typeahead';
import SaveCancel from '../../common/save-cancel';


const mapStateToProps = (state, {storePath, module}) => {
  const path = getStorePath(storePath, module, 'watchers', 'list', 'selectModal', 'selected');

  return {
    selected: _.get(state, path, []).map(({id}) => id),
  };
};

const SelectUsersModal = (props) => {
  const { module,
          action,
          entity,
          count,
          labelPrefix,
          maxUsers,
          onCancel,
          onConfirm,
          selected } = props;

  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const actionForm = _.capitalize(action);
  const entityForm = numberize(entity, count);
  const label = <span>{labelPrefix} <strong>{count}</strong> {entityForm}</span>;

  return (
    <ContentBox title={`${actionForm} ${_.capitalize(entityForm)}`}
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
      <SaveCancel className="fc-modal-footer"
                  cancelTabIndex="2"
                  cancelClassName="fc-modal-close"
                  cancelText="No"
                  onCancel={onCancel}
                  saveTabIndex="1"
                  onSave={onConfirm}
                  saveText={actionForm}
                  saveDisabled={!selected.length} />
    </ContentBox>
  );
};

SelectUsersModal.propTypes = {
  module: PropTypes.string.isRequired,
  storePath: PropTypes.string,
  action: PropTypes.string.isRequired,
  entity: PropTypes.string.isRequired,
  count: PropTypes.number.isRequired,
  labelPrefix: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.node,
  ]).isRequired,
  maxUsers: PropTypes.number.isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,

  //connected
  selected: PropTypes.array.isRequired,
};

SelectUsersModal.defaultProps = {
  storePath: '',
};

export default connect(mapStateToProps)(modal(SelectUsersModal));
