// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// helpers
import { getStore } from '../../lib/store-creator';

// components
import Typeahead from '../typeahead/typeahead';
import PilledInput from '../pilled-search/pilled-input';
import WatcherTypeaheadItem from './watcher-typeahead-item';


const mapStateToProps = (state, { storePath, entity: { entityType, entityId }, fieldName = 'watchers' }) => {
  let path = _.filter([storePath, entityType, fieldName, entityId, 'selectModal']).join('.');

  const {
    term = null,
    suggested = [],
    selected = [],
    isFetching
  } = _.get(state, path, {});

  return { term, suggested, selected, isFetching };
};

const mapDispatchToProps = (dispatch, { entity: { entityType, entityId }, fieldName = 'watchers' }) => {
  const { actions } = getStore([entityType, fieldName]);

  return {
    setTerm: term => dispatch(actions.setTerm(entityId, term)),
    suggestWatchers: () => dispatch(actions.suggestWatchers(entityId)),
    onSelectItem: item => dispatch(actions.selectItem(entityId, item)),
    onDeselectItem: index => dispatch(actions.deselectItem(entityId, index)),
  };
};

/**
 * Typeahead component for watchers search/select
 *
 * Used for assignment users for entities(watch orders, share searches, etc.)
 */
const WatcherTypeahead = (props) => {
  const { className, label, suggested, suggestWatchers, hideOnBlur, isFetching } = props;

  const placeholder = props.selected.length >= props.maxUsers ? '' : 'Name or email...';

  return (
    <div className={classNames('fc-watcher-typeahead', className)}>
      <div className="fc-watcher-typeahead__label">
        <label>
          {label}
        </label>
      </div>
      <Typeahead
        className="_no-search-icon"
        isFetching={isFetching}
        fetchItems={suggestWatchers}
        minQueryLength={2}
        component={WatcherTypeaheadItem}
        items={suggested}
        name="watchersSelect"
        placeholder={placeholder}
        inputElement={renderPilledInput(props)}
        hideOnBlur={hideOnBlur}
        onItemSelected={(item, event) => selectItem(props, item, event)}/>
    </div>
  );
};

const renderPilledInput = (props) => {
  const { term, setTerm, selected, maxUsers, onDeselectItem } = props;
  const pills = selected.map(getUsername);

  return (
    <PilledInput
      solid={true}
      autofocus={true}
      value={term}
      disabled={selected.length >= maxUsers}
      onChange={({target}) => setTerm(target.value)}
      pills={pills}
      icon={null}
      onPillClose={(name,index) => onDeselectItem(index)}/>
  );
};

const selectItem = ({ setTerm, selected, onSelectItem }, item, event) => {
  if (_.findIndex(selected, ({ id }) => id === item.id) < 0) {
    setTerm('');
    onSelectItem(item);
  } else {
    event.preventHiding();
  }
};

const getUsername = (user) => {
  return user.name ? user.name : `${user.firstName} ${user.lastName}`;
};

renderPilledInput.propTypes = {
  maxUsers: PropTypes.number,
  term: PropTypes.string,
  setTerm: PropTypes.func.isRequired,
  selected: PropTypes.array.isRequired,
  onDeselectItem: PropTypes.func.isRequired,
};

/**
 * WatcherTypeahead component expected props types
 */
WatcherTypeahead.propTypes = {
  storePath: PropTypes.string,
  entity: PropTypes.shape({
    entityType: PropTypes.string.isRequired,
    entityId: PropTypes.string.isRequired,
  }).isRequired,
  className: PropTypes.string,
  label: PropTypes.string,
  maxUsers: PropTypes.number,
  hideOnBlur: PropTypes.bool,

  //connected
  term: PropTypes.string,
  suggested: PropTypes.array.isRequired,
  selected: PropTypes.array.isRequired,
  setTerm: PropTypes.func.isRequired,
  suggestWatchers: PropTypes.func.isRequired,
  onSelectItem: PropTypes.func.isRequired,
  onDeselectItem: PropTypes.func.isRequired,
  isFetching: PropTypes.bool,
};

/**
 * WatcherTypeahead component default props values
 */
WatcherTypeahead.defaultProps = {
  storePath: '',
  hideOnBlur: false
};

export default connect(mapStateToProps, mapDispatchToProps)(WatcherTypeahead);
