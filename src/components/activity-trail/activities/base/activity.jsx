
// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { Button } from '../../../common/buttons';
import { Time } from '../../../common/datetime';
import UserInitials from '../../../users/initials';
import CustomerLink from './customer-link';

export default class Activity extends React.Component {

  static propTypes = {
    activity: PropTypes.shape({
      kind: PropTypes.string.isRequired,
    }).isRequired,
    undoAction: PropTypes.func,
    title: PropTypes.node,
    details: PropTypes.shape({
      newOne: PropTypes.node,
      previous: PropTypes.node,
    }),
    isFirst: PropTypes.bool,
  };

  constructor(...args) {
    super(...args);

    this.state = {
      expanded: false
    };
  }

  get controls() {
    if (this.props.undoAction) {
      return (
        <div className="fc-activity__controls">
          <Button onClick={this.props.undoAction}>Undo</Button>
        </div>
      );
    }
  }

  get title() {
    if (this.props.title) {
      return this.props.title;
    }
  }

  get detailsView() {
    const details = this.props.details;

    if (details && this.state.expanded) {
      return (
        <div className="fc-activity__details">
          <div className="fc-activity__details-head">New</div>
          {details.newOne}
          <div className="fc-activity__details-head">Previous</div>
          {details.previous}
        </div>
      );
    }
  }

  @autobind
  toggleExpanded() {
    this.setState({
      expanded: !this.state.expanded
    });
  }

  get viewMoreLink() {
    if (this.props.details) {
      return (
        <div className="fc-activity__view-more">
          <span onClick={this.toggleExpanded}>
            {this.state.expanded ? '...View Less' : 'View More...'}
          </span>
        </div>
      );
    }
  }

  get authorIcon() {
    const { activity } = this.props;

    const userType = _.get(activity, ['context', 'userType'], 'system');

    switch (userType) {
      case 'admin':
        return <UserInitials name={activity.data.admin.name} />;
      case 'customer':
        return (
          <div className="fc-activity__customer-icon">
            <i className="icon-customer"></i>
          </div>
        );
      default:
        return <div className="fc-activity__system-icon"></div>;
    }
  }

  get authorTitle() {
    const { activity } = this.props;

    const userType = _.get(activity, ['context', 'userType'], 'system');

    switch (userType) {
      case 'admin':
        return activity.data.admin.name;
      case 'customer':
        if (activity.data.customer) {
          return <CustomerLink customer={activity.data.customer} />;
        }
        return 'The customer';
      default:
        return 'FoxCommerce';
    }
  }

  render() {
    const props = this.props;
    const { activity } = props;

    const className = classNames('fc-activity', {
      '_first': props.isFirst
    });

    return (
      <li className={className}>
        <div className="fc-activity__head-content">
          <div className="fc-activity__body">
            <div className="fc-activity__timestamp">
              <Time value={activity.createdAt} />
            </div>
            <div className="fc-activity__info">
              {this.authorIcon}
              <div className="fc-activity__description">
                {this.authorTitle}&nbsp;{this.title}
                {this.viewMoreLink}
              </div>
            </div>
          </div>
          {this.controls}
        </div>
        {this.detailsView}
      </li>
    );
  }
}
