// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import Typeahead from '../typeahead/typeahead';
import UserInitials from '../users/initials';
import PilledInput from '../pilled-search/pilled-input';
import SaveCancel from '../common/save-cancel';

export default class AddWatcherModal extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      query: ''
    };
  }

  static propTypes = {
    isVisible: PropTypes.bool,
    entity: PropTypes.shape({
      entityType: PropTypes.string,
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    }).isRequired,
    cancelAction: PropTypes.func.isRequired,
    suggestWatchers: PropTypes.func.isRequired,
    suggestedItems: PropTypes.array.isRequired,
    selectedWatchers: PropTypes.array,
    onItemSelected: PropTypes.func,
    onAddClick: PropTypes.func,
    onDeleteClick: PropTypes.func
  };

  static defaultProps = {
    isVisible: false,
    selectedWatchers: [],
    query: ''
  };

  get title() {
    return `Assign ${this.props.entity.entityType}`;
  }

  get text() {
    return `${this.title} to:`;
  }

  get actionBlock() {
    return (
      <a className='fc-modal-close' onClick={this.props.cancelAction}>
        <i className='icon-close'></i>
      </a>
    );
  }

  get footer() {
    const {cancelAction, onAddClick} = this.props;

    return (
      <SaveCancel className="fc-modal-footer fc-add-watcher-modal__footer"
                  onCancel={cancelAction}
                  onSave={onAddClick}
                  saveText="Assign" />
    );
  }

  username(user) {
    return user.name
      ? user.name
      : `${user.firstName} ${user.lastName}`;
  }

  get pilledInput() {
    const pills = this.props.selectedWatchers.map(this.username);

    return (
      <PilledInput
        autofocus={true}
        value={this.state.query}
        onChange={(e) => this.setState({query: e.target.value})}
        pills={pills}
        icon={null}
        onPillClose={(name, idx) => this.props.onDeleteClick(name, idx)} />
    );
  }

  onItemSelected(name, idx) {
    this.setState({query: ''}, () => this.props.onItemSelected(name, idx));
  }

  @autobind
  typeaheadItem(props) {
    const item = props.model;
    const name = this.username(item);

    return (
      <div className="fc-add-watcher-modal__typeahead-item">
        <div className="fc-add-watcher-modal__typeahead-item-icon">
          <UserInitials name={name} email={item.email} />
        </div>
        <div className="fc-add-watcher-modal__typeahead-item-name">
          {name}
        </div>
        <div className="fc-add-watcher-modal__typeahead-item-email">
          {item.email}
        </div>
      </div>
    );
  }

  render() {
    const {isVisible, suggestWatchers, suggestedItems} = this.props;

    return (
      <ModalContainer isVisible={isVisible}>
        <ContentBox title={this.title}
                    actionBlock={this.actionBlock}
                    footer={this.footer}
                    className="fc-add-watcher-modal">
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <Typeahead
              className="_no-search-icon"
              labelClass="fc-add-watcher-modal__label"
              isFetching={false}
              fetchItems={suggestWatchers}
              minQueryLength={2}
              component={this.typeaheadItem}
              items={suggestedItems}
              label={this.text}
              name="customerQuery"
              placeholder="Name or email..."
              inputElement={this.pilledInput}
              onItemSelected={(name, idx) => this.onItemSelected(name, idx)} />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
};
