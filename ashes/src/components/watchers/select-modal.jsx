// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { numberize } from 'lib/text-utils';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';
import WatcherTypeahead from '../watcher-typeahead/watcher-typeahead';


function mapStateToProps(state, { storePath, entity: { entityType, entityId } }) {
  const path = _.compact([storePath, entityType, 'watchers', entityId, 'selectModal']).join('.');

  const {
    displayed = false,
    selected = [],
    group
  } = _.get(state, path, {});

  return {
    isVisible: displayed,
    selected,
    group,
  };
}

function SelectWatcherModal(props) {
  const { isVisible, storePath, entity, group } = props;

  return (
    <ModalContainer isVisible={isVisible}>
      <ContentBox
        title={renderTitle(group, entity)}
        actionBlock={renderActionBlock(props)}
        footer={renderFooter(props)}
        className="fc-add-watcher-modal">
        <div className="fc-modal-body fc-add-watcher-modal__content">
          <WatcherTypeahead
            storePath={storePath}
            entity={entity}
            hideOnBlur={true}
            label={renderText(group, entity)}
          />
        </div>
      </ContentBox>
    </ModalContainer>
  );
}

function renderTitle(group, { entityType }) {
  const entity = _.capitalize(numberize(entityType, 1));
  switch (group) {
    case 'watchers':
      return `Watch ${entity}`;
    case 'assignees':
    default:
      return `Assign ${entity}`;
  }
}

function renderText(group, { entityType }) {
  const entity = numberize(entityType, 1);
  switch (group) {
    case 'watchers':
      return 'Watchers:';
    case 'assignees':
    default:
      return `Assign ${entity} to:`;
  }
}

function renderActionBlock({ onCancel }) {
  return (
    <a className='fc-modal-close' onClick={onCancel}>
      <i className='icon-close'></i>
    </a>
  );
}

function renderFooter({ onCancel, onConfirm, selected, group }) {
  const saveLabel = group === 'watchers' ? 'Watch' : 'Assign';
  return (
    <SaveCancel
      className="fc-modal-footer fc-add-watcher-modal__footer"
      onCancel={onCancel}
      onSave={onConfirm}
      saveDisabled={!selected.length}
      saveText={saveLabel}/>
  );
}

SelectWatcherModal.propTypes = {
  storePath: PropTypes.string,
  group: PropTypes.string,
  entity: PropTypes.shape({
    entityType: PropTypes.string.isRequired,
    entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  }).isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,

  //connected
  isVisible: PropTypes.bool,
  selected: PropTypes.array.isRequired,
};

export default connect(mapStateToProps)(SelectWatcherModal);
