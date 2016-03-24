//libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind } from 'core-decorators';
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
import { PrimaryButton } from '../../common/buttons';
import Criterion from './criterion-view';


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
    setTimeout(()=> {
      this.props.groupActions.setFilterTerm('very');
      this.props.listActions.fetch();
    }, 1000);
  }

  get header() {
    const {list, group} = this.props;

    return (
      <header className={prefixed('__header')}>
        <div className={prefixed('__title')}>
          <h1 className="fc-title">
            {group.name}
            <span className={prefixed('__count')}>{list.total}</span>
          </h1>
          <PrimaryButton onClick={this.edit}>Edit Group</PrimaryButton>
        </div>
        <div className={prefixed('__about')}>
          <div>
            <span className={prefixed('__about__key')}>Type: </span>
            <span className={prefixed('__about__value')}>{_.capitalize(group.type)}</span>
          </div>
          <div>
            <span className={prefixed('__about__key')}>Created: </span>
            <span className={prefixed('__about__value')}>{moment(group.createdAt).format('DD/MM/YYYY HH:mm')}</span>
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
                  className={prefixed('__criteria')}
                  bodyClassName={classNames({'-open': this.state.criteriaOpen})}
                  actionBlock={this.criteriaToggle}>
        <span className={prefixed('__main')}>
          Customers match <span className={prefixed('__inline-label')}>{main}</span> of the following criteria:
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
  edit() {
    transitionTo(this.context.history, 'edit-dynamic-customer-group', {groupId: this.props.group.id});
  }

  get stats() {
    return (
      <PanelList className={prefixed('__stats')}>
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

  render() {
    //const {list, group} = this.props;

    return (
      <div className={prefixed('')}>
        <div className="fc-grid">
          <article className="fc-col-md-1-1">
            {this.header}
            {this.criteria}
            {this.stats}
          </article>
        </div>
      </div>
    );
  }
}
