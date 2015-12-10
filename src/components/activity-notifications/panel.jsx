import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import NotificationItem from '../activity-notifications/item';
import { PrimaryButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => state.activityNotifications, NotificationActions)
export default class NotificationPanel extends React.Component {

  static propTypes = {
    notifications: PropTypes.array,
    displayed: PropTypes.bool,
    toggleNotifications: PropTypes.func
  };

  static defaultProps = {
    displayed: false
  }

  get items() {
    const items = this.props.notifications;
    return items.map(item => {
      return (<NotificationItem item={item} key={`notification-item-${item.id}`}/>);
    });
  }

  get footer() {
    return (
      <div className="fc-activity-notifications__footer">
        <PrimaryButton onClick={() => console.log('Not implemented yet')}
                       className="fc-activity-notifications__footer-button">
          View All
        </PrimaryButton>
      </div>
    );
  }

  get body() {
    if (this.props.displayed) {
      return (
        <div>
          <div className="fc-activity-notifications__overlay"
               onClick={this.props.toggleNotifications}>
          </div>
          <ContentBox title='Notifications'
                      className="fc-activity-notifications__box"
                      footer={this.footer}>
            {this.items }
          </ContentBox>
        </div>
      );
    }
  }

  render() {
    return (
      <div className="fc-activity-notifications__panel">
        { this.body }
      </div>
    );
  }

}
