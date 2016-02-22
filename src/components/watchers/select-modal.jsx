// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

//helpers
import { getStorePath } from '../../lib/store-utils';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import { WatcherTypeahead } from '../fields';
import SaveCancel from '../common/save-cancel';


function mapStateToProps(state, {storePath, entity}) {
  const path = getStorePath(storePath, entity, 'watchers', 'selectModal');

  const {
    displayed = false,
    selected = [],
  } = _.get(state, path, {});

  return {
    isVisible: displayed,
    selected,
  };
}

function SelectWatcherModal(props) {
  const {isVisible, storePath, entity} = props;

  return (
    <ModalContainer isVisible={isVisible}>
      <ContentBox
        title={renderTitle(entity)}
        actionBlock={renderActionBlock(props)}
        footer={renderFooter(props)}
        className="fc-add-watcher-modal">
        <div className="fc-modal-body fc-add-watcher-modal__content">
          <WatcherTypeahead
            storePath={storePath}
            entity={entity}
            label={renderText(entity)} />
        </div>
      </ContentBox>
    </ModalContainer>
  );
}

function renderTitle({entityType}) {
  return `Assign ${entityType}`;
}

function renderText({entityType}) {
  return `Assign ${entityType} to:`;
}

function renderActionBlock({onCancel}) {
  return (
    <a className='fc-modal-close' onClick={onCancel}>
      <i className='icon-close'></i>
    </a>
  );
}

function renderFooter({onCancel, onConfirm, selected}) {
  return (
    <SaveCancel
      className="fc-modal-footer fc-add-watcher-modal__footer"
      onCancel={onCancel}
      onSave={onConfirm}
      saveDisabled={!selected.length}
      saveText="Assign" />
  );
}

SelectWatcherModal.propTypes = {
  storePath: PropTypes.string,
  entity: PropTypes.shape({
    entityType: PropTypes.string.isRequired,
    entityId: PropTypes.string.isRequired,
  }).isRequired,
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,

  //connected
  isVisible: PropTypes.bool,
  selected: PropTypes.array.isRequired,
};

export default connect(mapStateToProps)(SelectWatcherModal);
