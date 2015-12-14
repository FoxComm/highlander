
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';
import Typeahead from '../typeahead/typeahead';
import UserInitials from '../users/initials';
import PilledInput from '../pilled-search/pilled-input';

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
    suggestCustomers: PropTypes.func.isRequired,
    suggestedItems: PropTypes.array.isRequired,
    selectedWatchers: PropTypes.array,
    onItemSelected: PropTypes.func,
    onAddClick: PropTypes.func
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
  };

  get footer() {
    return (
      <div className="fc-modal-footer fc-add-watcher-modal__footer">
        <a className="fc-btn-link"
           onClick={this.props.cancelAction}>Cancel</a>
        <PrimaryButton onClick={this.props.onAddClick}>
          Assign
        </PrimaryButton>
      </div>
    );
  }

  get pilledInput() {
    return (
      <PilledInput
        value={this.state.query}
        onChange={(e) => this.setState({query: e.target.value})}
        pills={this.props.selectedWatchers.map(user => user.name)}
        icon={null}
        onPillClose={(name, idx) => this.props.onDeleteClick(name, idx)} />
    );
  }

  typeaheadItem(props) {
    const item = props.item;
    return (
      <div className="fc-add-watcher-modal__typeahead-item">
        <div className="fc-add-watcher-modal__typeahead-item-icon">
          <UserInitials name={item.name} email={item.email} />
        </div>
        <div className="fc-add-watcher-modal__typeahead-item-name">
          {item.name}
        </div>
        <div className="fc-add-watcher-modal__typeahead-item-email">
          {item.email}
        </div>
      </div>
    );
  };

  render() {
    const props = this.props;

    return (
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox title={this.title}
                    actionBlock={this.actionBlock}
                    footer={this.footer}
                    className="fc-add-watcher-modal">
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <Typeahead
              className="_no-search-icon"
              labelClass="fc-add-watcher-modal__label"
              fetchItems={props.suggestCustomers}
              minQueryLength={2}
              component={this.typeaheadItem}
              items={props.suggestedItems}
              label={this.text}
              name="customerQuery"
              placeholder="Name or email..."
              inputElement={this.pilledInput}
              onItemSelected={props.onItemSelected} />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
};
