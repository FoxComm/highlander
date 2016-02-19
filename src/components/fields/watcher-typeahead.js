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


function mapStateToProps(state, {storePath, entity}) {
  const path = getStorePath(storePath, entity, 'watchers', 'selectModal');

  console.debug('map state to props of WatcherTypeahead');
  const {
    term = null,
    suggested = [],
    selected = []
  } = _.get(state, path, {});

  return {term, suggested, selected};
}

function mapDispatchToProps(dispatch, {entity: {entityType, entityId}}) {
  const {actions} = getStore('watchers', entityType);

  console.debug('map dispatch to props of WatcherTypeahead');
  return {
    setTerm: term => dispatch(actions.setTerm(entityId, term)),
    suggestWatchers: () => dispatch(actions.suggestWatchers(entityId)),
    onSelectItem: item => dispatch(actions.selectItem(entityId, item)),
    onDeselectItem: index => dispatch(actions.deselectItem(entityId, index)),
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class WatcherTypeahead extends React.Component {

  static propTypes = {
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

  static defaultProps = {
    storePath: '',
  };

  username(user) {
    return user.name
      ? user.name
      : `${user.firstName} ${user.lastName}`;
  }

  @autobind
  onItemDeselected(name, index) {
    this.props.onDeselectItem(index);
  }

  get pilledInput() {
    const {term, setTerm, selected} = this.props;
    const pills = selected.map(this.username);

    return (
      <PilledInput
        solid={true}
        autofocus={true}
        value={term}
        onChange={({target}) => setTerm(target.value)}
        pills={pills}
        icon={null}
        onPillClose={this.onItemDeselected} />
    );
  }

  @autobind
  onItemSelected(item) {
    const {setTerm, selected, onSelectItem} = this.props;

    if (_.findIndex(selected, ({id}) => id === item.id) < 0) {
      setTerm('');
      onSelectItem(item);
    }
  }

  @autobind
  typeaheadItem(props) {
    const item = props.model;
    const name = this.username(item);

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
  }

  render() {
    const {className, label, suggested, suggestWatchers} = this.props;

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
          component={this.typeaheadItem}
          items={suggested}
          name="watchersSelect"
          placeholder="Name or email..."
          inputElement={this.pilledInput}
          onItemSelected={this.onItemSelected} />
      </div>
    );
  }
}
