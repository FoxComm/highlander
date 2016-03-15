//libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

//data
import { actions } from '../../modules/customer-groups/group';


const mapStateToProps = state => ({group: state.customerGroups.group});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class Group extends React.Component {

  componentDidMount() {
    const {params: {groupId}, actions: {fetchGroup}} = this.props;

    if (groupId) {
      fetchGroup(groupId);
    }
  }

  render() {
    const {props} = this;

    return (
      <div>
        Group details
      </div>
    );
  }
}
