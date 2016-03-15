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
import WatcherTypeaheadItem from './watcher-typeahead-item';


const mapStateToProps = (state, { storePath, entity, fieldName = 'watchers' }) => {
  const path = getStorePath(storePath, entity, fieldName, 'selectModal');

  const {
    term = null,
    suggested = [],
    selected = [],
    isFetching
  } = _.get(state, path, {});

  return { term, suggested, selected, isFetching };
};

const mapDispatchToProps = (dispatch, { entity: { entityType, entityId }, fieldName = 'watchers' }) => {
  const { actions } = getStore(fieldName, entityType);

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
        placeholder="Name or email..."
        inputElement={renderPilledInput(props)}
        hideOnBlur={hideOnBlur}
        onItemSelected={(item, event) => selectItem(props,item, event)}/>
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
};

/**
 * WatcherTypeahead component default props values
 */
WatcherTypeahead.defaultProps = {
  storePath: '',
  hideOnBlur: false
};

export default connect(mapStateToProps, mapDispatchToProps)(WatcherTypeahead);
