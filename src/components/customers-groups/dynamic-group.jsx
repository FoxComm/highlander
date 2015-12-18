import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import { PrimaryButton, Button } from '../common/buttons';
import { Link } from '../link';
import NewGroupBase from './new-group-base';
import QueryBuilder from './query-builder';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

import * as GroupBuilderActions from '../../modules/groups/builder';


@connect(state => state.groups.builder, GroupBuilderActions)
export default class DynamicGroup extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      groupId: PropTypes.string
    }).isRequired,
    name: PropTypes.string,
    changeName: PropTypes.func.isRequired,
    loadGroup: PropTypes.func.isRequired,
    saveQuery: PropTypes.func.isRequired,
    matchCriteria: PropTypes.string.isRequired,
    changeMatchCriteria: PropTypes.func.isRequired,
    searchResultsLength: PropTypes.number,
    searchResults: PropTypes.array
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      countCSSCycle: false,
    };
  }

  get searchCount() {
    let count = this.props.searchResultsLength;
    if (count == null) {
      count = '-';
    }
    const valueClass = classNames({
      'fc-group-new__count-value': true,
      '_is_modified': this.state.countCSSCycle,
      '_is_modified2': !this.state.countCSSCycle,
    });
    return (
      <div className="fc-group-new__count">
        <div className='fc-group-new-title fc-group-new__count-title'>Customers matched to criteria:</div>
        <div className={valueClass}>{count}</div>
      </div>
    );
  }

  componentWillReceiveProps(newProps) {
    this.setState({countCSSCycle: !this.state.countCSSCycle});
  }

  get groupId() {
    const { groupId } = this.props.params;
    return parseInt(groupId);
  }

  componentDidMount() {
    if (this.groupId) {
      this.props.loadGroup(this.groupId);
    }
  }

  @autobind
  onSubmit() {
    this.props.saveQuery(this.groupId);
  }

  render() {
    const mainMatchStatuses = {
      all: 'all',
      one: 'one',
    };

    return (
      <NewGroupBase title='New Dynamic Customer Group'
                    alternativeId='groups-new-manual'
                    alternativeTitle='manual group'>
        <Form onSubmit={this.onSubmit}>
          <FormField label='Group Name'
                     labelClassName='fc-group-new-title fc-group-new-name'>
            <input id='nameField'
                   className='fc-group-new-form-name'
                   name='Name'
                   maxLength='255'
                   type='text'
                   required
                   onChange={ ({target}) => this.props.changeName(target.value) }
                   value={ this.props.name } />
          </FormField>
          <div className='fc-group-new-match-div'>
            <span className='fc-group-new-match-span'>Customers match</span>
            <span className='fc-group-new-match-dropdown'>
              <Dropdown
                name='matchCriteria'
                items={mainMatchStatuses}
                value={this.props.matchCriteria}
                onChange={this.props.changeMatchCriteria}
              />
            </span>
            <span className='fc-group-new-match-span'>of the following criteria:</span>
          </div>
          <QueryBuilder {...this.props}/>
          {this.searchCount}
          <div className='fc-group-new-form-submits'>
            <Link to='customers'>Cancel</Link>
            <Button>Make Manual Group</Button>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
