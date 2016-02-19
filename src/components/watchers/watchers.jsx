// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// data
import { groups, emptyTitle } from '../../paragons/watcher';

//helpers
import { getStore } from '../../lib/store-creator';

// components
import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';
import UserInitials from '../users/initials';
import { Button } from '../common/buttons';
import SelectWatcherModal from './select-modal';


const maxDisplayed = 7;


function mapDispatchToProps(dispatch, {entity: {entityType, entityId}}) {
  const {actions} = getStore('watchers', entityType);

  console.debug('map dispatch to props of Watchers');
  return {
    showSelectModal: (group) => dispatch(actions.showSelectModal(entityId, group)),
    hideSelectModal: () => dispatch(actions.hideSelectModal(entityId)),
    toggleListModal: (group) => dispatch(actions.toggleListModal(entityId, group)),
    addWatchers: () => dispatch(actions.addWatchers(entityId)),
    removeWatcher: (group, id) => dispatch(actions.removeWatcher(entityId, group, id)),
  };
}

@connect(null, mapDispatchToProps)
export default class Watchers extends React.Component {

  static propTypes = {
    storePath: PropTypes.string,
    entity: PropTypes.shape({
      entityType: PropTypes.string.isRequired,
      entityId: PropTypes.string.isRequired,
    }).isRequired,
    data: PropTypes.object.isRequired,

    //connected
    showSelectModal: PropTypes.func.isRequired,
    hideSelectModal: PropTypes.func.isRequired,
    toggleListModal: PropTypes.func.isRequired,
    addWatchers: PropTypes.func.isRequired,
    removeWatcher: PropTypes.func.isRequired,
  };

  renderGroup(group) {
    const {data, showSelectModal} = this.props;
    const users = _.get(data, [group, 'entries'], []);

    return (
      <div className={classNames('fc-watchers__users-row', `fc-watchers__${group}-row`)}>
        <AddButton className="fc-watchers__add-button"
                   onClick={() => showSelectModal(group)} />
        {this.renderRow(group, users)}
      </div>
    );
  }

  @autobind
  renderRow(group, users) {
    //empty label if nothing to show
    if (_.isEmpty(users)) {
      return (
        <div className={classNames('fc-watchers__empty-list', `fc-watchers__${group}-empty`)}>
          {emptyTitle[group]}
        </div>
      );
    }

    const removeWatcher = (id) => this.props.removeWatcher(group, id);

    if (users.length <= maxDisplayed) {
      return users.map((user) => this.renderCell(group, user, removeWatcher));
    }

    const displayedUsers = users.slice(0, maxDisplayed - 1);
    const hiddenUsers = users.slice(maxDisplayed - 1);

    const displayedCells = displayedUsers.map((user) => this.renderCell(group, user, removeWatcher));
    const hiddenCells = hiddenUsers.map((user) => this.renderCell(group, user, removeWatcher, true));

    return [
      displayedCells,
      this.renderHiddenRow(hiddenCells, group),
    ];
  }

  @autobind
  renderHiddenRow(group, cells) {
    const {data, toggleListModal} = this.props;
    const active = _.get(data, [group, 'listModalDisplayed'], false);

    const hiddenBlockClass = classNames('fc-watchers__rest-block', {'_shown': active});
    const hiddenBlockOverlayClass = classNames('fc-watchers__rest-block-overlay', {'_shown': active});
    const buttonClass = classNames('fc-watchers__toggle-watchers-btn', {'_active': active});

    return (
      <div className="fc-watchers__rest-cell">
        <Button icon="ellipsis"
                className={buttonClass}
                onClick={() => toggleListModal(group)} />
        <div className={hiddenBlockOverlayClass}
             onClick={() => toggleListModal(group)}></div>
        <div className={hiddenBlockClass}>
          <div className="fc-watchers__users-row">
            {cells}
          </div>
        </div>
      </div>
    );
  }

  renderCell(group, user, removeWatcher, hidden = false) {
    const {id, name, firstName, lastName, email} = user;
    const key = hidden ? `cell-hidden-${group}-${name}` : `cell-${group}-${name}`;

    const actionBlock = (
      <Button icon="close" onClick={() => removeWatcher(id)} />
    );

    return (
      <div className="fc-watchers__cell" key={key}>
        <UserInitials name={name}
                      firstName={firstName}
                      lastName={lastName}
                      email={email}
                      actionBlock={actionBlock}
                      showTooltipOnClick={true} />
      </div>
    );
  }

  render() {
    const {entity, data, addWatchers, hideSelectModal} = this.props;

    return (
      <Panel className="fc-watchers">
        <div className="fc-watchers__container">
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Assignees
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" href="#">take it</a>
            </div>
          </div>
          <div className="fc-watchers__users-row fc-watchers__assignees">
            {this.renderGroup(groups.assignees)}
          </div>
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Watchers
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" href="#">watch</a>
            </div>
          </div>
          <div className="fc-watchers__users-row fc-watchers__watchers">
            {this.renderGroup(groups.watchers)}
          </div>
        </div>
        <SelectWatcherModal
          entity={entity}
          onCancel={hideSelectModal}
          onConfirm={addWatchers} />
      </Panel>
    );
  }

}
