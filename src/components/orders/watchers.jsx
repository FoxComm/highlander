// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// data
import { actions } from '../../modules/orders/watchers';
import { groups, entityForms } from '../../paragons/watcher';

// components
import Watchers from '../watchers/watchers';

//helpers
import { getSingularForm } from '../../lib/text-utils';


function getGroupData(group, watchers, order) {
  return {
    entries: order[group].map(user => user[getSingularForm(entityForms[group])]),
    listModalDisplayed: _.get(watchers, [group, 'listModalDisplayed'], false),
  };
}

function mapStateToProps({orders: {watchers}}, {order}) {
  return {
    data: {
      assignees: getGroupData(groups.assignees, watchers, order),
      watchers: getGroupData(groups.watchers, watchers, order),
      selectModal: watchers.selectModal,
    }
  };
}

function mapDispatchToProps(dispatch, {order: {referenceNumber}}) {
  const {
    showSelectModal,
    hideSelectModal,
    toggleListModal,
    suggestWatchers,
    selectItem,
    deselectItem,
    addWatchers,
    removeWatcher,
    } = actions;

  return {
    actions: {
      ...bindActionCreators({
        showSelectModal,
        hideSelectModal,
        toggleListModal,
        suggestWatchers,
        selectItem,
        deselectItem,
      }, dispatch),
      addWatchers: () => dispatch(actions.addWatchers(referenceNumber)),
      removeWatcher: (group, id) => dispatch(actions.removeWatcher(referenceNumber, group, id)),
    }
  };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class OrderWatchers extends React.Component {
  static propTypes = {
    order: PropTypes.object.isRequired,
    data: PropTypes.object.isRequired,
    actions: PropTypes.object.isRequired,
  };

  render() {
    const {data, actions} = this.props;

    return (
      <Watchers entityType="order"
                data={data}
                actions={actions} />
    );
  }
}
