'use strict';

import React from 'react';
import classNames from 'classnames';
import UserInitials from '../users/initials';
import ViewerStore from '../../stores/viewers';

const updateTime = 60000;

export default class Viewers extends React.Component {
  constructor(props, context) {
    super(props, context);
    ViewerStore.uriRoot = `${this.props.model}/${this.props.modelId}`;
    // mock data
    this.state = {
      viewers: [
        {
          id: 1,
          firstName: 'Denys',
          lastName: 'Mikhalenko'
        },
        {
          id: 2,
          firstName: 'Cameron',
          lastName: 'Stitt',
          email: 'cam@foxcommerce.com'
        },
        {
          id: 3,
          firstName: 'Adil',
          lastName: 'Wali',
          email: 'adil@foxcommerce.com',
          isLocker: true
        },
        {
          id: 4,
          firstName: 'Jeff',
          lastName: 'Mattaya',
          email: 'jeff@foxcommerce.com'
        }
      ]
    };
  }

  componentDidMount() {
    ViewerStore.listenToEvent('change', this);
    // disabled until we have API for this
    // this.onTimeout();
  }

  componentWillUnmount() {
    ViewerStore.stopListeningToEvent('change', this);
  }

  onTimeout() {
    ViewerStore.fetch();
    setTimeout(() => {
      this.onTimeout();
    }, updateTime);
  }

  onChangeViewerStore(viewers) {
    this.setState({
      viewers: viewers
    });
  }

  viewerItem(viewer) {
    const classnames = classNames({
      'fc-viewers-item': true,
      'is-locker': viewer.isLocker
    });
    return <li className={classnames} key={viewer.id}><UserInitials model={viewer}/></li>;
  }

  render() {
    const viewers = this.state.viewers;
    const locked = viewers.some(viewer => viewer.isLocker) && (
        <li className="fc-viewers-lock"><i className='icon-lock'></i></li>
      );

    return (
      <div className="fc-viewers">
        <ul>
          {locked}
          {viewers.map(this.viewerItem)}
        </ul>
      </div>
    );
  }
}

Viewers.propTypes = {
  model: React.PropTypes.string,
  modelId: React.PropTypes.oneOfType([
    React.PropTypes.string,
    React.PropTypes.number
  ])
};
