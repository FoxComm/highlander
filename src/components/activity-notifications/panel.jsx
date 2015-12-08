import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import NotificationItem from '../activity-notifications/item';
import { PrimaryButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => ({notifications: state.activityNotifications}), NotificationActions)
export default class NotificationPanel extends React.Component {

  static propTypes = {
    notifications: PropTypes.shape({
      notifications: PropTypes.array,
      displayed: PropTypes.bool
    }),
    toggleNotifiactions: PropTypes.func
  };

  get items() {
    const items = this.props.notifications.notifications;
    return items.map(item => {
      return (<NotificationItem item={ item } />);
    });
  }

  get footer() {
    return (
      <div className="fc-activity-notifications__footer">
        <PrimaryButton onClick={ () => console.log('Not implemented yet') }
                       className="fc-activity-notifications__footer-button">
          View All
        </PrimaryButton>
      </div>
    );
  }

  get body() {
    if (this.props.notifications.displayed) {
      return (
        <div>
          <div className="fc-activity-notifications__overlay"
               onClick={ this.props.toggleNotifiactions }>
          </div>
          <ContentBox title='Notifications'
                      className="fc-activity-notifications__box"
                      footer={ this.footer }>
            { this.items }
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
