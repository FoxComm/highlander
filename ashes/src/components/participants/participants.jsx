// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './participants.css';
import { connect } from 'react-redux';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { trackEvent } from 'lib/analytics';
import { numberize } from 'lib/text-utils';
import { groups } from 'paragons/participants';

// components
import WaitAnimation from '../common/wait-animation';
import SelectAdminsModal from '../users/select-modal';
import { AddButton, Button } from '../common/buttons';
import DetailedInitials from '../user-initials/detailed-initials';

// actions
import * as actions from 'modules/participants';
import reducer from 'modules/participants';

// types
import type { GroupType } from 'types/participants';
import type { EntityType } from 'types/entity';

type AsyncActions = {
  fetchParticipants: AsyncState,
}

type Props = {
  group: GroupType,
  entity: EntityType,
  title: string,
  emptyTitle: string,
  actionTitle: string,
  // connected
  fetchParticipants: () => AbortablePromise,
  removeParticipant: (id: number) => AbortablePromise,
  addParticipants: (ids: Array<number>) => AbortablePromise,
  participants: Array<Object>,
  asyncActions: AsyncActions,
  maxDisplayed: number,
  currentUser: TUser,
}

type State = {
  restUsersDisplayed: boolean,
  isUsersPopupShown: boolean,
}

function mapGlobalStateToProps(state) {
  return {
    currentUser: state.user.current,
  };
}

function mapDispatchToProps(dispatch, props) {
  const { entity, group } = props;
  return {
    fetchParticipants: () => dispatch(actions.fetchParticipants(entity, group)),
    removeParticipant: (id: number) => dispatch(actions.removeParticipant(entity, group, id)),
    addParticipants: (ids: Array<number>) => dispatch(actions.addParticipants(entity, group, ids)),
  };
}

class Participants extends Component {
  props: Props;
  state: State = {
    restUsersDisplayed: false,
    isUsersPopupShown: false,
  };

  static defaultProps = {
    maxDisplayed: 7,
  };

  componentDidMount() {
    this.props.fetchParticipants();
  }

  @autobind
  handleParticipate(e) {
    e.preventDefault();
    const { props } = this;

    trackEvent(props.title, 'click_assign_self');
    this.props.addParticipants([props.currentUser.id]);
  }

  get isParticipantsLoading(): boolean {
    return _.get(this.props.asyncActions, 'fetchParticipants.inProgress', false);
  }

  @autobind
  toggleRestUsers() {
    this.setState({
      restUsersDisplayed: !this.state.restUsersDisplayed,
    });
  }

  renderHiddenRow(cells) {
    const active = this.state.restUsersDisplayed;

    return (
      <div>
        <Button
          icon="ellipsis"
          styleName="toggle-watchers-btn"
          className={active ? styles['_active'] : void 0}
          onClick={this.toggleRestUsers}
        />
        <div
          styleName="rest-block-overlay"
          className={active ? styles['_shown'] : void 0}
          onClick={this.toggleRestUsers}
        />
        <div styleName="rest-block" styleName={active ? '_shown' : void 0}>
          <div styleName="users-row">
            {cells}
          </div>
        </div>
      </div>
    );
  };

  renderCell(user, hidden = false) {
    const { id } = user;
    const key = hidden ? `cell-hidden-${id}` : `cell-${id}`;

    const actionBlock = (
      <Button icon="close" onClick={() => this.props.removeParticipant(id)} />
    );

    return (
      <div styleName="cell" key={key}>
        <DetailedInitials {...user}
          actionBlock={actionBlock}
          showTooltipOnClick={true}
        />
      </div>
    );
  };

  get usersRow() {
    const { props } = this;
    const users = props.participants;
    if (_.isEmpty(users)) {
      return <div styleName="empty-list">{props.emptyTitle}</div>;
    }

    if (users.length <= props.maxDisplayed) {
      return users.map((user) => this.renderCell(user));
    }

    const displayedUsers = users.slice(0, props.maxDisplayed - 1);
    const hiddenUsers = users.slice(props.maxDisplayed - 1);

    const displayedCells = displayedUsers.map((user) => this.renderCell(user));
    const hiddenCells = hiddenUsers.map((user) => this.renderCell(user, true));

    return [
      displayedCells,
      this.renderHiddenRow(hiddenCells),
    ];
  }

  @autobind
  handleAddClick() {
    trackEvent(this.props.title, 'click_add');
    this.setState({
      isUsersPopupShown: true,
    });
  }

  get usersBlock() {
    if (this.isParticipantsLoading) {
      return <WaitAnimation size="s" />;
    }

    return (
      <div styleName="users-row">
        <AddButton
          styleName="add-button"
          onClick={this.handleAddClick}
        />
        {this.usersRow}
      </div>
    );
  }

  get popupTitle() {
    const { group, entity } = this.props;
    const entityTitle = _.capitalize(numberize(entity.entityType, 1));
    switch (group) {
      case groups.watchers:
        return `Watch ${entityTitle}`;
      case groups.assignees:
        return `Assign ${entityTitle}`;
    }
  }

  get popupBodyLabel() {
    const { group, entity } = this.props;

    const entityTitle = numberize(entity.entityType, 1);
    switch (group) {
      case groups.watchers:
        return 'Watchers:';
      case groups.assignees:
        return `Assign ${entityTitle} to:`;
    }
  }

  @autobind
  hideUsersPopup() {
    this.setState({
      isUsersPopupShown: false,
    });
  }

  @autobind
  handleUsersConfirm(admins: Array<TUser>) {
    this.props.addParticipants(admins.map(admin => admin.id));
    this.hideUsersPopup();
  }

  get selectAdminsModal() {
    return (
      <SelectAdminsModal
        isVisible={this.state.isUsersPopupShown}
        title={this.popupTitle}
        bodyLabel={this.popupBodyLabel}
        saveLabel={this.props.group == groups.watchers ? 'Watch' : 'Assign'}
        onCancel={this.hideUsersPopup}
        onConfirm={this.handleUsersConfirm}
      />
    );
  }

  render() {
    const { props } = this;

    return (
      <div styleName="root">
        <div styleName="title-row">
          <div styleName="title">
            {props.title}
          </div>
          <div styleName="controls">
            <a styleName="link" onClick={this.handleParticipate}>{props.actionTitle}</a>
          </div>
        </div>
        {this.usersBlock}
        {this.selectAdminsModal}
      </div>
    );
  }
}

export default _.flowRight(
  connect(mapGlobalStateToProps),
  makeLocalStore(addAsyncReducer(reducer)),
  connect(state => state, mapDispatchToProps)
)(Participants);
