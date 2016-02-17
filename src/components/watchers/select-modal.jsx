// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import { WatcherTypeahead } from '../fields';
import SaveCancel from '../common/save-cancel';

export default class SelectWatcherModal extends React.Component {
  static propTypes = {
    isVisible: PropTypes.bool,
    entityType: PropTypes.string.isRequired,
    onCancel: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    suggested: PropTypes.array.isRequired,
    selected: PropTypes.array.isRequired,
    suggestWatchers: PropTypes.func.isRequired,
    onSelectItem: PropTypes.func.isRequired,
    onDeselectItem: PropTypes.func.isRequired,
  };

  get title() {
    return `Assign ${this.props.entityType}`;
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
    const {isVisible, suggested, selected, suggestWatchers, onSelectItem, onDeselectItem} = this.props;

    return (
      <ModalContainer isVisible={isVisible}>
        <ContentBox
          title={this.title}
          actionBlock={this.actionBlock}
          footer={this.footer}
          className="fc-add-watcher-modal">
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <WatcherTypeahead
              label={this.text}
              suggested={suggested}
              selected={selected}
              suggestWatchers={suggestWatchers}
              onSelectItem={onSelectItem}
              onDeselectItem={onDeselectItem} />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
};
