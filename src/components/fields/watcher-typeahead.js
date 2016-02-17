// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import Typeahead from '../typeahead/typeahead';
import PilledInput from '../pilled-search/pilled-input';
import UserInitials from '../users/initials';


export default class WatcherTypeahead extends React.Component {

  static propTypes = {
    className: PropTypes.string,
    label: PropTypes.string,
    suggested: PropTypes.array.isRequired,
    selected: PropTypes.array.isRequired,
    suggestWatchers: PropTypes.func.isRequired,
    onSelectItem: PropTypes.func.isRequired,
    onDeselectItem: PropTypes.func.isRequired,
  };

  state = {
    term: '',
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
    const pills = this.props.selected.map(this.username);

    return (
      <PilledInput
        solid={true}
        autofocus={true}
        value={this.state.term}
        onChange={({target}) => this.setState({term: target.value})}
        pills={pills}
        icon={null}
        onPillClose={this.onItemDeselected} />
    );
  }

  @autobind
  onItemSelected(item) {
    const {selected, onSelectItem} = this.props;

    if (_.findIndex(selected, ({id}) => id === item.id) < 0) {
      this.setState({term: ''}, () => onSelectItem(item));
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
