// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// helpers
import { getStore } from '../../lib/store-creator';
import { getStorePath } from '../../lib/store-utils';

// components
import Typeahead from '../typeahead/typeahead';
import PilledInput from '../pilled-search/pilled-input';
import UserInitials from '../users/initials';


const mapStateToProps = (state, {storePath, entity}) => {
  const path = getStorePath(storePath, entity, 'watchers', 'selectModal');

  const {
    term = null,
    suggested = [],
    selected = []
  } = _.get(state, path, {});

  return {term, suggested, selected};
};

const mapDispatchToProps = (dispatch, {entity: {entityType, entityId}}) => {
  const {actions} = getStore('watchers', entityType);

  return {
    setTerm: term => dispatch(actions.setTerm(entityId, term)),
    suggestWatchers: () => dispatch(actions.suggestWatchers(entityId)),
    onSelectItem: item => dispatch(actions.selectItem(entityId, item)),
    onDeselectItem: index => dispatch(actions.deselectItem(entityId, index)),
  };
};

const WatcherTypeahead = (props) => {
  const {className, label, suggested, suggestWatchers} = props;

  return (
    <div className={classNames('fc-field-watcher-typeahead', className)}>
      <div className="fc-field-watcher-typeahead__label">
        <label>
          {label}
        </label>
      </div>
      <Typeahead
        className="_no-search-icon"
        isFetching={false}
        fetchItems={suggestWatchers}
        minQueryLength={2}
        component={TypeaheadItem}
        items={suggested}
        name="watchersSelect"
        placeholder="Name or email..."
        inputElement={renderPilledInput(props)}
        onItemSelected={(item) => selectItem(props,item)} />
    </div>
  );
};

const TypeaheadItem = (props) => {
  const item = props.model;
  const name = getUsername(item);

  return (
    <div className="fc-field-watcher-typeahead__item">
      <div className="fc-field-watcher-typeahead__item-icon">
        <UserInitials name={name} email={item.email} />
      </div>
      <div className="fc-field-watcher-typeahead__item-name">
        {name}
      </div>
      <div className="fc-field-watcher-typeahead__item-email">
        {item.email}
      </div>
    </div>
  );
};

const renderPilledInput = (props) => {
  const {term, setTerm, selected, onDeselectItem} = props;
  const pills = selected.map(getUsername);

  return (
    <PilledInput
      solid={true}
      autofocus={true}
      value={term}
      onChange={({target}) => setTerm(target.value)}
      pills={pills}
      icon={null}
      onPillClose={(name,index) => onDeselectItem(index)} />
  );
};

const selectItem = ({setTerm, selected, onSelectItem}, item) => {
  if (_.findIndex(selected, ({id}) => id === item.id) < 0) {
    setTerm('');
    onSelectItem(item);
  }
};

const getUsername = (user) => {
  return user.name
    ? user.name
    : `${user.firstName} ${user.lastName}`;
};


WatcherTypeahead.propTypes = {
  storePath: PropTypes.string,
  entity: PropTypes.shape({
    entityType: PropTypes.string.isRequired,
    entityId: PropTypes.string.isRequired,
  }).isRequired,
  className: PropTypes.string,
  label: PropTypes.string,

  //connected
  term: PropTypes.string,
  suggested: PropTypes.array.isRequired,
  selected: PropTypes.array.isRequired,
  setTerm: PropTypes.func.isRequired,
  suggestWatchers: PropTypes.func.isRequired,
  onSelectItem: PropTypes.func.isRequired,
  onDeselectItem: PropTypes.func.isRequired,
};

WatcherTypeahead.defaultProps = {
  storePath: '',
};

export default connect(mapStateToProps, mapDispatchToProps)(WatcherTypeahead);
