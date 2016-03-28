//libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

//data
import { actions } from '../../modules/customer-groups/dynamic/group';

//components
import DynamicGroup from './dynamic/group';

const mapStateToProps = state => ({group: state.customerGroups.dynamic.group});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class Group extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      groupId: PropTypes.string,
    }).isRequired,
    group: PropTypes.object,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  componentDidMount() {
    const {params: {groupId}, actions} = this.props;

    if (groupId) {
      actions.fetchGroup(groupId);
    }
  }

  render() {
    const {group} = this.props;

    if (!group) {
      return null;
    }

    return group.type === 'dynamic'
      ? <DynamicGroup group={group} />
      //since we do not have manual groups yet
      : null;
  };
}
