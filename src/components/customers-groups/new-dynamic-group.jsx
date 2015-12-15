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
export default class NewDynamicGroup extends React.Component {

  static propTypes = {
    saveQuery: PropTypes.func.isRequired,
    searchResultsLength: PropTypes.number,
    searchResults: PropTypes.array
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      name: '',
      matchCriteria: 'all',
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

  @autobind
  onSubmit() {
    this.props.saveQuery();
  }

  render() {
    const mainMatchStatuses = {
      all: 'all',
      none: 'none'
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
                   onChange={ ({target}) => this.setState({name: target.value}) }
                   value={ this.state.name } />
          </FormField>
          <div className='fc-group-new-match-div'>
            <span className='fc-group-new-match-span'>Customers match</span>
            <span className='fc-group-new-match-dropdown'>
              <Dropdown
                name='matchCriteria'
                items={mainMatchStatuses}
                value={this.state.matchCriteria}
                onChange={ (value) => this.setState({matchCriteria: value}) }
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
