'use strict';

import React from 'react';
import ClassNames from 'classNames';
import UserInitials from '../users/initials';
import ViewerStore from '../../stores/viewers';

const updateTime = 60000;

export default class Viewers extends React.Component {
  constructor(props, context) {
    super(props, context);
    ViewerStore.uriRoot = `${this.props.model}/${this.props.modelId}`;
    this.state = {
      viewers: []
    };
  }

  componentDidMount() {
    ViewerStore.listenToEvent('change', this);
    // disabled until we have API for this
    // this.onTimeout();
    // mock data
    this.setState({
      viewers: [
        {
          firstName: 'Cameron',
          lastName: 'Sitt',
          email: 'cam@foxcommerce.com'
        },
        {
          firstName: 'Jeff',
          lastName: 'Mattaya',
          email: 'jeff@foxcommerce.com',
          isLocker: true
        },
        {
          firstName: 'Denys',
          lastName: 'Mikhalenko'
        }
      ]
    });
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

  render() {
    const viewers = this.state.viewers;

    const viewerItem = (viewer) => {
      const classnames = ClassNames({
        'fc-viewers-item': true,
        'is-locker': viewer.isLocker
      });
      return <li className={classnames} key={viewer.id}><UserInitials model={viewer}/></li>;
    };

    const locked = viewers.some(viewer => viewer.isLocker) && (
        <li className="fc-viewers-lock"><i className='icon-lock'></i></li>
      );

    return (
      <div className="fc-viewers">
        <ul>
          {locked}
          {viewers.map(viewerItem)}
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
