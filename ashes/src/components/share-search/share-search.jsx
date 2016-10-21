/* flow */

/** Libs */
import _ from 'lodash';
import { connect } from 'react-redux';
import React, { Component, PropTypes } from 'react';

/** Component */
import { ModalContainer } from '../modal/base';
import ContentBox from '../content-box/content-box';

import { PrimaryButton } from '../common/buttons';
import WaitAnimation from '../common/wait-animation';
import PilledInput from '../pilled-search/pilled-input';
import Typeahead from '../typeahead/typeahead';
import TypeaheadItem from '../watcher-typeahead/watcher-typeahead-item';
import Alert from '../alerts/alert';

class ShareSearch extends Component {
  static propTypes = {
    search: PropTypes.object.isRequired,
    shares: PropTypes.object.isRequired,
    title: PropTypes.string.isRequired,
    isVisible: PropTypes.bool.isRequired,
    maxUsers: PropTypes.number,
    fetchAssociations: PropTypes.func.isRequired,
    suggestAssociations: PropTypes.func.isRequired,
    associateSearch: PropTypes.func.isRequired,
    dissociateSearch: PropTypes.func.isRequired,
    selectItem: PropTypes.func.isRequired,
    deselectItem: PropTypes.func.isRequired,
    setTerm: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired,
  };

  static defaultProps = {
    maxUsers: 3,
  };

  state = {
    firstLoad: true,
    numberUpdatedUsers: false,
    failed: false
  };

  componentWillReceiveProps(nextProps) {
    if (this.props.search.code != nextProps.search.code) {
      this.setState({
        firstLoad: true,
        numberUpdatedUsers: 0,
      });

      return this.props.fetchAssociations(nextProps.search);
    }

    const state = { ...this.state };

    const numberUpdatedUsers = nextProps.shares.associations.length - this.props.shares.associations.length;

    if (numberUpdatedUsers && !state.firstLoad) {
      state.numberUpdatedUsers = numberUpdatedUsers;
    }

    if (this.props.shares.isFetchingAssociations && !nextProps.shares.isFetchingAssociations) {
      state.firstLoad = false;
    }

    this.setState(state);
  }

  get closeAction() {
    return (
      <a className='fc-modal-close' onClick={this.props.onClose}>
        <i className='icon-close' />
      </a>
    );
  }

  get title() {
    return <span>Share Search: <strong>{this.props.title}</strong></span>;
  }

  get alert() {
    const { numberUpdatedUsers, failed } = this.state;
    if (numberUpdatedUsers || failed) {
      let label = '';
      if (failed) {
        label = `Failed updating data.`;
      } else if (numberUpdatedUsers > 0) {
        label = `Search was successfully shared with ${Math.abs(numberUpdatedUsers)} users.`;
      } else {
        label = `Search was successfully unshared from ${Math.abs(numberUpdatedUsers)} users.`;
      }

      return (
        <Alert type={numberUpdatedUsers ? Alert.SUCCESS : Alert.ERROR}
               closeAction={this.setState.bind(this, {numberUpdatedUsers: 0}, null)}>
          <span>{label}</span>
        </Alert>
      );
    }
  }

  get associationsList() {
    const { shares: { isFetchingAssociations = false, associations = [] }, search } = this.props;

    if (isFetchingAssociations) {
      return <WaitAnimation size="s" />;
    }

    const associationsNumber = associations.length ? associations.length : '...';

    return (
      <div>
        <p>Shared with <strong>{associationsNumber}</strong> users:</p>
        <ul className="fc-share-search__associations-list">
          {associations.map(item => {
            const isOwner = item.id === search.storeAdminId;
            const closeHandler = isOwner ? _.noop : this.props.dissociateSearch.bind(null, search, item.id);
            const closeButtonClass = isOwner ? '_disabled' : '';

            return (
              <li key={item.id}>
                <span>{item.name}</span>
                <span className="fc-share-search__associations-owner">{isOwner ? 'Owner' : ''}</span>
                <span>{item.email}</span>
                <span>
                  <a className={closeButtonClass} onClick={closeHandler}>
                    &times;
                  </a>
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    );
  }

  render() {
    const { search, shares } = this.props;
    const { isFetchingSuggestions = false, isUpdatingAssociations = false, suggested = [], selected = [] } = shares;

    return (
      <ModalContainer isVisible={this.props.isVisible}>
        <div className="fc-share-search">
          <div className="fc-modal-container">
            <ContentBox title={this.title} actionBlock={this.closeAction}>
              {this.alert}
              <div className="fc-share-search-typeahead__label">
                <label>Invite Users</label>
              </div>
              <Typeahead
                className="fc-share-search__typeahead _no-search-icon"
                isFetching={isFetchingSuggestions}
                fetchItems={this.props.suggestAssociations}
                minQueryLength={1}
                component={TypeaheadItem}
                items={suggested}
                name="watchersSelect"
                placeholder="Name or email..."
                inputElement={renderPilledInput(this.props)}
                hideOnBlur={true}
                onItemSelected={selectItem.bind(null, this.props)} />

              <div className="fc-share-search__controls">
                <PrimaryButton className="fc-align-right"
                               isLoading={isUpdatingAssociations}
                               onClick={this.props.associateSearch.bind(null, search, selected)}
                               disabled={!selected || selected.length === 0}>
                  Share
                </PrimaryButton>
              </div>

              <div className="fc-share-search__associations">
                {this.associationsList}
              </div>
            </ContentBox>
          </div>
        </div>
      </ModalContainer>
    );
  }
}

const selectItem = ({ setTerm, selectItem, shares: { selected = [] } }, item, event) => {
  if (_.findIndex(selected, ({ id }) => id === item.id) < 0) {
    setTerm('');
    selectItem(item);
  } else {
    event.preventHiding();
  }
};

const renderPilledInput = (props) => {
  const { setTerm, maxUsers, deselectItem, shares: { term = '', selected = [] } } = props;
  const pills = selected.map(user => user.name ? user.name : `${user.firstName} ${user.lastName}`);

  return (
    <PilledInput
      solid={true}
      autoFocus={true}
      value={term}
      disabled={selected.length >= maxUsers}
      onChange={({target}) => setTerm(target.value)}
      pills={pills}
      icon={null}
      onPillClose={(name,index) => deselectItem(index)} />
  );
};

renderPilledInput.propTypes = {
  setTerm: PropTypes.func,
  maxUsers: PropTypes.number,
  deselectItem: PropTypes.func,
  shares: PropTypes.object,
};

const mapStateToProps = (state, props) => {
  const search = _.invoke(state, `${props.entity}.currentSearch`);

  return {
    search,
    shares: _.get(search, 'shares', {}),
  };
};

export default connect(mapStateToProps)(ShareSearch);
