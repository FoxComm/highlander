
// libs
import React, { PropTypes } from 'react';
import moment from 'moment';

// components
import { Button } from '../common/buttons';
import UserInitials from '../users/initials';

export default class Activity extends React.Component {

  static propTypes = {
    activity: PropTypes.shape({
      type: PropTypes.string.isRequired,
    }).isRequired,
    undoAction: PropTypes.func,
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

  render() {
    const { activity } = this.props;
    const data = activity.data;

    return (
      <li className="fc-activity">
        <div className="fc-activity__body">
          <div className="fc-activity__timestamp">
            { moment(activity.createdAt).format('LT') }
          </div>
          <div className="fc-activity__content">
            <UserInitials name={data.author} />
            {data.author}
          </div>
        </div>
        {this.controls}
      </li>
    );
  }
}
