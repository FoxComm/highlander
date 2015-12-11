
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
    super(args);
    this.state = {
      query: ''
    };
  }

  render() {
    const props = this.props;
    const title = `Assign ${props.entity.entityType}`;
    const text = `${title} to:`;

    const actionBlock = (
      <a className='fc-modal-close' onClick={props.cancelAction}>
        <i className='icon-close'></i>
      </a>
    );

    const footer = (
      <div className="fc-modal-footer fc-add-watcher-modal__footer">
        <a className="fc-btn-link"
           onClick={props.cancelAction}>Cancel</a>
        <PrimaryButton onClick={props.cancelAction}>
          Assign
        </PrimaryButton>
      </div>
    );

    const pilledInput = (
      <PilledInput
        value={this.state.query}
        onChange={(e) => this.setState({query: e.target.value})}
        pills={props.selectedWatchers.map(user => user.name)}
        icon={null}
        onPillClose={_.noop} />
    );

    const typeaheadItem = props => {
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

    return (
      <ModalContainer isVisible={props.isVisible}>
        <ContentBox title={title}
                    actionBlock={actionBlock}
                    footer={footer}
                    className="fc-add-watcher-modal">
          <div className="fc-modal-body fc-add-watcher-modal__content">
            <Typeahead
              className="_no-search-icon"
              labelClass="fc-add-watcher-modal__label"
              fetchItems={props.suggestCustomers}
              minQueryLength={2}
              component={typeaheadItem}
              items={props.suggestedItems}
              label={text}
              name="customerQuery"
              placeholder="Name or email..."
              inputElement={pilledInput}
              onItemSelected={props.onItemSelected} />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
};

AddWatcherModal.propTypes = {
  isVisible: PropTypes.bool,
  entity: PropTypes.shape({
    entityType: PropTypes.string,
    entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  }).isRequired,
  cancelAction: PropTypes.func.isRequired,
  suggestCustomers: PropTypes.func.isRequired,
  suggestedItems: PropTypes.array.isRequired,
  selectedWatchers: PropTypes.array,
  onItemSelected: PropTypes.func
};

AddWatcherModal.defaultProps = {
  isVisible: false,
  selectedWatchers: [],
  query: ''
};

