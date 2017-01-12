//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

//data
import operators from '../../../paragons/customer-groups/operators';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import FormField from '../../forms/formfield';
import { Dropdown } from '../../dropdown';
import QueryBuilder from './query-builder';

import * as actions from '../../../modules/customer-groups/group';

const SELECT_CRITERIA = [
  [operators.and, 'all'],
  [operators.or, 'any']
];

const prefixed = prefix('fc-customer-group-edit');

const mapStateToProps = state => ({ group: state.customerGroups.details.group });
const mapDispatchToProps = dispatch => ({ actions: bindActionCreators(actions, dispatch) });

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroupEditor extends React.Component {

  static propTypes = {
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array).isRequired,
      isValid: PropTypes.bool,
    }),
    actions: PropTypes.shape({
      setName: PropTypes.func.isRequired,
      setMainCondition: PropTypes.func.isRequired,
      setConditions: PropTypes.func.isRequired,
    }).isRequired,
  };

  componentDidMount() {
    const { group, actions } = this.props;

    if (!group.mainCondition) {
      actions.setMainCondition(operators.and);
    }
  }

  get nameField() {
    const { group: { name }, actions: { setName } } = this.props;

    return (
      <FormField label="Group Name"
                 labelClassName={classNames(prefixed('title'), prefixed('name'))}>
        <input id="nameField"
               className={prefixed('form-name')}
               name="Name"
               maxLength="255"
               type="text"
               required
               onChange={({target}) => setName(target.value)}
               value={name} />
      </FormField>
    );
  }

  get mainCondition() {
    const { group: { mainCondition }, actions: { setMainCondition } } = this.props;

    return (
      <div className={prefixed('match-div')}>
        <span className={prefixed('match-span')}>Customers match</span>
        <span className={prefixed('match-dropdown')}>
          <Dropdown name="matchCriteria"
                    value={mainCondition}
                    onChange={value => setMainCondition(value)}
                    items={SELECT_CRITERIA}
          />
        </span>
        <span className={prefixed('form-name')}>of the following criteria:</span>
      </div>
    );
  }

  render() {
    const { group, actions } = this.props;

    return (
      <div>
        {this.nameField}
        {this.mainCondition}
        <QueryBuilder conditions={group.conditions}
                      isValid={group.isValid}
                      setConditions={actions.setConditions} />
      </div>
    );
  }
}
