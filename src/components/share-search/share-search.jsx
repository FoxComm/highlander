/** Libs */
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';

/** Component */
import wrapModal from '../modal/wrapper';
import ContentBox from '../content-box/content-box';

import { PrimaryButton } from '../common/buttons';
import WaitAnimation from '../common/wait-animation';
import PilledInput from '../pilled-search/pilled-input';
import Typeahead from '../typeahead/typeahead';
import TypeaheadItem from '../watcher-typeahed/watcher-typeahead-item';
import Alert from '../alerts/alert';

@wrapModal
export default class ShareSearch extends Component {

  static propTypes = {
    search: PropTypes.object.isRequired,
    fetchAssociations: PropTypes.func.isRequired,
    suggestAssociations: PropTypes.func.isRequired,
    associateSearch: PropTypes.func.isRequired,
    dissociateSearch: PropTypes.func.isRequired,
    selectItem: PropTypes.func.isRequired,
    deselectItem: PropTypes.func.isRequired,
    setTerm: PropTypes.func.isRequired,
    closeAction: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    maxUsers: PropTypes.number,
  };

  static defaultProps = {
    maxUsers: 3,
  };

  state = {
    firstLoad: true,
    numberUpdatedUsers: false,
    failed: false
  };

  componentWillMount() {
    this.props.fetchAssociations(this.props.search);
  }

  componentWillReceiveProps(nextProps) {
    const numberUpdatedUsers = nextProps.search.associations.length - this.props.search.associations.length;

    const state = this.state;

    if (numberUpdatedUsers && !state.firstLoad) {
      state.numberUpdatedUsers = numberUpdatedUsers;
    }

    if (this.props.search.isFetchingAssociations && !nextProps.search.isFetchingAssociations) {
      state.firstLoad = false;
    }

    this.setState(state);
  }

  get closeAction() {
    return (
      <a className='fc-modal-close' onClick={this.props.closeAction}>
        <i className='icon-close'/>
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
               closeAction={this.setState.bind(this, {numberUpdatedUsers: 0})}>
          <span>{label}</span>
        </Alert>
      );
    }
  }

  get associationsList() {
    const { isFetchingAssociations = false, associations = [] } = this.props.search;

    if (isFetchingAssociations) {
      return <WaitAnimation/>;
    }

    const associationsNumber = associations.length ? associations.length : '...';

    return (
      <div>
        <p>Shared with <strong>{associationsNumber}</strong> users:</p>
        <ul>
          {associations.map(item => {
            return (
              <li key={item.id}>
                <span>{item.name}</span>
                <span>{item.email}</span>
                <span>
                  <a onClick={this.props.dissociateSearch.bind(null, this.props.search, item.id)}>
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
    const search = this.props.search;
    const { isFetchingSuggestions = false, isUpdatingAssociations = false, suggested = [] } = search;

    return (
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
              onItemSelected={selectItem.bind(null, this.props)}/>

            <div className="fc-share-search__controls">
              <PrimaryButton className="fc-align-right"
                             isLoading={isUpdatingAssociations}
                             onClick={this.props.associateSearch.bind(null, search, search.selected)}
                             disabled={!search.selected}>
                Share
              </PrimaryButton>
            </div>

            <div className="fc-share-search__associations">
              {this.associationsList}
            </div>
          </ContentBox>
        </div>
      </div>
    );
  }
}

const selectItem = ({ setTerm, selectItem, search: { selected = [] } }, item, event) => {
  if (_.findIndex(selected, ({ id }) => id === item.id) < 0) {
    setTerm('');
    selectItem(item);
  } else {
    event.preventHiding();
  }
};

const renderPilledInput = (props) => {
  const { setTerm, maxUsers, deselectItem, search: { term = '', selected = [] } } = props;
  const pills = selected.map(user => user.name ? user.name : `${user.firstName} ${user.lastName}`);

  return (
    <PilledInput
      solid={true}
      autofocus={true}
      value={term}
      disabled={selected.length >= maxUsers}
      onChange={({target}) => setTerm(target.value)}
      pills={pills}
      icon={null}
      onPillClose={(name,index) => deselectItem(index)}/>
  );
};
