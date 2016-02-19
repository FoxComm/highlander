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

  console.debug('map state to props of SelectWatcherModal');
  return {
    isVisible: displayed,
    selected,
  };
}

@connect(mapStateToProps)
export default class SelectWatcherModal extends React.Component {
  static propTypes = {
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

  get title() {
    return `Assign ${this.props.entity.entityType}`;
  }

  get text() {
    return `${this.title} to:`;
  }

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.onCancel}>
        <i className='icon-close'></i>
      </a>
    );
  }

  get footer() {
    const {onCancel, onConfirm, selected} = this.props;

    return (
      <SaveCancel
        className="fc-modal-footer fc-add-watcher-modal__footer"
        onCancel={onCancel}
        onSave={onConfirm}
        saveDisabled={!selected.length}
        saveText="Assign" />
    );
  }

  render() {
    const {isVisible, storePath, entity} = this.props;

    return (
      <ModalContainer isVisible={isVisible}>
        <ContentBox
          title={this.title}
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal">
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <WatcherTypeahead
              storePath={storePath}
              entity={entity}
              label={this.text} />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
};
