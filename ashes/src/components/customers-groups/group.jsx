/* @flow */

//libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

//data
import { actions } from '../../modules/customer-groups/dynamic/group';

//components
import DynamicGroup from './dynamic/group';

type Props = {
  group: TCustomerGroup;
  reset: () => void;
  fetchGroup: (id: string) => Promise;
  params: {
    groupId: string;
  };
};

class Group extends React.Component {
  props: Props;

  componentWillMount() {
    const { group, reset, params: { groupId } } = this.props;

    if (groupId != group.id) reset();
  }

  componentDidMount() {
    const { group, fetchGroup, params: { groupId } } = this.props;

    if (groupId != group.id) fetchGroup(groupId);
  }

  render() {
    const { group } = this.props;

    if (!group.id) {
      return null;
    }

    return <DynamicGroup group={group} />;
  };
}


const mapStateToProps = state => ({
  group: state.customerGroups.dynamic.group
});

export default connect(mapStateToProps, actions)(Group);
