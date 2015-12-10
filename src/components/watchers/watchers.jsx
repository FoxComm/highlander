
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';
import UserInitials from '../users/initials';
import { Button } from '../common/buttons';
import { ModalContainer } from '../modal/base';

// redux
import * as WatchersActions from '../../modules/watchers';

const Groups = {
  ASSIGNEES: 'assignees',
  WATCHERS: 'watchers'
};

@connect((state, props) => ({
  data: _.get(state.watchers, [props.entity.entityType, props.entity.entityId], {})
}), WatchersActions)
export default class Watchers extends React.Component {

  static propTypes = {
    assignees: PropTypes.array,
    watchers: PropTypes.array,
    entity: PropTypes.shape({
      entityType: PropTypes.string,
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
    }).isRequired
  };

  static defaultProps = {
    assignees: [
      {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
    ],
    watchers: [
      {name: 'Jeff Mataya', email: 'jeff@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Donkey Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Donkey', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'},
      {name: 'Eugene Sypachev', email: 'eugene@foxcommerce.com'}
    ]
  }

  get entity() {
    return this.props.entity;
  }

  get maxDisplayed() {
    return 7;
  }

  get assignees() {
    if (_.isEmpty(this.props.assignees)) {
      return (
        <div className="fc-watchers__assignees-row">
          <AddButton className="fc-watchers__add-button"
                       onClick={() => this.props.showAddingModal(this.entity)}/>
          <div className="fc-watchers__empty-list fc-watchers__assignees-empty">
            Unassigned
          </div>
        </div>
      );
    } else {
      return this.buildRow(this.props.assignees, 'fc-watchers__assignees-row', Groups.ASSIGNEES);
    }
  }

  get watchers() {
    if (_.isEmpty(this.props.watchers)) {
      return (
        <div className="fc-watchers__watchers-row">
          <AddButton className="fc-watchers__add-button"
                       onClick={() => this.props.showAddingModal(this.entity)}/>
          <div className="fc-watchers__empty-list fc-watchers__watchers-empty">
            Unwatched
          </div>
        </div>
      );
    } else {
      return this.buildRow(this.props.watchers, 'fc-watchers__watchers-row', Groups.WATCHERS);
    }
  }

  renderCell(user, key) {
    return (
      <div className="fc-watchers__cell" key={key}>
        <UserInitials name={user.name} email={user.email}/>
      </div>
    );
  }

  @autobind
  buildHiddenRow(hiddenCells, group) {
    const active = _.get(this.props, ['data', group, 'displayed'], false);
    const hiddenBlockClass = classNames('fc-watchers__rest-block', {
      '_shown': active
    });
    const buttonClass = classNames('fc-watchers__toggle-watchers-btn', {
      '_active': active
    });
    return (
      <div className="fc-watchers__rest-cell">
        <Button icon="list"
                className={buttonClass}
                onClick={() => this.props.toggleWatchers(this.entity, group)} />
        <div className={hiddenBlockClass}>
          <div className="fc-watchers__users-row">
            {hiddenCells}
          </div>
        </div>
      </div>
    );
  }

  @autobind
  buildRow(users, className, group) {
    const rowClass = classNames("fc-watchers__users-row", className);
    if (users.length <= this.maxDisplayed) {
      const cells = users.map((watcher, idx) => this.renderCell(watcher, `cell-${group}-${idx}`));
      return (
        <div className={rowClass}>
          <AddButton className="fc-watchers__add-button"
                     onClick={() => this.props.showAddingModal(this.entity)}/>
          {cells}
        </div>
      );
    } else {
      const displayedWatchers = users.slice(0, this.maxDisplayed - 1);
      const hiddenWatchers = users.slice(this.maxDisplayed - 1);
      const displayedCells = displayedWatchers.map((watcher, idx) => this.renderCell(watcher, `cell-${group}-${idx}`));
      const hiddenCells = hiddenWatchers.map((watcher, idx) => this.renderCell(watcher, `cell-hidden-${group}-${idx}`));
      return (
        <div className={rowClass}>
          <AddButton className="fc-watchers__add-button"
                     onClick={() => this.props.showAddingModal(this.entity)}/>
          {displayedCells}
          {this.buildHiddenRow(hiddenCells, group)}
        </div>
      );
    }
  }

  render() {
    console.log(this.props);
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
            {this.assignees}
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
            {this.watchers}
          </div>
        </div>
        <ModalContainer isVisible={this.props.data.modalDisplayed}>
          Add new watcher!
        </ModalContainer>
      </Panel>
    );
  }

}
