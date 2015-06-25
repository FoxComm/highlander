'use strict';

import React from 'react';
import UserInitials from '../users/initials';
import ViewerStore from './store';

const updateTime  = 15000;

export default class Viewers extends React.Component {
  constructor(props) {
    super(props);
    ViewerStore.uriRoot = `${this.props.model}/${this.props.modelId}`;
    this.state = {
      viewers: []
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
    this.setState({viewers: viewers});
  }

  render() {
    let viewers = this.state.viewers;

    return (
      <ul className="viewers">
        {viewers.map((viewer) => {
          return <li key={viewer.id}><UserInitials model={viewer}/></li>;
        })}
      </ul>
    );
  }
}

Viewers.propTypes = {
  model: React.PropTypes.string,
  modelId: React.PropTypes.string
};
