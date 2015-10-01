'use strict';

import React from 'react';
import UserInitials from '../users/initials';
import ViewerStore from '../../stores/viewers';

const updateTime  = 60000;

export default class Viewers extends React.Component {
  constructor(props) {
    super(props);
    ViewerStore.uriRoot = `${this.props.model}/${this.props.modelId}`;
    this.state = {
      viewers: [],
      lockedViewers: []
    };
  }

  componentDidMount() {
    ViewerStore.listenToEvent('change', this);
    this.onTimeout();
  }

  componentWillUnmount() {
    ViewerStore.stopListeningToEvent('change', this);
  }

  onTimeout() {
    ViewerStore.fetch();
    setTimeout(() => { this.onTimeout(); }, updateTime);
  }

  onChangeViewerStore(viewers) {
    this.setState({
      viewers: viewers.filter((viewer) => { return !viewer.isLocker; }),
      lockedViewers: viewers.filter((viewer) => { return viewer.isLocker; })
    });
  }

  render() {
    let
      viewers = this.state.viewers,
      lockedViewers = this.state.lockedViewers,
      lockedContent = null;

    let viewerItem = (viewer) => {
      return <li key={viewer.id}><UserInitials model={viewer}/></li>;
    };

    if (lockedViewers.length > 0) {
      lockedContent = (
        <ul>
          <li className="lock"><i className='icon-lock'></i></li>
          {lockedViewers.map(viewerItem)}
        </ul>
      );
    }

    return (
      <div className="fc-viewers">
        <ul>
          {viewers.map(viewerItem)}
        </ul>
        {lockedContent}
      </div>
    );
  }
}

Viewers.propTypes = {
  model: React.PropTypes.string,
  modelId: React.PropTypes.string
};
