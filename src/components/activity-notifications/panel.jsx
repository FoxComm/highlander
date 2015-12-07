import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import { PrimaryButton } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => ({notifications: state.activityNotifications}), NotificationActions)
export default class NotificationPanel extends React.Component {

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
            Notifications!
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
