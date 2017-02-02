//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

//data
import operators from 'paragons/customer-groups/operators';
import {
  setType,
  setName,
  setConditions,
  setMainCondition,
  GROUP_TYPE_MANUAL
} from 'modules/customer-groups/details/group';

//helpers
import { prefix } from 'lib/text-utils';

//components
import FormField from 'components/forms/formfield';
import { Dropdown } from 'components/dropdown';
import QueryBuilder from './query-builder';

const SELECT_CRITERIA = [
  [operators.and, 'all'],
  [operators.or, 'any']
];

const prefixed = prefix('fc-customer-group-edit');

class GroupEditor extends React.Component {

  static propTypes = {
    type: PropTypes.string.isRequired,
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
      setType: PropTypes.func.isRequired,
      setMainCondition: PropTypes.func.isRequired,
      setConditions: PropTypes.func.isRequired,
    }).isRequired,
  };

  componentDidMount() {
    const { group, actions } = this.props;

    actions.setType(this.type);

    if (!group.mainCondition) {
      actions.setMainCondition(operators.and);
    }
  }

  get type() {
    const { type, group } = this.props;
    return type || group.groupType;
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

  get typeField() {
    return (
      <FormField>
        <input
          type="hidden"
          name="type"
          value={this.type}
        />
      </FormField>
    );
  }

  get mainCondition() {
    const { group: { mainCondition }, actions: { setMainCondition } } = this.props;

    return (
      <div className={prefixed('match-div')}>
        <span className={prefixed('match-span')}>Customers match</span>
        <span className={prefixed('match-dropdown')}>
          <Dropdown
            name="matchCriteria"
            value={mainCondition}
            onChange={value => setMainCondition(value)}
            items={SELECT_CRITERIA}
          />
        </span>
        <span className={prefixed('form-name')}>of the following criteria:</span>
      </div>
    );
  }

  get dynamicGroupControls() {
    const { group, actions } = this.props;

    if (this.type == GROUP_TYPE_MANUAL) return null;

    return (
      <div>
        {this.mainCondition}
        <QueryBuilder
          conditions={group.conditions}
          isValid={group.isValid}
          setConditions={actions.setConditions}
        />
      </div>
    );
  }

  render() {
    return (
      <div>
        {this.typeField}
        {this.nameField}
        {this.dynamicGroupControls}
      </div>
    );
  }
}

const mapState = state => ({
  group: state.customerGroups.details.group,
});

const mapActions = dispatch => ({
  actions: bindActionCreators({ setType, setName, setConditions, setMainCondition }, dispatch),
});

export default connect(mapState, mapActions)(GroupEditor);
