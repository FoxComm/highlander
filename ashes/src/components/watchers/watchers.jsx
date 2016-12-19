// libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { trackEvent } from 'lib/analytics';

// data
import { groups, emptyTitle } from '../../paragons/watcher';

//helpers
import { getStore } from '../../lib/store-creator';

// components
import Panel from '../panel/panel';
import { AddButton } from '../common/buttons';
import DetailedInitials from '../user-initials/detailed-initials';
import WaitAnimation from '../common/wait-animation';
import { Button } from '../common/buttons';
import SelectWatcherModal from './select-modal';

const maxDisplayed = 7;

const getGroupData = (watchState, group) => ({
  entries: _.get(watchState, [group, 'entries'], []),
  listModalDisplayed: _.get(watchState, [group, 'listModalDisplayed'], false),
});


const mapStateToProps = (state, { entity: { entityType, entityId } }) => {
  const basePath = [entityType, 'watchers', entityId];

  const watchState = _.get(state, basePath);

  return {
    currentUser: state.user.current,
    isFetching: {
      [groups.assignees]: _.get(watchState, [groups.assignees, 'isFetching']),
      [groups.watchers]: _.get(state, [groups.watchers, 'isFetching']),
    },
    data: {
      assignees: getGroupData(watchState, groups.assignees),
      watchers: getGroupData(watchState, groups.watchers),
    }
  };
};

const mapDispatchToProps = (dispatch, { entity: { entityType, entityId } }) => {
  const { actions } = getStore([entityType, 'watchers']);

  return {
    fetch: (group) => dispatch(actions.fetchWatchers(entityId, group)),
    watch: (group, id) => dispatch(actions.watch(entityId, group, id)),
    showSelectModal: (group) => dispatch(actions.showSelectModal(entityId, group)),
    hideSelectModal: () => dispatch(actions.hideSelectModal(entityId)),
    toggleListModal: (group) => dispatch(actions.toggleListModal(entityId, group)),
    addWatchers: () => dispatch(actions.addWatchers(entityId)),
    removeWatcher: (group, id) => dispatch(actions.removeWatcher(entityId, group, id)),
  };
};

class Watchers extends Component {

  static propTypes = {
    storePath: PropTypes.string,
    entity: PropTypes.shape({
      entityType: PropTypes.string.isRequired,
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    }).isRequired,
    data: PropTypes.object.isRequired,
    currentUser: PropTypes.shape({
      id: PropTypes.number,
    }),

    //connected
    showSelectModal: PropTypes.func.isRequired,
    hideSelectModal: PropTypes.func.isRequired,
    toggleListModal: PropTypes.func.isRequired,
    addWatchers: PropTypes.func.isRequired,
    removeWatcher: PropTypes.func.isRequired,
    fetch: PropTypes.func.isRequired,
    watch: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.props.fetch(groups.assignees);
    this.props.fetch(groups.watchers);
  }

  watch(e, group) {
    e.preventDefault();

    trackEvent(getGroupTitle(group), 'click_assign_self');
    this.props.watch(group, this.props.currentUser.id);
  }

  render() {
    const props = this.props;

    return (
      <Panel className="fc-watchers">
        <div className="fc-watchers__container">
{/*
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Assignees
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" onClick={e => this.watch(e, groups.assignees)}>take it</a>
            </div>
          </div>
          {renderGroup(props, groups.assignees)}
*/}
          <div className="fc-watchers__title-row">
            <div className="fc-watchers__title">
              Watchers
            </div>
            <div className="fc-watchers__controls">
              <a className="fc-watchers__link" onClick={e => this.watch(e, groups.watchers)}>watch</a>
            </div>
          </div>
          {renderGroup(props, groups.watchers)}
        </div>
        <SelectWatcherModal
          entity={props.entity}
          onCancel={props.hideSelectModal}
          onConfirm={props.addWatchers} />
      </Panel>
    );
  }
}

function getGroupTitle(group) {
  switch (group) {
    case groups.assignees:
      return 'Assignees';
    case groups.watchers:
      return 'Watchers';
    default:
      throw new Error(`Unknown group ${group}`);
  }
}

const renderGroup = (props, group) => {
  if (props.isFetching[group]) {
    return <WaitAnimation size="s" />;
  }

  const users = _.get(props.data, [group, 'entries'], []);
  const handleAddClick = () => {
    trackEvent(getGroupTitle(group), 'click_add');
    props.showSelectModal(group);
  };

  return (
    <div className={classNames('fc-watchers__users-row', `fc-watchers__${group}-row`)}>
      <AddButton
        className="fc-watchers__add-button"
        onClick={handleAddClick}
      />
      {renderRow(props, group, users)}
    </div>
  );
};

renderGroup.propTypes = {
  data: PropTypes.object,
  isFetching: PropTypes.object,
};

const renderRow = (props, group, users) => {
  //empty label if nothing to show
  if (_.isEmpty(users)) {
    return (
      <div className={classNames('fc-watchers__empty-list', `fc-watchers__${group}-empty`)}>
        {emptyTitle[group]}
      </div>
    );
  }

  const removeWatcher = (id) => props.removeWatcher(group, id);

  if (users.length <= maxDisplayed) {
    return users.map((user) => renderCell(group, user, removeWatcher));
  }

  const displayedUsers = users.slice(0, maxDisplayed - 1);
  const hiddenUsers = users.slice(maxDisplayed - 1);

  const displayedCells = displayedUsers.map((user) => renderCell(group, user, removeWatcher));
  const hiddenCells = hiddenUsers.map((user) => renderCell(group, user, removeWatcher, true));

  return [
    displayedCells,
    renderHiddenRow(props, hiddenCells, group),
  ];
};

const renderHiddenRow = (props, group, cells) => {
  const { toggleListModal } = props;
  const active = _.get(props.data, [group, 'listModalDisplayed'], false);

  const hiddenBlockClass = classNames('fc-watchers__rest-block', { '_shown': active });
  const hiddenBlockOverlayClass = classNames('fc-watchers__rest-block-overlay', { '_shown': active });
  const buttonClass = classNames('fc-watchers__toggle-watchers-btn', { '_active': active });

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
};

renderHiddenRow.propTypes = {
  toggleListModal: PropTypes.func,
  data: PropTypes.object,
};

const renderCell = (group, user, removeWatcher, hidden = false) => {
  const { id, name } = user;
  const key = hidden ? `cell-hidden-${group}-${name}` : `cell-${group}-${name}`;

  const actionBlock = (
    <Button icon="close" onClick={() => removeWatcher(id)} />
  );

  return (
    <div className="fc-watchers__cell" key={key}>
      <DetailedInitials {...user}
        actionBlock={actionBlock}
        showTooltipOnClick={true} />
    </div>
  );
};

export default connect(mapStateToProps, mapDispatchToProps)(Watchers);
