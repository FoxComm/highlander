//libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind, debounce } from 'core-decorators';
import moment from 'moment';
import classNames from 'classnames';

//data
import operators from '../../../paragons/customer-groups/operators';
import { actions as groupActions } from '../../../modules/customer-groups/dynamic/group';
import { actions as listActions } from '../../../modules/customer-groups/dynamic/list';

//helpers
import { transitionTo } from '../../../route-helpers';
import { prefix } from '../../../lib/text-utils';

//components
import ContentBox from '../../content-box/content-box';
import { PanelList, PanelListItem } from '../../panel/panel-list';
import Currency from '../../common/currency';
import Form from '../../forms/form';
import { PrimaryButton } from '../../common/buttons';
import Criterion from './criterion-view';
import PrependIconInput from '../../icon-input/prepend-icon-input';


const prefixed = prefix('fc-customer-group-dynamic');

const mapStateToProps = state => ({list: state.customerGroups.dynamic.list});
const mapDispatchToProps = dispatch => ({
  groupActions: bindActionCreators(groupActions, dispatch),
  listActions: bindActionCreators(listActions, dispatch),
});

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroup extends Component {

  static propTypes = {
    list: PropTypes.object,
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array),
    }),
  };

  static contextTypes = {
    history: PropTypes.object.isRequired,
  };

  state = {
    criteriaOpen: true,
  };

  componentDidMount() {
    this.props.listActions.fetch();
  }

  get header() {
    const {list, group} = this.props;

    return (
      <header className={prefixed('header')}>
        <div className={prefixed('title')}>
          <h1 className="fc-title">
            {group.name}
            <span className={prefixed('count')}>{list.total}</span>
          </h1>
          <PrimaryButton onClick={this.goEdit}>Edit Group</PrimaryButton>
        </div>
        <div className={prefixed('about')}>
          <div>
            <span className={prefixed('about__key')}>Type: </span>
            <span className={prefixed('about__value')}>{_.capitalize(group.type)}</span>
          </div>
          <div>
            <span className={prefixed('about__key')}>Created: </span>
            <span className={prefixed('about__value')}>{moment(group.createdAt).format('DD/MM/YYYY HH:mm')}</span>
          </div>
        </div>
      </header>
    );
  }

  @autobind
  renderCriterion([field, operator, value], index) {
    return (
      <Criterion key={index}
                 field={field}
                 operator={operator}
                 value={value} />
    );
  }

  get criteria() {
    const {mainCondition, conditions} = this.props.group;
    const main = mainCondition === operators.and ? 'all' : 'any';

    return (
      <ContentBox title="Criteria"
                  className={prefixed('criteria')}
                  bodyClassName={classNames({'-open': this.state.criteriaOpen})}
                  actionBlock={this.criteriaToggle}>
        <span className={prefixed('main')}>
          Customers match <span className={prefixed('inline-label')}>{main}</span> of the following criteria:
        </span>
        {conditions.map(this.renderCriterion)}
      </ContentBox>
    );
  }

  get criteriaToggle() {
    const {criteriaOpen} = this.state;
    const icon = criteriaOpen ? 'icon-chevron-up' : 'icon-chevron-down';

    return (
      <i className={icon} onClick={() => this.setState({criteriaOpen: !criteriaOpen})} />
    );
  }

  @autobind
  goEdit() {
    transitionTo(this.context.history, 'edit-dynamic-customer-group', {groupId: this.props.group.id});
  }

  get stats() {
    return (
      <PanelList className={prefixed('stats')}>
        <PanelListItem title="Total Orders">
          132
        </PanelListItem>
        <PanelListItem title="Total Sales">
          <Currency value={5786.57} />
        </PanelListItem>
        <PanelListItem title="Avg. Order Size">
          2
        </PanelListItem>
        <PanelListItem title="Avg. Order Value">
          <Currency value={75.34} />
        </PanelListItem>
        <PanelListItem title="Return Rate">
          14%
        </PanelListItem>
      </PanelList>
    );
  }

  @autobind
  setFilterTerm({target}) {
    this.props.groupActions.setFilterTerm(target.value);
    this.updateSearch();
  }

  @debounce(200)
  updateSearch() {
    this.props.listActions.fetch();
  }

  get filter() {
    return (
      <PrependIconInput className={prefixed('filter')}>
        <input type="text"
               onChange={this.setFilterTerm}
               value={this.props.group.filterTerm} />
      </PrependIconInput>
    );
  }

  render() {
    return (
      <div className={prefixed('')}>
        <div className="fc-grid">
          <article className="fc-col-md-1-1">
            {this.header}
            {this.criteria}
            {this.stats}
            {this.filter}
          </article>
        </div>
      </div>
    );
  }
}
