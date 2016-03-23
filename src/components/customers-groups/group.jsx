//libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

//data
import { actions } from '../../modules/customer-groups/dynamic/group';

//helpers
import { prefix } from '../../lib/text-utils';

//components
import DynamicGroup from './dynamic/group';


const prefixed = prefix('fc-customer-group');

const mapStateToProps = state => ({group: state.customerGroups.dynamic.group});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class Group extends React.Component {

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

    const view = group.type === 'dynamic'
      ? <DynamicGroup group={group} />
      //since we do not have manual groups yet
      : null;

    return (
      <div className={prefixed()}>
        <div className="fc-grid">
          <header className="fc-col-md-1-1">
            <h1 className="fc-title">
              {group.name}
            </h1>
          </header>
          <article className="fc-col-md-1-1">
            {view}
          </article>
        </div>
      </div>
    );
  };
}
