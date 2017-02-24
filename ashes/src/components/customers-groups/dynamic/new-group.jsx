//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

//data
import { actions } from '../../../modules/customer-groups/dynamic/group';
import { fetchRegions } from '../../../modules/regions';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import NewGroupBase from './../new-group';
import DynamicGroupEditor from './group-editor';
import Form from '../../forms/form';
import { transitionTo, transitionToLazy } from 'browserHistory';
import SaveCancel from 'components/common/save-cancel';


const prefixed = prefix('fc-customer-group-dynamic-edit');

const mapStateToProps = state => ({group: state.customerGroups.dynamic.group});
const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators(actions, dispatch),
  fetchRegions: () => dispatch(fetchRegions()),
});

@connect(mapStateToProps, mapDispatchToProps)
export default class NewDynamicGroup extends React.Component {

  static propTypes = {
    group: PropTypes.shape({
      id: PropTypes.number,
      isValid: PropTypes.bool,
      isSaved: PropTypes.bool,
    }),
    actions: PropTypes.shape({
      reset: PropTypes.func.isRequired,
      saveGroup: PropTypes.func.isRequired,
    }).isRequired,
    fetchRegions: PropTypes.func.isRequired,
  };

  componentWillMount() {
    this.props.actions.reset();
  }

  componentDidMount() {
    this.props.fetchRegions();
  }

  componentDidUpdate() {
    const {id, isSaved} = this.props.group;
    if (isSaved) {
      transitionTo('customer-group', {groupId: id});
      return false;
    }

    return true;
  }

  render() {
    const {group, actions} = this.props;

    return (
      <NewGroupBase title="New Dynamic Customer Group">
        <Form onSubmit={() => actions.saveGroup()}>
          <DynamicGroupEditor />
          <div className={prefixed('form-submits')}>
            <SaveCancel
              onCancel={transitionToLazy('customer-groups')}
              saveText="Save Dynamic Group"
              saveDisabled={!group.isValid}
            />
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
